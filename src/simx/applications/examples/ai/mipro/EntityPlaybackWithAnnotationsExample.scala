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
 * in June 2015.
 */

import simx.components.ai.feature.recording.Events._
import simx.components.ai.feature.recording._
import simx.components.ai.feature.recording.storage.Persistence
import simx.components.ai.feature.sl_chris.{AnnotationSource, AnnotationReader}
import simx.components.ai.mipro.{EntityCreationDSL, EventDSL, Processor, Start}
import simx.components.editor.EditorComponentAspect
import simx.components.renderer.jvr.JVRInit
import simx.core.entity.Entity
import simx.core.helper.{Jfx, IO, chirality}
import simx.core.ontology._
import simx.core.svaractor.SVarActor
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}

import scala.collection.immutable

object EntityPlaybackWithAnnotationsExample extends SimXApplicationMain[EntityPlaybackWithAnnotationsExample]

class EntityPlaybackWithAnnotationsExample() extends SimXApplication with JVRInit  {

  val playbackDataFile        = Jfx.askForFile("Choose playback file", Some("xml")).get
  val playbackData            = Persistence.load(playbackDataFile)
  val playbackAnnotationFile  = IO.changeExtension(playbackDataFile, "csv")
  val annotation              = new AnnotationReader(playbackAnnotationFile, playbackData.metaData)

//  val playbackDataFolder        = Jfx.askForFolder("Choose playback files folder").get
//  val playbackDataFiles         = playbackDataFolder.listFiles().filter(_.getAbsolutePath.endsWith(".xml"))
//  val playbackData              = Persistence.load(playbackDataFiles)
//  val playbackAnnotationSources = playbackData.metaData.map(AnnotationSource.from(_))
//  val annotation                = new AnnotationReader(playbackAnnotationSources)

  var entityPlayer: Option[SVarActor.Ref] = None

  override protected def applicationConfiguration = ApplicationConfig withComponent
    EditorComponentAspect('editor, appName = "MasterControlProgram")

  protected def configureComponents(components: immutable.Map[Symbol, SVarActor.Ref]): Unit = {
    entityPlayer = Some(SVarActor.createActor(new EntityPlayer(playbackData)))
  }

  protected def createEntities(): Unit = {}

  val RightHand      = types.EntityType(Symbols.hand)     and types.Chirality(chirality.Right)
  val LeftHand       = types.EntityType(Symbols.hand)     and types.Chirality(chirality.Left)
  val Spine          = types.EntityType(Symbols.spine)
  val User           = types.EntityType(Symbols.user)

  protected def finishConfiguration() {

    Start a new Processor with EntityCreationDSL with EventDSL {
      
      Requires property types.Transformation from LeftHand
      Requires property types.Transformation from RightHand
      Requires property types.Transformation from Spine

      Creates entity `with` property User named 'User

      Updates the properties of User `with` {
        if(annotation("rotate").isPresent)
          types.Gesture(Symbols.rotate)
        else
          types.Gesture(Symbols.idle)
      }

      Reacts to event describedBy playbackFinished by { println("Playback finished.") }
    }
  }

  protected def removeFromLocalRep(e : Entity){}
}