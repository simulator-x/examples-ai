/*
 * Copyright 2013 The SIRIS Project
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

import simx.core.ontology.{Symbols, types}
import simx.components.ai.feature.{FeatureEventDescription, local}
import simx.core.worldinterface.eventhandling.EventDescription

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 9/12/13
 * Time: 11:39 AM
 */
trait ExamplesAIDefinitions {

  val shieldEvent = new EventDescription(Symbols.shield)
  val iceballEvent = new EventDescription(Symbols.iceball, types.Direction :: types.Position :: Nil)
  val fireballEvent = new EventDescription(Symbols.fireball, types.Direction :: types.Position :: Nil)

  val acceptedEvent = new EventDescription(local.Symbols.accepted, types.String :: Nil)
  val rejectedEvent = new EventDescription(local.Symbols.rejected, types.String :: Nil)

  val shieldGestureDesc = types.Boolean.withAnnotations(Symbols.shield)
  val shieldGestureEvent = FeatureEventDescription(shieldGestureDesc)

  val handsUpGestureDesc = types.Boolean.withAnnotations(Symbols.iceball)
  val handsUpGestureEvent = FeatureEventDescription(handsUpGestureDesc)

  val fireballGestureDesc = types.Boolean.withAnnotations(Symbols.fireball)
  val fireballGestureEvent = FeatureEventDescription(fireballGestureDesc)

  val pushGestureDesc = types.Transformation.withAnnotations(Symbols.left, Symbols.hand, Symbols.pointing)
  val pushGestureEvent = FeatureEventDescription(pushGestureDesc)

}
