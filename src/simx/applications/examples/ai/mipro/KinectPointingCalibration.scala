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

import java.io.{FileOutputStream, OutputStreamWriter, BufferedWriter, File}

import com.thoughtworks.xstream.XStream
import simx.applications.examples.ai.Servers
import simx.components.ai.mipro.{EntityCreationDSL, Processor, Start}
import simx.components.editor.EditorComponentAspect
import simx.components.renderer.jvr.JVRInit
import simx.components.vrpn.VRPNComponentAspect
import simx.components.vrpn.avatar.KinectFAASTSkeleton_1_2
import simx.core.component.{ExecutionStrategy, Soft}
import simx.core.entity.Entity
import simx.core.helper.{Jfx, Ray, chirality}
import simx.core.ontology._
import simx.core.ontology.types.Position
import simx.core.svaractor.SVarActor
import simx.core.worldinterface.entity.filter.SValEquals
import simx.core.{ApplicationConfig, SimXApplication, SimXApplicationMain}

import scala.collection.immutable
import scala.xml.{Unparsed, TopScope, Null, Elem}

/**
 * Created on 19 Aug 15 
 * by Martin.
 */
object KinectPointingCalibration extends SimXApplicationMain[KinectPointingCalibration]

class KinectPointingCalibration extends SimXApplication with JVRInit with EntityCreationDSL {
  val gfxName = 'renderer

  override protected def applicationConfiguration = ApplicationConfig withComponent
    EditorComponentAspect('editor, appName = "MasterControlProgram") and
    VRPNComponentAspect('vrpn)

  protected def configureComponents(components: immutable.Map[Symbol, SVarActor.Ref]): Unit = {}

  //Some definitions
  val RightHand  = types.EntityType(Symbols.hand) and types.Chirality(chirality.Right)
  val Head       = types.EntityType(Symbols.head)
  val User       = types.EntityType(Symbols.user)

  protected def createEntities(): Unit = {
    KinectFAASTSkeleton_1_2.userDescription("Tracker0@" + Servers.localhost, None).realize()


    var rays = Map[String, List[Ray]]().withDefault((_) => List[Ray]())
    var record = false
    var cornerName = "upper_right"
    onOneEntityAppearance(SValEquals(User)) { userEntity =>
      userEntity.set(types.Enabled.withAnnotations(Symbols.record)(record))
      userEntity.set(types.Boolean.withAnnotations(Symbols.file)(false))
      userEntity.set(types.String(cornerName))

      userEntity.observe(types.Enabled.withAnnotations(Symbols.record)) {record = _}
      userEntity.observe(types.String) {cornerName = _}

      userEntity.observe(types.Ray) { newRay =>
        if(record) {
          rays = rays.updated(cornerName, newRay :: rays(cornerName))
        }
      }

      userEntity.observe(types.Boolean.withAnnotations(Symbols.file)) { saveToFile =>
        if(saveToFile) {
          val rayFile = Jfx.askForFile("Save rays to.", Some("xml")).get

          val content: Set[Elem] = rays.map{ nameRaysTuple =>
            val xStream = new XStream()
            val rayElems = nameRaysTuple._2.map{r => xStream.toXML(r)}.mkString("\n")
            Elem(null, nameRaysTuple._1, Null, TopScope, false, Unparsed(rayElems))
          }.toSet

          val xml = <KinectToScreenCalibration>{content}</KinectToScreenCalibration>

          save(xml, rayFile, prettyPrint = true, addXmlDeclaration = false)
          //println(rays)

          rays = Map[String, List[Ray]]().withDefault((_) => List[Ray]())
          userEntity.set(types.Boolean.withAnnotations(Symbols.file)(false))
        }
      }
    }
  }

  protected def finishConfiguration() {

    Start a new Processor { //that
      Requires property Position from RightHand
      Requires property Position from Head

      Updates the properties of entity describedBy User `with` {
        types.Ray(Ray.fromTo((Position of Head).value, (Position of RightHand).value))
      }
    }
  }

  protected def removeFromLocalRep(e : Entity){}

  private def pretty(xml: Elem) = {
    val prettyPrinter = new scala.xml.PrettyPrinter(80, 2)
    prettyPrinter.format(xml)
  }

  private def save(xml: Elem, file: File, prettyPrint: Boolean = false, addXmlDeclaration: Boolean = false): Unit = {
    var xmlString = if(prettyPrint) pretty(xml) else xml.toString()
    if(addXmlDeclaration)
      xmlString = """<?xml version='1.0' encoding='UTF-8'?>""" + "\n" + xmlString

    val out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))
    try {out.write(xmlString)} finally {out.close()}
  }
}