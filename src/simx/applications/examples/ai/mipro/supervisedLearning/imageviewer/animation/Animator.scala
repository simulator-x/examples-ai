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

package simx.applications.examples.ai.mipro.supervisedLearning.imageviewer.animation

import simx.core.svaractor.SVarActor
import simx.core.svaractor.unifiedaccess.EntityUpdateHandling

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 16/03/14
 * Time: 12:41
 */
abstract class Animator(durationInMillis: Long, simulationFrequencyInHz: Float = 60f) extends SVarActor with EntityUpdateHandling {

  private val framePeriod: Long = ((1f/simulationFrequencyInHz)*1000f).toLong
  private var startTime = 0L
  private var last = 0L

  /**
   * called when the actor is started
   */
  override protected def startUp() = {
    setup()
    startTime = System.currentTimeMillis()
    simulate()
  }

  private def simulate() {
    val now = System.currentTimeMillis()
    if(last != 0L) {
      val deltaTinSec = (now - last).toFloat / 1000f
      animate(deltaTinSec)
    }
    last = now
    if((now - startTime) > durationInMillis) shutdown()
    else addJobIn(framePeriod){simulate()}
  }

  /**
   * Called one at startup.
   */
  protected def setup() = {}

  /**
   * Called once every 1/simulationFrequencyInHz seconds.
   * @param deltaTinSec Time passed since last call in seconds.
   */
  protected def animate(deltaTinSec: Float)
}