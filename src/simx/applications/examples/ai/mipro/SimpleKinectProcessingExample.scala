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

import simx.applications.examples.ai.Servers
import simx.components.ai.mipro.implementations.{AccelerationProcessor, VelocityProcessor}
import simx.components.ai.mipro.{EntityCreationDSL, Processor, Start}
import simx.components.editor.EditorComponentAspect
import simx.components.renderer.jvr.{JVRComponentAspect, JVRInit}
import simx.components.vrpn.VRPNComponentAspect
import simx.components.vrpn.avatar.KinectFAASTSkeleton_1_2
import simx.core.component.{ExecutionStrategy, Soft}
import simx.core.components.renderer.setup.BasicDisplayConfiguration
import simx.core.entity.Entity
import simx.core.helper.chirality
import simx.core.ontology._
import simx.core.svaractor.SVarActor
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}

import scala.collection.immutable

/**
 * Created on 19 Aug 15 
 * by Martin.
 */
object SimpleKinectProcessingExample extends SimXApplicationMain[SimpleKinectProcessingExample]

class SimpleKinectProcessingExample extends SimXApplication with JVRInit {
  val gfxName = 'renderer

  override protected def applicationConfiguration = ApplicationConfig withComponent
    JVRComponentAspect(gfxName, BasicDisplayConfiguration(640, 480, fullscreen = false)) and
    EditorComponentAspect('editor, appName = "MasterControlProgram") and
    VRPNComponentAspect('vrpn)

  protected def configureComponents(components: immutable.Map[Symbol, SVarActor.Ref]): Unit = {
    exitOnClose(components(gfxName), shutdown) // register for exit on close
    start(ExecutionStrategy where components(gfxName) runs Soft(60))
  }

  protected def createEntities(): Unit = {
    KinectFAASTSkeleton_1_2.userDescription("Tracker0@" + Servers.fishTank).realize()
  }

  //Some definitions
  val RightHand  = types.EntityType(Symbols.hand) and types.Chirality(chirality.Right)
  val Spine      = types.EntityType(Symbols.spine)

  val RightHandRelative =
    types.EntityType.withAnnotations(types.RelativeTo(Symbols.spine))(Symbols.hand) and types.Chirality(chirality.Right)

  protected def finishConfiguration() {

    Start a new VelocityProcessor(RightHand)
    Start a new AccelerationProcessor(RightHand)

    Start a new Processor with EntityCreationDSL { //that
      Is named 'RelativeToProcessor

      Requires property types.Position from RightHand
      Requires property types.Transformation from Spine

      Creates entity `with` properties RightHandRelative named 'RightHandRelative

      Updates the properties of entity describedBy RightHandRelative `with` {
        (types.Position of RightHand) relativeTo (types.Transformation of Spine)
      }
    }
  }

  protected def removeFromLocalRep(e : Entity){}
}