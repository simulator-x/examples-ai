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

package simx.applications.examples.ai.misc

import java.io.{File, PrintWriter}

import akka.actor._
import simx.core.helper.IO
import simx.core.svaractor.SVarActor

import scala.concurrent.duration._

object ActorTest {
  def main(args: Array[String]): Unit = {
    //SVarActor.createActor(Props.apply(new TestActor()), Some("TestActor"))
    SVarActor.createActor(new ListTestSVarActor)
    //ActorSystem().actorOf(Props.apply(new TestActor()), "TestActor")
  }
}

case class DoIt()

class ListTestSVarActor(targetedDeltaInMSec: Long = 5L) extends SVarActor {
  var last = 0L
  var deltas = List[Long]()

  /**
   * called when the actor is started
   */
  override protected def startUp(): Unit = {
    addJobIn(targetedDeltaInMSec){doStuff()}
  }

  def doStuff(): Unit = {
    val now = System.currentTimeMillis()
    deltas ::= now - last
    last = now
    if(deltas.size == 10000) {
      deltas = deltas .dropRight(200)
      val diffs = deltas.map{d => d.toFloat - targetedDeltaInMSec.toFloat}
      val logFile = IO.dateTimeFileFrom(new File("simx-actor-test.csv"), append = false)
      new PrintWriter(logFile) {
        write(diffs.mkString("\n").replace('.',','))
        close()
      }
      println("Saved log to " + logFile.getAbsolutePath)
//      println("Mean: " + (diffs.sum / diffs.size.toFloat))
//      println("Max : " + diffs.max)
      deltas = Nil
    }
    addJobIn(targetedDeltaInMSec){doStuff()}
  }
}



class TestSVarActor extends SVarActor {
  var last = 0L

  /**
   * called when the actor is started
   */
  override protected def startUp(): Unit = {
    addJobIn(1L){doStuff()}
  }

  def doStuff(): Unit = {
    val now = System.currentTimeMillis()
    println(now - last)
    last = now
    addJobIn(1L){doStuff()}
  }
}

class TestActor() extends Actor {
  import context._
  private var last = 0L

  final def receive: Receive = {
    case ReceiveTimeout =>
      val now = System.currentTimeMillis()
      println(now - last)
      last = now
      context.system.scheduler.scheduleOnce(1L milliseconds, self, ReceiveTimeout)
//akka.actor.LightArrayRevolverScheduler
//      Thread.sleep(1)
//      self ! ReceiveTimeout
//      Thread.sleep(1)
  }

  override def preStart() {
    super.preStart()
    //context.setReceiveTimeout(10L milliseconds)
    self ! ReceiveTimeout
  }

}
