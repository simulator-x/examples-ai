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

package simx.applications.examples.ai.feature

import simx.components.renderer.jvr.JVRInit
import simx.core.{ApplicationConfig, SimXApplicationMain, SimXApplication}
import scala.collection.immutable
import simx.components.ai.feature._
import simx.core.ontology.types
import simplex3d.math.float._
import simplex3d.math.float.functions._
import simx.core.worldinterface.eventhandling.{EventProvider, EventHandler}
import simx.core.svaractor.SVarActor
import simx.core.components.io.SpeechEvents
import simx.core.component.ExecutionStrategy
import simx.core.component.Soft
import simx.components.vrpn.VRPNComponentAspect
import simx.components.editor.EditorComponentAspect
import simx.components.ai.atn.core.ATNMachineAspect
import simx.core.entity.Entity

/**
 * @author Martin Fischbach
 */
object FeatureTest extends SimXApplicationMain( new FeatureTest )

class FeatureTest extends SimXApplication with EventHandler with EventProvider
with ExamplesAIATN with ExamplesAIFeatures {

  //override protected implicit val actorContext = this

  val featureName = 'featureComponent
  val vrpnName = 'vrpnComponent
  val editorName = 'editor

  val remoteEventProvidersIp = "132.187.8.153"
  val remoteEventProvidersPort = 9000

  val ioTrackerToWorldOffsetInWorldCS = ConstVec3(0f, 0.66f, -2.015f)
  val ioTrackerToWorldRotationInWorldCS = ConstQuat4(Quat4.rotateZ(radians(90))) * ConstQuat4(Quat4.rotateX(radians(90)))
  val ioTrackerToWorldScale = ConstVec3(Vec3.One) * 0.001f

  /**
   * Defines the components that [[simx.core.SimXApplication]] has to create
   */
  protected def applicationConfiguration = ApplicationConfig withComponent
    VRPNComponentAspect(vrpnName) and
    FeatureComponentAspect(featureName,
      ioTrackerToWorldOffsetInWorldCS, ioTrackerToWorldRotationInWorldCS, ioTrackerToWorldScale, debug = true) and
    EditorComponentAspect(editorName, "FeatureTest") and
    ATNMachineAspect(atnName, this, Some(4100L),  true)

  protected def configureComponents(components: immutable.Map[Symbol, SVarActor.Ref]) {
    println("[Configuring components]")
    start(ExecutionStrategy where components(atnName) runs Soft(10))
  }

  protected def createEntities() {
    realizeSCFeatures()
  }

  protected def finishConfiguration() {
    println("[Configuring application]")
    SpeechEvents.token.observe(e => println("Token: " + e.get(types.String).get))
    //requestInputFeatureEvents()
    println("[Application is running]")
  }

  protected def removeFromLocalRep(e : Entity){}

//  private def requestInputFeatureEvents() {
//    val posDesc = types.Position.withAnnotations(Symbols.hand, Symbols.right)
//    val oriDesc = local.Orientation.withAnnotations(Symbols.hand, Symbols.right)
//    val posEventDesc = FeatureEventDescription(posDesc)
//    val oriEventDesc = FeatureEventDescription(oriDesc)
//    requestEvent(posEventDesc)
//    requestEvent(oriEventDesc)
//  }
}