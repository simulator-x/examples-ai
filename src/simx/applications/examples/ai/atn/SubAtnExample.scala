/*
 * Copyright 2014 The SIRIS Project
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

package simx.applications.examples.ai.atn

import simplex3d.math.floatx.{Vec3f, Mat4x3f, ConstMat4f}
import simx.components.ai.atn.core.ATNMachineAspect
import simx.components.editor.EditorComponentAspect
import simx.components.renderer.jvr.JVRComponentAspect
import simx.core.component.{Soft, ExecutionStrategy}
import simx.core.components.io.SpeechEvents
import simx.core.components.renderer.createparameter.{PointLight, ShapeFromFile}
import simx.core.components.renderer.setup.BasicDisplayConfiguration
import simx.core.entity.Entity
import simx.core.ontology.{EntityDescription, Symbols, types}
import simx.core.svaractor.SVarActor.Ref
import simx.core.worldinterface.entity.filter.SValEquals
import simx.core.{ApplicationConfig, SimXApplicationMain, SimXApplication}
import simx.core.worldinterface.eventhandling.{EventDescription, EventHandler, EventProvider}
import simx.core.components.renderer.createparameter.convert._

import scala.util.Random


object SubAtnExample extends SimXApplicationMain (new SubAtnExample) {
  val editorName = 'editor
  val atnName = 'simpleAtn
  val gfxName = 'renderer
}


class SubAtnExample extends SimXApplication with EventHandler with EventProvider {

  import SubAtnExample._

  protected def applicationConfiguration = ApplicationConfig withComponent
    JVRComponentAspect(gfxName, BasicDisplayConfiguration(640, 480, fullscreen = false)) and
    EditorComponentAspect(editorName) and
    ATNMachineAspect(atnName, new SubAtn, autoResetAfter = None, drawGraphs = true)


  protected def configureComponents(components: Map[Symbol, Ref]) = {
    start(ExecutionStrategy where
      components(atnName) runs Soft(10) and
      components(gfxName) runs Soft(60))
  }

  protected def createEntities() {}

  protected def finishConfiguration() {
    initKeyboard()
    initLight()

    Commands.createSphere.observe{ event =>
      new EntityDescription("a ball",
        ShapeFromFile(
          file = "assets/vis/ball.dae",
          transformation = ConstMat4f(Mat4x3f.translate(Vec3f(rnd, rnd, -10f)))
        )
      ).realize()
    }
  }

  private def rnd = getNextRnd()

  private def getNextRnd(min: Float = -1f, max: Float = 1f): Float = {
    assert(min < max)
    val delta = max - min
    min + Random.nextFloat() * delta
  }

  private def initKeyboard(): Unit = {
    onOneEntityAppearance(SValEquals(types.EntityType(Symbols.keyboard))) { keyboardEntity =>
      keyboardEntity.observe(types.Key_1){ pressed =>
        if(pressed) SpeechEvents.token.emit(types.String("create"), types.Time(System.currentTimeMillis()))
      }
      keyboardEntity.observe(types.Key_2){ pressed =>
        if(pressed) SpeechEvents.token.emit(types.String("a"), types.Time(System.currentTimeMillis()))
      }
      keyboardEntity.observe(types.Key_3){ pressed =>
        if(pressed) SpeechEvents.token.emit(types.String("sphere"), types.Time(System.currentTimeMillis()))
      }
    }
  }

  private def initLight(): Unit = {
    new EntityDescription("the light",
      PointLight(
        name = "the light",
        transformation = ConstMat4f(Mat4x3f.Identity)
      )
    ).realize()
  }

  protected def removeFromLocalRep(e: Entity) {}
}
