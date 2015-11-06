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

import simplex3d.math.floatx.{ConstVec3f, Vec3f}
import simx.components.ai.atn.core.ATNMachineAspect
import simx.components.editor.EditorComponentAspect
import simx.components.renderer.jvr.JVRComponentAspect
import simx.core.component.{Soft, ExecutionStrategy}
import simx.core.components.io.SpeechEvents
import simx.core.components.renderer.setup.BasicDisplayConfiguration
import simx.core.entity.Entity
import simx.core.ontology.{Symbols, types}
import simx.core.svaractor.SVarActor.Ref
import simx.core.{ApplicationConfig, SimXApplicationMain, SimXApplication}
import simx.core.worldinterface.eventhandling.{EventDescription, EventHandler, EventProvider}


object SplitMergeAtnExample extends SimXApplicationMain (new SplitMergeAtnExample) {
  val editorName  = 'editor
  val atnName     = 'splitMergeAtn
  val gfxName     = 'renderer
}

object ExampleEvents {
  val pointingGestureDetected = new EventDescription(Symbols.pointing)
  val command = new EventDescription(Symbols.action)
}

class SplitMergeAtnExample extends SimXApplication with EventHandler with EventProvider {
  import SplitMergeAtnExample._

  protected def applicationConfiguration = ApplicationConfig withComponent
    JVRComponentAspect(gfxName, BasicDisplayConfiguration(640, 480, fullscreen = false)) and
    EditorComponentAspect(editorName) and
    ATNMachineAspect(atnName, new SplitMergeAtn, autoResetAfter = Some(1000000L), drawGraphs = true)

  protected def configureComponents(components: Map[Symbol, Ref]) = {
    start(ExecutionStrategy where
      components(atnName) runs Soft(10) and
      components(gfxName) runs Soft(60))
  }

  protected def createEntities() {}

  protected def finishConfiguration() {
    initKeyboard()
    ExampleEvents.command.observe{event =>
      val pointingDirection = event.values.firstValueFor(types.Direction)
      val entity = getEntityFrom(pointingDirection)
      val command = event.values.getAllValuesFor(types.String)
      execute(command, entity)
    }
  }

  private def initKeyboard(): Unit = {
    handleDevice(types.Keyboard){ keyboardEntity =>
      keyboardEntity.observe(types.Key_1){pressed =>
        if(pressed) SpeechEvents.token.emit(types.String("remove"), types.Time(System.currentTimeMillis()))
      }
      keyboardEntity.observe(types.Key_2){ pressed =>
        if(pressed) SpeechEvents.token.emit(types.String("this"), types.Time(System.currentTimeMillis()))
      }
      keyboardEntity.observe(types.Key_3){ pressed =>
        if(pressed) ExampleEvents.pointingGestureDetected.emit(types.Direction(Vec3f.UnitZ), types.Time(System.currentTimeMillis()))
      }
      keyboardEntity.observe(types.Key_4){ pressed =>
        if(pressed) SpeechEvents.token.emit(types.String("ball"), types.Time(System.currentTimeMillis()))
      }
    }
  }

  private def getEntityFrom(pointingDirection: ConstVec3f): Entity = {
    //Just an example
    //Here entities the entity closest to the pointing direction could be retrieved.
    //You probably require more information than just the direction, e.g. the origin
    new Entity()
  }

  private def execute(command: List[String], e: Entity): Unit = {
    //Just an example
    //Execute the command.
    //Feel free to adapt this very simple example.
    println("[SplitMergeAtnExample] executing command " + command)
  }

  protected def removeFromLocalRep(e: Entity) {}
}
