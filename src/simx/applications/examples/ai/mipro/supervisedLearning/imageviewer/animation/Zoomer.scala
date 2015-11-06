/*
 * Copyright 2014 The SIRIS Project
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

package simx.applications.examples.ai.mipro.supervisedLearning.imageviewer.animation

import simx.components.renderer.gui.{QuadShape, Shape}
import simx.core.svaractor.SVarActor
import simx.core.entity.Entity
import simx.components.renderer.gui.ontology.{types => gt}

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 16/03/14
 * Time: 14:46
 *
 */
class Zoomer(scaleFactor: Float, currentImageEntity: Entity, imageViewerRef: SVarActor.Ref)
  extends LinearInterpolator(currentImageEntity, gt.Shape, imageViewerRef)
{

  animationDurationInSec = 0.1f

  override protected def newValue(interpolationPercentage: Float, initialValue: Shape) : Shape = {
    val scale = 1f + (interpolationPercentage * (scaleFactor - 1f))
    initialValue match {
      case qs: QuadShape => QuadShape(qs.size * scale)
      case _ => throw new Exception("[Zoomer] Only QuadShapes are supported.")
    }
  }
  override protected def removeFromLocalRep(e: Entity) = {}
}