/*
 * Copyright 2015 The SIRIS Project
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * The SIRIS Project is a cooperation between Beuth University, Berlin and the
 * HCI Group at the University of Würzburg. The project is funded by the German
 * Federal Ministry of Education and Research (grant no. 17N4409).
 */

package simx.applications.examples.ai.mipro

import simplex3d.math.floatx.Vec3f
import simx.applications.examples.ai.Servers
import simx.applications.examples.ai.stubs.Utils.{constVec3fsValToList, toList}
import simx.applications.examples.ai.stubs.{NeuralNetworkStub, Utils}
import simx.components.ai.feature.recording.Events._
import simx.components.ai.feature.recording.storage.Persistence
import simx.components.ai.feature.recording.{EntityPlayer, StartFastForwardPlayback}
import simx.components.ai.feature.sl_chris.AnnotationReader
import simx.components.ai.mipro.implementations.{AccelerationProcessor, VelocityProcessor}
import simx.components.ai.mipro.supervisedlearning.NeuralNetworkProcessor
import simx.components.ai.mipro.{EntityCreationDSL, EventDSL, Processor, Start}
import simx.components.editor.EditorComponentAspect
import simx.components.renderer.jvr.JVRInit
import simx.components.vrpn.VRPNComponentAspect
import simx.components.vrpn.avatar.KinectFAASTSkeleton_1_2
import simx.core.entity.Entity
import simx.core.helper.{IO, Jfx, chirality}
import simx.core.ontology._
import simx.core.ontology.functions.Interpolators
import simx.core.svaractor.SVarActor
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}

import scala.collection.immutable

/**
 * Created by
 * martin
 * in June 2015.
 */
object NeuralNetworkExample extends SimXApplicationMain[NeuralNetworkExample]

class NeuralNetworkExample extends SimXApplication with JVRInit {

  println(Symbols.position)
  println(Symbols.transformation)

  var entityPlayer: Option[SVarActor.Ref] = None

  private var training, testing, prediction = false

  val playbackDataFile = IO.askForOptions("Select task:", "Training", "Testing", "Prediction") match {
    case 0 =>
      training = true
      Jfx.askForFile("Choose entity recording for training", Some("xml"))
    case 1 =>
      testing = true
      Jfx.askForFile("Choose entity recording for testing", Some("xml"))
    case 2 =>
      prediction = true
      None
  }

  val playbackData            = playbackDataFile.map(Persistence.load(_))
  val playbackAnnotationFile  = playbackDataFile.map(IO.changeExtension("csv"))
  val annotationOption        = playbackData.map{ data =>
    new AnnotationReader(playbackAnnotationFile.get, data.recordingStart.get)}
  //Just for convenience
  def annotation              = annotationOption.get

  //Folder for storage of neural network parameters (e.g. thetas or X and Y matrices)
  val neuralNetworkFolder     = Jfx.askForFolder("Select neural network folder").get

  override protected def applicationConfiguration = ApplicationConfig withComponent
    EditorComponentAspect('editor, appName = "MasterControlProgram") and
    VRPNComponentAspect('vrpn) iff prediction

  protected def configureComponents(components: immutable.Map[Symbol, SVarActor.Ref]): Unit = {
    playbackData.foreach{data => entityPlayer = Some(SVarActor.createActor(new EntityPlayer(data)))}
  }

  protected def createEntities(): Unit = {
    if(training || testing) startPlayback(delay = 5000L)
    else establishKinectConnection()
  }

  //Semantic descriptions of required entities
  val RightHand  = types.EntityType(Symbols.hand) and types.Chirality(chirality.Right)
  val LeftHand   = types.EntityType(Symbols.hand) and types.Chirality(chirality.Left)
  val Spine      = types.EntityType(Symbols.spine)

  val RightHandRelative =
    types.EntityType.withAnnotations(types.Typ(Symbols.relative))(Symbols.hand) and types.Chirality(chirality.Right)

  protected def finishConfiguration() {

    Start a new VelocityProcessor(RightHand)
    Start a new AccelerationProcessor(RightHand)

    //See generalized implementation: simx.components.ai.mipro.implementations.RelativePositionProcessor
    Start a new Processor with EntityCreationDSL { //that
      Requires property types.Position from RightHand
      Requires property types.Transformation from Spine

      Creates entity `with` properties RightHandRelative named 'RightHandRelative

      Updates the properties of entity describedBy RightHandRelative `with` {
        (types.Position of RightHand) relativeTo (types.Transformation of Spine)
      }
    }    

    Start a new NeuralNetworkProcessor with EntityCreationDSL with EventDSL with Interpolators {

      Requires property types.Position from RightHandRelative
      Requires property types.Acceleration from RightHand

      Uses network implemented by classOf[NeuralNetworkStub]
      Stores networkConfiguration in folder neuralNetworkFolder

      Creates entity `with` property types.EntityType(Symbols.gesture) named 'Gesture

      Updates the properties of entity describedBy types.EntityType(Symbols.gesture) `with` {
        //Calculate the mean acceleration value of the right hand over the last 200ms
        val times = types.Milliseconds(0L) :: types.Milliseconds(100L) :: types.Milliseconds(200L) :: Nil
        val semanticAccelerationValues      = times.map{t => types.Acceleration of RightHand at t}
        val rawAccelerationValues           = semanticAccelerationValues.map(_.value)
        val sum  = rawAccelerationValues.foldLeft(Vec3f.Zero)(_+_)
        val mean = sum / times.size.toFloat

        //Prepare the X matrix (inputs of the neural network) by unrolling
        //the position of the right hand relative to the spine and
        //the mean acceleration value of the right hand over the last 200ms
        val X: List[Double] = Utils.createMatrix(
          types.Position of RightHandRelative,
          mean
        )

        if(training) {
          val groundTruth = annotation("rotate").isPresent
          network.appendTrainingData(X)(groundTruth)
          types.Boolean(groundTruth)
        } else /* prediction or testing */ {
          val gestureIsPresent: Boolean = network.predict(X) > 0.5f
          if(testing) addPrediction(gestureIsPresent, annotation("rotate").isPresent)
          types.Boolean(gestureIsPresent)
        }
      }

      Reacts to event describedBy playbackFinished by {
        if(training) network.trainNetwork() else if(testing) printF1Score()
      }
    }
  }

  private def startPlayback(delay: Long = 5000L): Unit = {
    //Start playback in 5 sec
    addJobIn(delay) {
      entityPlayer.foreach { player =>
        player ! StartFastForwardPlayback(forerunInMillis = 5000L, coolDownInMillis = 3000L, speedUp = 5L)
      }
    }
  }

  private def establishKinectConnection(): Unit = {
    KinectFAASTSkeleton_1_2.simpleUpperBodyUserDescription("Tracker0@" + Servers.fishTank).realize()
  }

  protected def removeFromLocalRep(e : Entity){println("[info][GestureRecognitionExample] Removed entity " + e)}
}