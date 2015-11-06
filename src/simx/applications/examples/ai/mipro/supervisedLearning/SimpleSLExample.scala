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

package simx.applications.examples.ai.mipro.supervisedLearning

import java.io.File

import simplex3d.math.floatx.ConstVec3f
import simx.applications.examples.ai.Servers
import simx.applications.examples.ai.mipro.supervisedLearning.imageviewer.{DisplayConfiguration, EventDescriptions, ImageViewer}
import simx.applications.examples.ai.ontology.{types => localTypes}
import simx.components.ai.mipro.implementations.{AccelerationProcessor, SupervisedLearningProcessor, VelocityProcessor}
import simx.components.ai.mipro.supervisedlearning.{Predict, SLData, SupervisedLearning}
import simx.components.ai.mipro.supervisedlearning.slprovider.neuralnetwork.NeuralNetworkConfiguration
import simx.components.ai.mipro.{SemanticEntityReference, EntityCreationDSL, Processor, Start}
import simx.components.editor.EditorComponentAspect
import simx.components.renderer.gui.GuiComponentAspect
import simx.components.renderer.jvr.{JVRComponentAspect, JVRInit}
import simx.components.vrpn.VRPNComponentAspect
import simx.components.vrpn.avatar.KinectFAASTSkeleton_1_2
import simx.core.component.{ExecutionStrategy, Soft}
import simx.core.components.renderer.setup.BasicDisplayConfiguration
import simx.core.entity.Entity
import simx.core.entity.description.TypedSValSet
import simx.core.entity.typeconversion.Converter
import simx.core.entity.typeconversion.SemanticTypeSet._
import simx.core.helper.chirality
import simx.core.ontology.functions.Interpolators
import simx.core.ontology.types.Vector3
import simx.core.ontology.{GroundedSymbol, Symbols, types}
import simx.core.svaractor.SVarActor
import simx.core.svaractor.SVarActor.Ref
import simx.core.worldinterface.entity.filter.SValEquals
import simx.core.worldinterface.eventhandling.EventProvider
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}

/**
 * Created by chris on 21/07/15.
 */
object SimpleSLExample extends SimXApplicationMain[SimpleSLExample]{
  val fullScreen = false
}

class SimpleSLExample extends SimXApplication
with JVRInit
with EventProvider
{

  println(Symbols.position)
  println(Symbols.transformation)



  val editorName  = 'editor
  val vrpnName    = 'vrpn
  val gfxName     = 'renderer
  val kinectName  = 'kinect
  val guiName     = 'gui

  implicit val slData = new SLData(None, None, Predict())
  private val neuralNetworkFolder = new File("assets/nn-data/")

  private val displaySetup = DisplayConfiguration(1280, 800, fullscreen = SupervisedLearningExample.fullScreen)



  override protected def applicationConfiguration = ApplicationConfig withComponent
    EditorComponentAspect(editorName, appName = "MasterControlProgram") and
    JVRComponentAspect(gfxName, BasicDisplayConfiguration(1280, 800, fullscreen = false)) and /*on "renderNode"*/// and
    VRPNComponentAspect('vrpn) iff slData.isPredict and
    GuiComponentAspect(guiName, displaySetup)



  /**
   * Called after all components were created
   * @param components the map of components, accessible by their names
   */
  override protected def configureComponents(components: Map[Symbol, Ref]): Unit = {
    exitOnClose(components(gfxName), shutdown) // register for exit on close
    start(ExecutionStrategy where
      components(gfxName) runs Soft(60) and
      components(guiName) runs Soft(60)
    )
  }

  val RightHand      = types.EntityType(Symbols.hand) and types.Chirality(chirality.Right)
  val RightElbow     = types.EntityType(Symbols.elbow) and types.Chirality(chirality.Right)
  val RightShoulder  = types.EntityType(Symbols.shoulder) and types.Chirality(chirality.Right)
  val Spine          = types.EntityType(Symbols.spine)

  val RightHandRelative =
    types.EntityType.withAnnotations(Symbols.relative)(Symbols.hand) and types.Chirality(chirality.Right)
  val RightHandOverTime =
    types.EntityType.withAnnotations(Symbols.overTime)(Symbols.hand) and types.Chirality(chirality.Right)

  val RotateGesture  = types.EntityType(Symbols.rotate)
  val PointingGesture = types.EntityType(Symbols.pointing)
  val SwipeGesture = types.EntityType(Symbols.swipe)

  val User = types.EntityType(Symbols.user)


  override protected def finishConfiguration(): Unit = {
    if(slData.isPredict) {
      SVarActor.createActor(new ImageViewer(displaySetup))
      KinectFAASTSkeleton_1_2.upperBodyUserDescription("Tracker0@" + Servers.localhost).realize()
    }

    val times = (0L to 400L by 25L).toList

    Start a new VelocityProcessor(RightHandRelative)
    Start a new AccelerationProcessor(RightHandRelative)

    Start a new Processor with EntityCreationDSL {
      Requires property types.Position from entity describedBy RightHand
      Requires property types.Transformation from entity describedBy Spine

      Creates entity `with` properties RightHandRelative named 'RightHandRelative

      Updates the properties of entity describedBy RightHandRelative `with` {
        (types.Position of RightHand) relativeTo (types.Transformation of Spine)
      }
    }

    Start a new Processor with EntityCreationDSL with Interpolators {

      Requires properties types.Transformation from  RightHand
      Requires properties types.Transformation from  RightElbow
      Requires properties types.Transformation from  RightShoulder

      Updates the properties of entity describedBy RightHandRelative `with` {
        val underArmVec: Vector3 = (types.Position of (types.Transformation of RightHand))  - (types.Position of (types.Transformation of RightElbow))
        val upperArmVec: Vector3 = (types.Position of (types.Transformation of RightElbow)) - (types.Position of (types.Transformation of RightShoulder))
        types.Angle between (underArmVec, upperArmVec)
      }
    }

    Start a new Processor with EntityCreationDSL with Interpolators with ProcessorHelper {
      Requires property types.Position from RightHand
      Requires property types.Position from Spine
      Updates the properties of entity describedBy RightHandOverTime `with` {
        val distancesOverTime = distanceBetweenObjectsOverTimeDelta(times, RightHand, new SemanticEntityReference(Spine))
        localTypes.Distances(distancesOverTime)
      }
    }

    Start a new Processor with EntityCreationDSL with Interpolators with ProcessorHelper {
      Requires property types.Acceleration from RightHandRelative
      Creates entity `with` properties RightHandOverTime named 'RightHandOverTime
      Updates the properties of entity describedBy RightHandOverTime `with` {
        val accOverTime = accelerationOverTimeMean(times, RightHandRelative)
        types.Acceleration(accOverTime)
      }
    }

    Start a new Processor with EntityCreationDSL with Interpolators with ProcessorHelper {
      Requires property types.Acceleration from RightHandRelative
      Updates the properties of entity describedBy RightHandOverTime `with` {
        val accOverTime = accelerationOverTimeValues(times, RightHandRelative)
        localTypes.Accelerations(accOverTime)
      }
    }

    Start a new Processor with EntityCreationDSL with Interpolators with ProcessorHelper {
      Requires property types.Velocity from RightHandRelative
      Updates the properties of entity describedBy RightHandOverTime `with` {
        val veloOverTime = velocityOverTimeValues(times, RightHandRelative)
        localTypes.Velocities(veloOverTime)
      }
    }

    Start a new Processor with EntityCreationDSL with Interpolators with ProcessorHelper {
      Requires property types.Angle from RightHandRelative
      Updates the properties of entity describedBy RightHandOverTime `with` {
        val angleOverTime = elbowAngleOverTimeDelta(times, RightHandRelative)
        localTypes.Angles(angleOverTime)
      }
    }

    Start a new Processor with EntityCreationDSL with Interpolators with ProcessorHelper {
      Requires property types.Position from RightHandRelative
      Updates the properties of entity describedBy RightHandOverTime `with` {
        val normalVecOverTime: List[ConstVec3f] = normalVectorOverTimeDelta(times, RightHandRelative)
        localTypes.Normals(normalVecOverTime)
      }
    }

    Start a new Processor with EntityCreationDSL with Interpolators with ProcessorHelper {
      Requires property types.Position from RightHandRelative
      Updates the properties of entity describedBy RightHandOverTime `with` {
        val normalVecOverTime = curvatureOverTimeSum(times, RightHandRelative)
        localTypes.Curvature(normalVecOverTime)
      }
    }

    Start a new Processor with EntityCreationDSL with Interpolators with ProcessorHelper {
      Requires property types.Position from RightHandRelative
      Updates the properties of entity describedBy RightHandOverTime `with` {
        val directionsOverTime = directionOverTimeDelta(times, RightHandRelative)
        localTypes.Directions(directionsOverTime)
      }
    }

    Start a new SupervisedLearningProcessor with EntityCreationDSL with Interpolators with ProcessorHelper {

      IsConfigured by NeuralNetworkConfiguration("lswipe", neuralNetworkFolder)

      Requires properties (localTypes.Velocities and localTypes.Accelerations and types.Acceleration and localTypes.Directions and localTypes.Curvature) from RightHandOverTime

      Creates entity `with` property SwipeGesture named 'SwipeGesture

      Updates the properties of entity describedBy SwipeGesture `with` prediction
    }


    Start a new SupervisedLearningProcessor with EntityCreationDSL with Interpolators with ProcessorHelper {

      IsConfigured by NeuralNetworkConfiguration("point", neuralNetworkFolder)

      Requires properties (localTypes.Velocities and localTypes.Angles and localTypes.Directions and localTypes.Distances) from RightHandOverTime
      Requires properties types.Angle from RightHandRelative

      Creates entity `with` property PointingGesture named 'PointingGesture

      Updates the properties of entity describedBy PointingGesture `with` prediction
    }

    Start a new SupervisedLearningProcessor with EntityCreationDSL with Interpolators with ProcessorHelper {

      IsConfigured by NeuralNetworkConfiguration("rrotate", neuralNetworkFolder)

      Requires properties (localTypes.Velocities and types.Acceleration and localTypes.Normals and localTypes.Curvature and localTypes.Angles) from RightHandOverTime

      Creates entity `with` property RotateGesture named 'RotateGesture

      Updates the properties of entity describedBy RotateGesture `with` prediction
    }


    Start a new Processor with EntityCreationDSL with Interpolators with EventProvider {

      Requires properties types.Real from  RotateGesture
      Requires properties types.Real from  PointingGesture
      Requires properties types.Real from  SwipeGesture

      Updates the properties of entity describedBy User `with` {
        val activationRotate = types.Real of RotateGesture
        val activationPoint = types.Real of PointingGesture
        val activationSwipe = types.Real of SwipeGesture

        val result = processActivations(activationRotate.value, activationPoint.value, activationSwipe.value)

        localTypes.Gesture(result)
      }
    }



    onOneEntityAppearance(SValEquals(types.EntityType(Symbols.keyboard))) { keyboardEntity =>
      keyboardEntity.observe(types.Key_Space){pressed => /*if (pressed) startPlayback()*/}
    }
  }

  private val keyMap = Map(
    types.Key_Up    -> EventDescriptions.fastBackward,
    types.Key_Down  -> EventDescriptions.fastForward,
    types.Key_Left  -> EventDescriptions.previous,
    types.Key_Right -> EventDescriptions.next,
    types.Key_Space -> EventDescriptions.select,
    types.Key_i     -> EventDescriptions.zoomIn,
    types.Key_o     -> EventDescriptions.zoomOut,
    types.Key_r     -> EventDescriptions.rotate
  )

  private def initializeKeyboardControl() {
    handleDevice(types.Keyboard)( (keyboardEntity) => {
      keyMap.foreach(mapping => {
        keyboardEntity.observe(mapping._1).head(pressed => if(pressed) mapping._2.emit())
      })
    })
  }

  override protected def createEntities(): Unit = {
    initializeKeyboardControl()
  }

  override protected def removeFromLocalRep(e: Entity): Unit = {}

  var isAllowed = true
  var rotates: List[Float] = Nil
  var swipes: List[Float] = Nil
  var pointings: List[Float] = Nil

  def processActivations(rotate: Float, pointing: Float, swipe: Float): GroundedSymbol ={
    var result: GroundedSymbol = Symbols.gesture
    if(isAllowed) {
      if(pointing > 0.95f) {
        pointings ::= pointing
        if(pointings.size == 2) {
          EventDescriptions.select.emit()
          result = Symbols.pointing
          resetProcessing()
        }
      } else if(rotate > 0.95f) {
        rotates ::= rotate
        if(rotates.size == 5) {
          EventDescriptions.rotate.emit()
          result = Symbols.rotate
          resetProcessing()
        }
      }
      if(swipe > 0.7f) {
        swipes ::= swipe
        if(swipes.size == 5) {
          EventDescriptions.previous.emit()
          result = Symbols.swipe
          resetProcessing()
        }
      }
    }
    result
  }

  def resetProcessing(): Unit ={
    rotates = Nil
    swipes = Nil
    pointings = Nil
    isAllowed = false
    addJobIn(1000) {isAllowed = true}
  }

}
