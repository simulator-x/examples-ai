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
import simx.core.entity.Entity
import simx.core.ontology.SValDescription
import simx.core.svaractor.SVarActor
import simx.core.svaractor.semantictrait.base.{Thing, Base}

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 16/03/14
 * Time: 14:53
 */
abstract class LinearInterpolator[T, B](
  entity: Entity,
  animatedProperty: SValDescription[T, B, _ <: Base, _ <: Thing],
  imageViewerRef: SVarActor.Ref)
extends Animator(Long.MaxValue)
{
  private var initialValue: Option[T] = None
  private var timePassed = 0f
  protected var animationDurationInSec = 1f
  override protected def setup() = {
    entity.get(animatedProperty).head(
      (value) => initialValue = Some(value)
    )
  }
  override protected def animate(deltaTinSec: Float) = {
    initialValue.collect{case iValue =>
      if(timePassed < animationDurationInSec) {
        timePassed += deltaTinSec
        if(timePassed > animationDurationInSec) timePassed = animationDurationInSec
        val interpolationPercentage = timePassed/animationDurationInSec
        entity.set(animatedProperty(newValue(interpolationPercentage, iValue)))
      } else {
        imageViewerRef ! Done()
        shutdown()
      }
    }
  }

  protected def newValue(interpolationPercentage: Float, initialValue: T): T
}
