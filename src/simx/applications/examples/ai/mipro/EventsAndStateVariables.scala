/*
 * Copyright 2012 The SIRIS Project
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

import simplex3d.math.float._
import simx.components.ai.mipro.{EntityCreationDSL, Processor, Start}
import simx.components.editor.EditorComponentAspect
import simx.components.physics.jbullet.JBulletComponentAspect
import simx.components.renderer.jvr.{JVRComponentAspect, JVRInit, JVRPickEvent}
import simx.components.sound.{LWJGLSoundComponentAspect, OpenALInit, SoundMaterial}
import simx.core.component.remote.RemoteCreation
import simx.core.component.{ExecutionStrategy, Soft}
import simx.core.components.io.SpeechEvents
import simx.core.components.physics.ImplicitEitherConversion._
import simx.core.components.physics.PhysSphere
import simx.core.components.renderer.createparameter.{ReadFromElseWhere, ShapeFromFile}
import simx.core.components.renderer.setup.BasicDisplayConfiguration
import simx.core.entity.Entity
import simx.core.entity.description.SValSet
import simx.core.helper.{chirality, IO}
import simx.core.ontology.{Symbols, EntityDescription, types}
import simx.core.svaractor.SVarActor
import simx.core.worldinterface.entity.filter.SValEquals
import simx.core.worldinterface.eventhandling.{EventHandler, EventProvider}
import simx.core.worldinterface.naming.NameIt
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}
import simx.applications.examples.ai.objects._

import scala.collection.immutable
import scala.util.Random

/**
 * @author Dennis Wiebusch, Martin Fischbach
 */
object EventsAndStateVariables extends SimXApplicationMain[EventsAndStateVariables] {
  val useEditor = IO.askForOption("Use Editor Component?")
}

class EventsAndStateVariables(args : Array[String]) extends SimXApplication
with JVRInit with OpenALInit with RemoteCreation with EventProvider with EventHandler
{
  //Component names
  val physicsName = 'physics
  val editorName = 'editor
  val soundName = 'sound
  val gfxName = 'renderer

  //Some application specific variables
  private var cameraEntityOption: Option[Entity] = None
  private var tableEntityOption: Option[Entity] = None
  private val ballRadius = 0.2f
  private val ballPosition = ConstVec3(0f, 1.5f, -7f)
  private var ballCounter = 0

  /**
   *  Methods defined in trait SimXApplication
   */

  override protected def applicationConfiguration = ApplicationConfig withComponent
    JVRComponentAspect(gfxName, BasicDisplayConfiguration(1280, 800, fullscreen = false)) /*on "renderNode"*/ and
    JBulletComponentAspect(physicsName, ConstVec3(0, -9.81f, 0)) /*on "physicsNode"*/ and
    LWJGLSoundComponentAspect(soundName) /*on "soundNode"*/ and
    EditorComponentAspect(editorName, appName = "MasterControlProgram") iff EventsAndStateVariables.useEditor

  protected def configureComponents(components: immutable.Map[Symbol, SVarActor.Ref]) {
    exitOnClose(components(gfxName), shutdown) // register for exit on close
    start(ExecutionStrategy where
      components(physicsName) runs Soft(60) and
      components(gfxName) runs Soft(60)  and
      components(soundName) runs Soft(60)
      //where
      //components(physicsName) isTriggeredBy components(gfxName) and
      //components(gfxName) isTriggeredBy components(physicsName) startWith Set(components(physicsName))
    )
  }

  protected def createEntities() {
    Sounds.init()
    Ball("the ball", ballRadius, ballPosition).realize(entityComplete)
    Cylinder(ballPosition, additionalProperties = SValSet(types.EntityType(Symbols.cylinder))).realize()
    Light("the light", Vec3(-4f, 8f, -7f), Vec3(270f, -25f, 0f)).realize(entityComplete)
    Table("the table", Vec3(3f, 1f, 2f), Vec3(0f, -1.5f, -7f)).realize((tableEntity: Entity) => {
      entityComplete(tableEntity)
      tableEntityOption = Some(tableEntity)
    })
  }

  protected def finishConfiguration() {

    val Cylinder = types.EntityType(Symbols.cylinder)
    val Ball = types.EntityType(Symbols.iceball)

    Start a new Processor with EntityCreationDSL { //that
      Is named 'TestProc
      Requires property types.Transformation from Ball

      Updates the properties of entity describedBy Cylinder `with` {
        types.Transformation((types.Transformation of Ball).value * ConstMat4(Mat4x3.translate(Vec3.UnitX)))
      }
    }





    rotateTable()
    initializePicking()
    initializeBallSpawning()
    initializeMouseControl()
    SpeechEvents.token.observe{ event =>
      val text = event.values.firstValueFor(types.String)
      println("[info][EventsAndStateVariables] Test event received. Contained sting is: " + text)
    }
    println("[info][EventsAndStateVariables] Application is running: Press SPACE to spawn new balls!")
  }

  /**
   *  Methods defined in trait EntityUpdateHandling
   */

  protected def removeFromLocalRep(e : Entity){println("[info][EventsAndStateVariables] Removed entity " + e)}

  /**
   *  Some application specific methods
   */

  private def entityComplete(e: Entity) {println("[info][EventsAndStateVariables] Completed entity " + e)}

  private def initializePicking(){
    var clickedOnce = Set[Entity]()
    JVRPickEvent.observe{ event =>
      if(event.get(types.Enabled).getOrElse(false)) event.get(types.Entity).collect{
        case entity if !clickedOnce.contains(entity) =>
          println("picked " + entity)
          clickedOnce = clickedOnce + entity
          entity.set(types.Velocity(Vec3.Zero))
          entity.disableAspect(PhysSphere())
        case entity =>
          println("picked " + entity)
          clickedOnce = clickedOnce - entity
          entity.enableAspect(PhysSphere())
      }
    }
  }

  private def rotateTable() {
    addJobIn(16L){
      tableEntityOption.collect{ case tableEntity =>
        //In complex applications it is reasonable to check if the list is not empty, rather than just call 'head'
        tableEntity.get(types.Transformation){
          currentTransform => tableEntity.set(types.Transformation(rotate(currentTransform)))
        }
      }
      rotateTable()
    }
  }

  private def rotate(mat: ConstMat4) =
    mat * ConstMat4(Mat4x3.rotateY(0.01f))

  private def initializeBallSpawning() {
    onOneEntityAppearance(SValEquals(types.EntityType(Symbols.keyboard))) { keyboardEntity =>
      keyboardEntity.observe(types.Key_Space){pressed => if (pressed) spawnBall()}
      keyboardEntity.observe(types.Key_t){ pressed =>
        if (pressed) {
          println("[info][EventsAndStateVariables] Test event emitted")
          SpeechEvents.token.emit(types.String("test"), types.Time(10L))
        }
      }
    }
  }

  private def spawnBall() {
    ballCounter += 1
    val randomOffset = Vec3(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) * 0.05f
    Ball("ball#" + ballCounter, ballRadius, ballPosition + randomOffset) realize {
      newBallEntity =>
        newBallEntity.observe(types.Transformation){
          newTransform => if(extractHeight(newTransform) < -2f) newBallEntity.remove()
        }
    }
  }

  private def extractHeight(mat: ConstMat4) = mat.m31

  def storeCameraEntityLocally(userEntity : Entity){
    cameraEntityOption = Some(userEntity)
  }

  private def initializeMouseControl() {
    onOneEntityAppearance(SValEquals(types.EntityType(Symbols.camera)))(storeCameraEntityLocally)
    onOneEntityAppearance(SValEquals(types.EntityType(Symbols.mouse))){ mouseEntity =>
      mouseEntity.observe(types.Position2D){ newMousePosition => cameraEntityOption.collect{
          case userEntity => userEntity.set(types.ViewPlatform(calculateView(newMousePosition)))
        }
      }
    }
  }

  private def calculateView(mousePos: ConstVec2) : ConstMat4 = {
    val weight = 0.1f
    val angleHorizontal = ((mousePos.x - 400f) / -400f) * weight
    val angleVertical = ((mousePos.y - 300f) / -300f) * weight
    Mat4x3.rotateY(angleHorizontal).rotateX(angleVertical)
  }

}