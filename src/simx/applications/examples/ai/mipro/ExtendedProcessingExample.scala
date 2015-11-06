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

/**
 * Created by
 * Martin Fischbach
 * in September 2015.
 */
import simx.applications.examples.ai.Servers
import simx.components.ai.mipro.{EntityCreationDSL, Processor, Start}
import simx.components.editor.EditorComponentAspect
import simx.components.renderer.jvr.JVRInit
import simx.components.vrpn.VRPNComponentAspect
import simx.components.vrpn.avatar.KinectFAASTSkeleton_1_2
import simx.core.entity.Entity
import simx.core.helper.{EntityObservation, chirality}
import simx.core.ontology._
import simx.core.ontology.functions.Interpolators
import simx.core.svaractor.SVarActor
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}

import scala.collection.immutable

object ExtendedProcessingExample extends SimXApplicationMain[ExtendedProcessingExample]

class ExtendedProcessingExample(args : Array[String]) extends SimXApplication with JVRInit with EntityObservation {
  
  override protected def applicationConfiguration = ApplicationConfig withComponent
    VRPNComponentAspect('vrpn)  and
    EditorComponentAspect('editor, appName = "MasterControlProgram")

  protected def configureComponents(components: immutable.Map[Symbol, SVarActor.Ref]): Unit = {}

  val RightHand = types.EntityType(Symbols.hand) and types.Chirality(chirality.Right)
  val LeftHand  = types.EntityType(Symbols.hand) and types.Chirality(chirality.Left)
  val Spine     = types.EntityType(Symbols.spine)
  
  protected def createEntities() {
    KinectFAASTSkeleton_1_2.simpleUpperBodyUserDescription("Tracker0@" + Servers.fishTank).realize()

    Start a new Processor with EntityCreationDSL { //that
      Requires property types.Position from RightHand
      Requires property types.Transformation from Spine
      
      Creates entity `with` property types.EntityType(Symbols.relative) named 'RightHandRelativeToSpine
      
      Updates the properties of entity describedBy types.EntityType(Symbols.debug) `with`{
        (types.Position of RightHand) relativeTo (types.Transformation of Spine)
      }
    }
    
    Start a new Processor with EntityCreationDSL { //that
      Requires property types.Position from RightHand
      Requires property types.Position from LeftHand
      Requires property types.Transformation from Spine

      Creates entity `with` property types.EntityType(Symbols.user) named 'User

      Updates the properties of entity describedBy types.EntityType(Symbols.user) `with`{
        val leftVector  = (types.Position of LeftHand)  - (types.Position of Spine)
        val rightVector = (types.Position of RightHand) - (types.Position of Spine)
        types.Angle between (leftVector, rightVector)
      }
    }

    Start a new Processor with Interpolators { //that
      Requires property types.Position from LeftHand
      
      val deltaT      = types.Milliseconds(60L)
      val deltaTinSec = deltaT.value.toFloat / 1000f

      Updates the properties of LeftHand `with` {
        val deltaP = (types.Position of LeftHand at types.Milliseconds(0)).value -
                     (types.Position of LeftHand at deltaT).value
        types.Velocity(deltaP / deltaTinSec)
      }
    }
  }

  protected def finishConfiguration() {}

  protected def removeFromLocalRep(e : Entity){}
}