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
import simx.components.ai.feature.sl_chris.AnnotationReader
import simx.components.ai.mipro.{EntityCreationDSL, EventDSL, Processor, Start}
import simx.components.editor.EditorComponentAspect
import simx.components.renderer.jvr.JVRInit
import simx.core.entity.Entity
import simx.core.helper.{IO, chirality}
import simx.core.ontology._
import simx.core.svaractor.SVarActor
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}

import scala.collection.immutable

object EntityPlaybackWithAnnotationsExample extends SimXApplicationMain[EntityPlaybackWithAnnotationsExample]

class EntityPlaybackWithAnnotationsExample() extends SimXApplication with JVRInit  {

  val playbackDataFile        = IO.askForFile("Choose playback file", IO.extensionFilter("bin")).get
  val playbackData            = Persistence.load(playbackDataFile)
  val playbackAnnotationFile  = IO.changeExtension(playbackDataFile, "csv")
  val annotation              = new AnnotationReader(playbackAnnotationFile, playbackData.recordingStart.get)
  
  var entityPlayer: Option[SVarActor.Ref] = None

  override protected def applicationConfiguration = ApplicationConfig withComponent
    EditorComponentAspect('editor, appName = "MasterControlProgram")

  protected def configureComponents(components: immutable.Map[Symbol, SVarActor.Ref]): Unit = {
    entityPlayer = Some(SVarActor.createActor(new EntityPlayer(playbackData)))
  }

  protected def createEntities(): Unit = {}

  protected def finishConfiguration() {

    Start a new Processor with EntityCreationDSL with EventDSL {
      
      Requires property types.Transformation from entity describedBy (types.EntityType(Symbols.hand) and types.Chirality(chirality.Left))
      Requires property types.Transformation from entity describedBy (types.EntityType(Symbols.hand) and types.Chirality(chirality.Right))
      Requires property types.Transformation from entity describedBy types.EntityType(Symbols.spine)

      Creates entity `with` property types.EntityType(Symbols.gesture) named 'Rotation

      Updates the properties of entity describedBy types.EntityType(Symbols.gesture) `with` {
        types.Boolean(annotation("rotate").isPresent)
      }

      Reacts to event describedBy playbackFinished by { println("Playback finished.") }
    }

    addJobIn(5000L){
      entityPlayer.foreach(_ ! StartPlayback())
    }
  }

  protected def removeFromLocalRep(e : Entity){}
}