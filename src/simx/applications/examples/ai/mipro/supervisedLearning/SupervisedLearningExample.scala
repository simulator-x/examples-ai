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

import simplex3d.math.floatx.{Vec3f, ConstVec3f}
import simx.applications.examples.ai.Servers
import simx.applications.examples.ai.mipro.supervisedLearning.imageviewer.{ImageViewer, EventDescriptions, DisplayConfiguration}
import simx.components.ai.mipro.implementations.{AccelerationProcessor, SupervisedLearningProcessor, VelocityProcessor}
import simx.components.ai.mipro.supervisedlearning.SupervisedLearning
import simx.components.ai.mipro.supervisedlearning.helper.Conversion.toList
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
import simx.core.entity.description.{TypedSValSet, SVal}
import simx.core.entity.typeconversion.SemanticTypeSet._
import simx.core.helper.chirality
import simx.core.ontology.functions.Interpolators
import simx.core.ontology.types.Vector3
import simx.core.ontology.{Symbols, types}
import simx.core.svaractor.SVarActor
import simx.core.svaractor.SVarActor.Ref
import simx.core.worldinterface.entity.filter.SValEquals
import simx.core.worldinterface.eventhandling.EventProvider
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}

/**
 * Created by chris on 21/07/15.
 */
object SupervisedLearningExample extends SimXApplicationMain[SupervisedLearningExample]{
  val fullScreen = false
}

class SupervisedLearningExample extends SimXApplication
with JVRInit
with EventProvider
with SupervisedLearning {

  println(Symbols.position)
  println(Symbols.transformation)

  val editorName  = 'editor
  val vrpnName    = 'vrpn
  val gfxName     = 'renderer
  val kinectName  = 'kinect
  val guiName     = 'gui


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
  val RotateGesture  = types.EntityType(Symbols.gesture)
  val PointingGesture = types.EntityType(Symbols.tuioCursorAdded)
  val User = types.EntityType(Symbols.user)
  /**
   * Called after components were configured and the creation of entities was initiated
   */
  override protected def finishConfiguration(): Unit = {
    if(slData.isPredict) {
      SVarActor.createActor(new ImageViewer(displaySetup))
      KinectFAASTSkeleton_1_2.upperBodyUserDescription("Tracker0@" + Servers.localhost).realize()
    }

    Start a new Processor with EntityCreationDSL {
      Requires property types.Position from entity describedBy RightHand
      Requires property types.Transformation from entity describedBy Spine

      Creates entity `with` properties RightHandRelative named 'RightHandRelative

      Updates the properties of entity describedBy RightHandRelative `with` {
        (types.Position of RightHand) relativeTo (types.Transformation of Spine)
      }
    }

    Start a new VelocityProcessor(RightHandRelative)
    Start a new AccelerationProcessor(RightHandRelative)

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

    Start a new SupervisedLearningProcessor with EntityCreationDSL with Interpolators with ProcessorHelper {

      IsConfigured by NeuralNetworkConfiguration("rrotate", neuralNetworkFolder)

      Requires properties (types.Position and types.Velocity and types.Acceleration and types.Angle) from RightHandRelative

      Creates entity `with` property RotateGesture named 'RotateGesture

      Updates the properties of entity describedBy types.EntityType(Symbols.gesture) `with` isGestureWith {
        val times = (0L to 400L by 25L).toList
        var inputFeatures: List[Double] = createFeaturesFrom(
          accelerationOverTimeMean(times, RightHandRelative),
          velocityOverTimeValues(times, RightHandRelative).flatten,
          elbowAngleOverTimeDelta(times, RightHandRelative),
          normalVectorOverTimeDelta(times,RightHandRelative).flatten
        )
        inputFeatures = inputFeatures.::(curvatureOverTimeSum(times, RightHandRelative))
        inputFeatures
      }
    }


    Start a new SupervisedLearningProcessor with EntityCreationDSL with Interpolators with ProcessorHelper {

      IsConfigured by NeuralNetworkConfiguration("point", neuralNetworkFolder)

      Requires properties (types.Position and types.Velocity and types.Acceleration and types.Angle) from RightHandRelative
      Requires property types.Position from entity describedBy Spine
      Requires properties (types.Position and types.Orientation) from entity describedBy RightHand

      Creates entity `with` property PointingGesture named 'Pointing

      Updates the properties of entity describedBy types.EntityType(Symbols.tuioCursorAdded) `with` isGestureWith {
        val times = (0L to 400L by 25L).toList
        var inputFeatures: List[Double] = createFeaturesFrom(
          //Right hand direction over time
          directionOverTimeDelta(times, RightHandRelative).flatten,
          //Right hand speed over time
          velocityOverTimeValues(times, RightHandRelative).flatten,
          //Mean speed of right hand
          velocityOverTimeMean(times, RightHandRelative),
          //Orientation of right hand
          orientationOfObject(RightHand),
          //Right elbow angle over time
          elbowAngleOverTimeDelta(times, RightHandRelative),
          //Distance between right hand and spine over time
          distanceBetweenObjectsOverTimeDelta(times, RightHand, new SemanticEntityReference(Spine))
        )
        //Right elbow angle
        inputFeatures = inputFeatures.::((types.Angle of RightHandRelative).value)
        //Right hand distance (to spine)
        inputFeatures = inputFeatures.::((types.Position of RightHandRelative).value.length)
        inputFeatures
      }
    }

    Start a new Processor with EntityCreationDSL with Interpolators with EventProvider {

      Requires properties types.Real from  PointingGesture
      Requires properties types.Real from  RotateGesture

      var isAllowed = true
      var rotates: List[Float] = Nil

      Updates the properties of entity describedBy User `with` {
        var result: Symbol = Symbol("")
        val pointing = types.Real of PointingGesture
        val rotate = types.Real of RotateGesture

        if(isAllowed){
          if(pointing.value > 0.8f) {
            EventDescriptions.select.emit()
            result = Symbol("Pointing")
            isAllowed = false
            addJobIn(3000) {isAllowed = true}
          } else if(rotate.value > 0.95f) {
            rotates ::= rotate.value
            if(rotates.size == 5) {
              EventDescriptions.rotate.emit()
              result = Symbol("Rotate")
              isAllowed = false
              addJobIn(3000) {isAllowed = true}
              rotates = Nil
            }
          }
        }
        types.Identifier(result)
      }
    }

    onOneEntityAppearance(SValEquals(types.EntityType(Symbols.keyboard))) { keyboardEntity =>
      keyboardEntity.observe(types.Key_Space){pressed => if (pressed) startPlayback()}
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

  /**
   * Called when the entities are meant to be created
   */
  override protected def createEntities(): Unit = {
    initializeKeyboardControl()
  }

  override protected def removeFromLocalRep(e: Entity): Unit = {}
  def createFeaturesFrom(x: List[Double], xs: List[Double]*) = x ::: xs.flatten.toList

}
