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
 * HCI Group at the University of Wuerzburg. The project is funded by the German
 * Federal Ministry of Education and Research (grant no. 17N4409).
 */

package simx.applications.examples.ai.mipro.supervisedLearning.imageviewer

import simx.core.ontology.Symbols
import simx.core.worldinterface.eventhandling.EventDescription

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 14/03/14
 * Time: 16:53
 */
object EventDescriptions {

  val next = new EventDescription( Symbols.next )
  val previous = new EventDescription( Symbols.previous )
  val fastForward = new EventDescription( Symbols.fastForward )
  val fastBackward = new EventDescription( Symbols.fastBackward )
  val select = new EventDescription( Symbols.select )
  val zoomIn = new EventDescription( Symbols.zoomIn )
  val zoomOut = new EventDescription( Symbols.zoomOut )
  val rotate = new EventDescription( Symbols.rotate )
}
