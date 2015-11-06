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

package simx.applications.examples.ai.mipro.supervisedLearning.imageviewer

import java.io.{FileFilter, File}
import java.util.UUID

import simplex3d.math.float._
import simx.applications.examples.ai.mipro.supervisedLearning.imageviewer.animation.{Rotator, Zoomer, Slider}
import simx.components.renderer.gui.{QuadShape, objects}
import simx.components.renderer.gui.objects.{Light, Panel}
import simx.components.renderer.jvr.{PPE, PostProcessingEffect}
import simx.core.components.renderer.setup.DisplaySetupDesc
import simx.core.entity.Entity
import simx.core.entity.component.EntityCreationHandling
import simx.core.ontology.{EntityDescription, types}
import simx.core.svaractor.SVarActor
import simx.core.svaractor.unifiedaccess.EntityUpdateHandling
import simx.core.worldinterface.eventhandling.{EventHandler, Event}
import simx.core.worldinterface.naming.NameIt
import simx.components.renderer.gui.ontology.{types => gt}


/**
 * Created by ChrisZ on 29/10/2015.
 */
class ImageViewer(displaySetup: DisplaySetupDesc) extends SVarActor with EventHandler with EntityCreationHandling with EntityUpdateHandling {

  private val imageViewerRef = self

  private val screen = try {
    displaySetup.deviceGroups.head._2.dpys.head.displayDescs.head
  } catch {case _ : Throwable => throw new Exception("[GUI Component] At least one display is required.")}
  private val screenSize =  ConstVec2(screen.size._1.toFloat, screen.size._2.toFloat)
  private def nativeShapeOf(imgFile: File) = Panel.nativeShapeOf(displaySetup)(imgFile)

  private val base = new File("assets/images")
  private val images = base.listFiles(new FileFilter {
    override def accept(pathname: File) =
      pathname.getAbsolutePath.endsWith(".jpg") || pathname.getAbsolutePath.endsWith(".png")
  })

  private var currentImageIdx = 0
  private def currentImage = images(currentImageIdx)
  private def advance(nrOfImages: Int = 1) {
    currentImageIdx = currentImageIdx + nrOfImages
    while(currentImageIdx < 0) currentImageIdx += images.size
    while(currentImageIdx >= images.size) currentImageIdx -= images.size
  }

  private var selectionShader: Option[Entity] = None

  private var operations: List[Any] = Nil
  private var operationInProcess = false
  private var selected: Option[Float] = None

  private var currentImageEntity: Option[Entity] = None

  val startTime = System.currentTimeMillis()

  override protected def startUp(): Unit = {
    Light("light", Vec3(0f, 0f, 10f)).realize()

    val shaderEffect =
      PostProcessingEffect( "Selection" ).
        describedByShaders( "pipeline_shader/quad.vs" :: "assets/shader/selection.fs" :: Nil ).
        usingColorBufferAsName( "sceneMap" ).
        where( "time" ).
        hasValue( -1f ).
        isReachableBy(types.TimeInSeconds).
        and( "enabled" ).
        hasValue( false ).
        isReachableBy(types.Boolean).
        pack

    new EntityDescription(PPE( ppe = shaderEffect ), NameIt("SelectionShader")).realize(e => {
      selectionShader = Some(e)})

    updateShaderTime()
    self ! IterateImage()
  }


  private def updateShaderTime() {
    val t = (System.currentTimeMillis() - startTime).toFloat / 1000f
    selectionShader.collect{ case shader =>
      shader.set(types.TimeInSeconds(t.toFloat))}
    addJobIn(16){updateShaderTime()}
  }

  private def setSelectionFrameStatus(enabled: Boolean) {
    selectionShader.collect{
      case shader => shader.set(types.Boolean(enabled))}
  }

  addHandler[IterateImage](msg => {
    selected = None
    setSelectionFrameStatus(enabled = false)
    advance(if(msg.next) 1 else -1)
    objects.ImagePanel(
       name = "Image" + currentImage.getName + "-" + UUID.randomUUID().toString,
       imageFile = currentImage,
       pos = Vec2(if(msg.next) -screenSize.x else screenSize.x, 0f),
       shape = nativeShapeOf(currentImage)
     ).realize(e => {
      val previousImageEntity = currentImageEntity
      currentImageEntity = Some(e)
      SVarActor.createActor(new Slider(
        leftToRight = msg.next,
        screenSize = screenSize,
        simulationSpeed = msg.simulationSpeed,
        currentImageEntity = e,
        previousImageEntity = previousImageEntity,
        imageViewer = imageViewerRef
      ))
    })
  })

  addHandler[Zoom](msg => {
    currentImageEntity.collect{case cie => SVarActor.createActor(new Zoomer(msg.scaleFactor, cie, imageViewerRef))}
  })

  addHandler[Rotate](msg => {
    currentImageEntity.collect{case cie => SVarActor.createActor(new Rotator(-90, cie, imageViewerRef))}
  })

  addHandler[Select](msg => {
    currentImageEntity.collect{case cie =>
      cie.get(gt.Shape).head(
        (currentShape) => {
          val size = currentShape match {
            case qs: QuadShape => qs.size
            case _ => throw new Exception("[ImageViewer] Only QuadShapes are supported.")
          }
          val scaleFactor = selected.map(1/_).getOrElse(screenSize.y / size.y)
          if(selected.isDefined) {
            selected = None
            setSelectionFrameStatus(enabled = false)
          } else {
            selected = Some(scaleFactor)
            setSelectionFrameStatus(enabled = true)
          }
          SVarActor.createActor(new Zoomer(scaleFactor, cie, imageViewerRef))
        }
      )
  }})

  addHandler[Done](msg => {
    operationInProcess = false
    applyOperationIfPossible()
  })

  private def addOperation(op: Any) {
    operations = op :: operations
    applyOperationIfPossible()
  }

  private def applyOperationIfPossible() {
    if(!operationInProcess && operations.nonEmpty) {
      imageViewerRef ! operations.last
      operations = operations.init
      operationInProcess = true
    }
  }

  EventDescriptions.fastBackward.observe{
    case event => 1 to 3 foreach(_ => addOperation(IterateImage(next = false, 2.5f)))
  }

  EventDescriptions.fastForward.observe{
    case event => 1 to 3 foreach(_ => addOperation(IterateImage(next = true, 2.5f)))
  }

  EventDescriptions.previous.observe{
    case event => addOperation(IterateImage(next = false))
  }

  EventDescriptions.next.observe{
    case event => addOperation(IterateImage(next = true))
  }

  EventDescriptions.select.observe{
    case event => addOperation(Select())
  }

  EventDescriptions.zoomIn.observe{
    case event => addOperation(Zoom(2f))
  }

  EventDescriptions.zoomOut.observe{
    case event => addOperation(Zoom(0.5f))
  }

  EventDescriptions.rotate.observe{
    case event => addOperation(Rotate())
  }

  override protected def removeFromLocalRep(e: Entity): Unit = {}

}
