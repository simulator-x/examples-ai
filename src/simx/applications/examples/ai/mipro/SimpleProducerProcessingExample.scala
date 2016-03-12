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

import simplex3d.math.float.{functions, _}
import simx.components.ai.mipro._
import simx.components.editor.EditorComponentAspect
import simx.components.renderer.jvr.JVRInit
import simx.core.entity.Entity
import simx.core.helper.chirality
import simx.core.ontology._
import simx.core.svaractor.SVarActor
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}

import scala.collection.immutable

/**
 * Created by
 * martin
 * in September 2015.
 */
object SimpleProducerProcessingExample extends SimXApplicationMain[SimpleProducerProcessingExample]

class SimpleProducerProcessingExample extends SimXApplication with JVRInit {

  override protected def applicationConfiguration = ApplicationConfig withComponent
    EditorComponentAspect('editor, appName = "MasterControlProgram")

  protected def configureComponents(components: immutable.Map[Symbol, SVarActor.Ref]): Unit = {}

  //Some definitions
  val RightHand = types.EntityType(Symbols.hand) and types.Chirality(chirality.Right)

  protected def createEntities(): Unit = {}

  protected def finishConfiguration() {
    Start a new Producer with EntityCreationDSL { //that
      Is named 'Rotator

      Creates entity `with` properties RightHand named 'RightHand

      Updates the properties of RightHand every types.Milliseconds(16L) `with` {
        val rawAngle: Float = functions.radians(Context.elapsedTime.value.toFloat / 10f)
        types.Angle(rawAngle)
      }
    }

    Start a new Processor { //that
      Requires property types.Angle from RightHand
      Updates the properties of RightHand `with` {
        val rawPosition = Mat4x3.rotateZ((types.Angle of RightHand).value) * Vec4.UnitX
        types.Position(rawPosition.xyz)
      }
    }
  }

  protected def removeFromLocalRep(e : Entity){}
}