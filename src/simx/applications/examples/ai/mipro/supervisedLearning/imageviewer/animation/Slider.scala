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

import simx.applications.examples.ai.mipro.supervisedLearning.imageviewer.Done
import simx.core.ontology.types
import simplex3d.math.float._
import simx.core.entity.Entity
import simx.core.svaractor.SVarActor

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 16/03/14
 * Time: 15:01
 */
class Slider(
  leftToRight: Boolean,
  screenSize: ConstVec2,
  simulationSpeed: Float,
  currentImageEntity: Entity,
  previousImageEntity: Option[Entity],
  imageViewer: SVarActor.Ref)
extends Animator(Long.MaxValue)
{
  val sign = if(leftToRight) 1f else -1f
  private var previousImagePos = 0f
  private var currentImagePos = -sign * screenSize.x
  protected def animate(deltaTinSec: Float) = {
    if(sign * currentImagePos < 0) {
      val step = deltaTinSec * simulationSpeed * 1.5f
      currentImagePos += step * sign
      previousImagePos += step * sign
      if(sign * currentImagePos > 0) currentImagePos = 0
      currentImageEntity.set(types.Position2D(ConstVec2(currentImagePos, 0)))
      previousImageEntity.collect{case pie => pie.set(types.Position2D(ConstVec2(previousImagePos, 0)))}
    } else {
      previousImageEntity.collect{case pie => pie.remove()}
      imageViewer ! Done()
      shutdown()
    }
  }
  override protected def removeFromLocalRep(e: Entity) = {}
}