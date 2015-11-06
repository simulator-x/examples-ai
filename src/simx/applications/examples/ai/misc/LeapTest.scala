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

package simx.applications.examples.ai.misc

/**
 * Created by chrisz on 03/06/15.
 */
import simx.components.editor.EditorComponentAspect
import simx.components.io.leapmotion.{LeapMotionComponent, LeapMotionComponentAspect}
import simx.core.entity.Entity
import simx.core.svaractor.SVarActor._
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}

object LeapTest extends SimXApplicationMain[LeapTest]

class LeapTest() extends SimXApplication {

  val editorName = 'editor
  val leapName = 'leap

  /**
   * Defines the components that SimXApplication has to create
   */
  override protected def applicationConfiguration= ApplicationConfig withComponent
    LeapMotionComponentAspect(leapName) and
    EditorComponentAspect(editorName, appName = "MasterControlProgram")
  /**
   * Called after all components were created
   * @param components the map of components, accessible by their names
   */
  override protected def configureComponents(components: Map[Symbol, Ref]): Unit = {}

  /**
   * Called after components were configured and the creation of entities was initiated
   */
  override protected def finishConfiguration(): Unit = {


  }

  /**
   * Called when the entities are meant to be created
   */
  override protected def createEntities(): Unit = {

    LeapMotionComponent.trackedParts.foreach{bodyPart =>  {
      handleEntityRegistration(bodyPart.toEntityProperties.toFilter)(e => println(e.getSimpleName))
    }}
  }

  override protected def removeFromLocalRep(e: Entity): Unit = {}
}
