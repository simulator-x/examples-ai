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
 * martin
 * in September 2015.
 */
import java.io.File

import simx.applications.examples.ai.Servers
import simx.components.ai.feature.recording._
import simx.components.editor.EditorComponentAspect
import simx.components.io.j4k.{KinectComponentAspect, KinectVideoFrameEntityDescription}
import simx.components.io.leapmotion.{LeapMotionComponent, LeapMotionComponentAspect}
import simx.components.renderer.jvr.JVRInit
import simx.components.vrpn.VRPNComponentAspect
import simx.components.vrpn.avatar.KinectFAASTSkeleton_1_2
import simx.core.entity.Entity
import simx.core.helper.{IO, Jfx}
import simx.core.ontology._
import simx.core.svaractor.SVarActor
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}

import scala.collection.immutable

object EntityRecordingExample extends SimXApplicationMain[EntityRecordingExample]

class EntityRecordingExample() extends SimXApplication with JVRInit  {
  
  val editorName = 'editor
  val vrpnName = 'vrpn
  val j4kName = 'j4k
  val leapName = 'leapName

  val useKinect = IO.askForOptions("Specify device ...","Kinect V2", "Leap Motion") == 0
  val recordVideo = IO.askForOption("Record Video") //false
  val recordingsDefaultFolder = new File("recordings")
  var fullUpperBody = false
  if(useKinect) fullUpperBody = IO.askForOptions("Record ...", "Full Upper Body", "Head, Spine & Hands") == 0

  val recordingsFolder = Jfx.askForFolder("Store recordings in ...").getOrElse(recordingsDefaultFolder)

  override protected def applicationConfiguration = ApplicationConfig withComponent
    VRPNComponentAspect(vrpnName) iff useKinect  and
    LeapMotionComponentAspect(leapName) iff !useKinect and
    EditorComponentAspect(editorName, appName = "MasterControlProgram") and
    KinectComponentAspect(j4kName) iff recordVideo

  var entityRecorder: Option[SVarActor.Ref] = None

  protected def configureComponents(components: immutable.Map[Symbol, SVarActor.Ref]): Unit = {
    entityRecorder = Some(SVarActor.createActor(new EntityRecorder(defaultFolder = recordingsFolder)))
  }

  def bodyParts =
    if(fullUpperBody) KinectFAASTSkeleton_1_2.upperBody else KinectFAASTSkeleton_1_2.simpleUpperBody

  protected def createEntities(): Unit = {
    if(useKinect) {
      if(fullUpperBody) KinectFAASTSkeleton_1_2.upperBodyUserDescription("Tracker0@" + Servers.workstation).realize()
      else KinectFAASTSkeleton_1_2.simpleUpperBodyUserDescription("Tracker0@" + Servers.workstation).realize()
    }
    if(recordVideo) KinectVideoFrameEntityDescription().realize(addToRecordingSet)
  }

  protected def finishConfiguration() {
    if(useKinect) requestKinectEntitiesToRecord() else requestLeapEntitiesToRecord()
  }

  private def requestKinectEntitiesToRecord(): Unit = {
    bodyParts.foreach{ bodyPart =>
      //Requests all entities that have been registered so far and satisfy the passed filter.
      //In addition, the passed handler is applied for all entities that will satisfy the passed filter.
      handleEntityRegistration(bodyPart.getEP.toFilter)(addToRecordingSet)
      bodyPart.subParts.foreach{subPart =>  handleEntityRegistration(subPart.getEP.toFilter)(addToRecordingSet)}
    }
  }

  private def requestLeapEntitiesToRecord() {
    LeapMotionComponent.trackedParts.foreach{bodyPart =>  {
      handleEntityRegistration(bodyPart.toEntityProperties.toFilter)(addToRecordingSet)
    }}
  }
  
  private def addToRecordingSet(e: Entity): Unit = {
    //Applies the passed function only if recorder is not None
    entityRecorder.foreach{recorder => recorder ! Record(e)}
  }

  protected def removeFromLocalRep(e : Entity){}
}