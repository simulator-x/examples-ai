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

import simplex3d.math.float._
import simx.applications.examples.ai.objects._
import simx.components.editor.EditorComponentAspect
import simx.components.physics.jbullet.JBulletComponentAspect
import simx.components.renderer.jvr.{JVRComponentAspect, JVRInit}
import simx.core.component.{ExecutionStrategy, Soft}
import simx.core.components.physics.ImplicitEitherConversion._
import simx.core.components.renderer.setup.BasicDisplayConfiguration
import simx.core.entity.Entity
import simx.core.ontology.{EntityDescription, Symbols, types}
import simx.core.svaractor.SVarActor
import simx.core.worldinterface.entity.filter._
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}

import scala.collection.immutable
import scala.util.Random

/**
 * Created by
 * martin
 * in September 2015.
 */
object EntityRequestExample extends SimXApplicationMain[EntityRequestExample] {
  val useEditor = true //IO.askForOption("Use Editor Component?")
}

class EntityRequestExample(args : Array[String]) extends SimXApplication with JVRInit
{
  val physicsName = 'physics
  val gfxName = 'renderer

  override protected def applicationConfiguration = ApplicationConfig withComponent
    JVRComponentAspect(gfxName, BasicDisplayConfiguration(1280, 800, fullscreen = false)) /*on "renderNode"*/ and
    JBulletComponentAspect(physicsName, ConstVec3(0, -9.81f, 0)) /*on "physicsNode"*/ and
    EditorComponentAspect('editor, appName = "MasterControlProgram") iff EntityRequestExample.useEditor

  protected def configureComponents(components: immutable.Map[Symbol, SVarActor.Ref]) {
    exitOnClose(components(gfxName), shutdown) // register for exit on close
    start(ExecutionStrategy where
      components(physicsName) runs Soft(60) and
      components(gfxName) runs Soft(60)
    )
  }

  private var tableEntityOption: Option[Entity] = None
  private val ballRadius = 0.2f
  private val ballPosition = ConstVec3(0f, 1.5f, -7f)

  import simx.core.helper.chirality
  val entityTypeProperty =
    types.EntityType.withAnnotations(types.RelativeTo(Symbols.elbow), types.Chirality(chirality.Right))(Symbols.hand)
  val chiralityProperty = types.Chirality(chirality.Right)

  protected def createEntities() {
    Ball("theBall", ballRadius, ballPosition).realize(entityComplete)
    Light("the light", Vec3(-4f, 8f, -7f), Vec3(270f, -25f, 0f)).realize(entityComplete)
    Table("the table", Vec3(3f, 1f, 2f), Vec3(0f, -1.5f, -7f)).realize((tableEntity: Entity) => {
      entityComplete(tableEntity)
      tableEntityOption = Some(tableEntity)
    })

    new EntityDescription('rightHandRelativeToRightElbow, entityTypeProperty, chiralityProperty).realize(println)
  }

  protected def finishConfiguration() {
    rotateTable()
    initializeBallSpawning()
    initializeMouseControl()
    println("[info][EntityRegistryTest] Application is running: Press SPACE to spawn new balls!")
  }

  private def requestEntities(): Unit = {


    requestRegisteredEntities(NameContains("ball")){ entities =>
      println("Got " + entities.size + " balls: " + entities.map(_.getSimpleName).mkString(", "))
    }

    requestRegisteredEntities(HasSVal(types.Transformation)){ entities =>
      println("Got " + entities.size + " entities that have a transformation: " + entities.map(_.getSimpleName).mkString(", "))
    }

    requestRegisteredEntities(!HasSVal(types.Transformation)){ entities =>
      println("Got " + entities.size + " entities that have NO transformation: " + entities.map(_.getSimpleName).mkString(", "))
    }

    requestRegisteredEntities(SValEquals(entityTypeProperty) and SValEquals(chiralityProperty)) { entities =>
      println("Got " + entities.size + " entities that qualify as 'rightHandRelativeToRightElbow': " + entities.map(_.getSimpleName).mkString(", "))
    }
  }

  private def entityComplete(e: Entity) {println("[info][EntityRegistryTest] Completed entity " + e)}
  protected def removeFromLocalRep(e : Entity){println("[info][EntityRegistryTest] Removed entity " + e)}

  var f = 0.1f
  private def rotateTable() : Unit = {
    addJobIn(16L){
      if (tableEntityOption.isDefined){
        val tableEntity = tableEntityOption.get

        //In complex applications it is reasonable to check if the list is not empty, rather than just call 'head'
        tableEntity.get(types.Transformation).head(
          currentTransform => tableEntity.set(types.Transformation(rotate(currentTransform))))
      }
      rt()
    }
  }

  val rt : () => Unit  = () => rotateTable()

  private def rotate(mat: ConstMat4) =
    mat * ConstMat4(Mat4x3.rotateY(0.01f))

  private def initializeBallSpawning() {
    handleDevice(types.Keyboard){ keyboardEntity =>
      keyboardEntity.observe(types.Key_Space).head( pressed => if(pressed) spawnBall() )
      keyboardEntity.observe(types.Key_g).head( pressed => if(pressed) requestEntities() )
    }
  }

  private var ballCounter = 0

  private def spawnBall() {
    ballCounter += 1
    val randomOffset = Vec3(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) * 0.05f
    Ball("ball#" + ballCounter, ballRadius, ballPosition + randomOffset) realize {
      newBallEntity =>
        newBallEntity.observe(types.Transformation).head {
          newTransform => if(extractHeight(newTransform) < -2f) newBallEntity.remove()
        }
    }
  }

  private def extractHeight(mat: ConstMat4) = mat.m31

  private var userEntityOption: Option[Entity] = None

  def doIt(userEntity : Entity){
    userEntityOption = Some(userEntity)
  }

  private def initializeMouseControl() {
    handleDevice(types.User)(doIt)
    handleDevice(types.Mouse){ mouseEntity =>
      mouseEntity.observe(types.Position2D).head{
        newMousePosition => userEntityOption.collect{
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

