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
import simx.core.entity.description.SValSet
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

  override protected def applicationConfiguration = ApplicationConfig withComponent
    EditorComponentAspect('editor, appName = "MasterControlProgram") and
    VRPNComponentAspect('vrpn)

  protected def configureComponents(components: immutable.Map[Symbol, SVarActor.Ref]): Unit = {}

  protected def createEntities(): Unit = {
    KinectFAASTSkeleton_1_2.userDescription("Tracker0@" + Servers.workstation, None).realize()
  }

  //Some definitions
  val RightHand      = types.EntityType(Symbols.hand)     and types.Chirality(chirality.Right)
  val RightElbow     = types.EntityType(Symbols.elbow)    and types.Chirality(chirality.Right)
  val RightShoulder  = types.EntityType(Symbols.shoulder) and types.Chirality(chirality.Right)
  val Spine          = types.EntityType(Symbols.spine)
  val User           = types.EntityType(Symbols.user)

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

    Start a new Processor { //that
      Requires property types.Transformation from RightHand
      Requires property types.Transformation from RightShoulder
      Requires property types.Transformation from RightElbow

      Updates the properties of User `with` {
        val shoulderT = (types.Transformation of RightShoulder).value
        val handT     = (types.Transformation of RightHand).value
        val elbowT    = (types.Transformation of RightElbow).value

        val shoulderP = shoulderT(3).xyz
        val handP     = handT(3).xyz
        val elbowP    = elbowT(3).xyz

        val angle  = types.Angle between (types.Vector3(handP - elbowP), types.Vector3(shoulderP - elbowP))
        val angleD = simplex3d.math.float.functions.degrees(angle.value)

        types.Angle(angleD)
      }
    }

  }

  protected def removeFromLocalRep(e : Entity){}
}