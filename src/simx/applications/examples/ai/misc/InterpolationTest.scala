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

import simplex3d.math.float._
import simx.core.entity.description.SVal.SValType
import simx.core.entity.description.{SValHistory, SValSet}
import simx.core.ontology.types

/**
 * Created by martin 
 * on 03/06/15.
 */
object InterpolationTest {

  def main(args: Array[java.lang.String]): Unit = {


    val newer = ConstQuat4(functions.quaternion(math.Pi.toFloat, Vec3.UnitX))
    val older = ConstQuat4(functions.quaternion(math.Pi.toFloat, Vec3.UnitY))

    println(newer)
    println(older)

    println(functions.slerp(newer, older, 0f))
    println(functions.slerp(newer, older, 0.5f))
    println(functions.slerp(newer, older, 1f))

    import simx.core.ontology.functions.DefaultInterpolators._

    var f = types.Real(0f, 0L)
    f = f.withHistoryPrependedBy(1f, 10L)
    f = f.withHistoryPrependedBy(2f, 20L)
    f = f.withHistoryPrependedBy(3f, 30L)

    println(f)

    println(f.withHistory at 2L)
    println(f.withHistory at 16L)

    println()

    val storage = SValSet()

    storage.add(types.Real(0f, 0L))
    println(storage)

    storage.getFirstSValFor(types.Real) match {
      case Some(sVal: SValHistory[_, _, _]) =>
        storage.replaceAllWith(sVal.withHistoryPrependedBy(1f, 10L))
      case _ =>
        println("Nope")
    }

    println(storage)


    val test: Option[SValType[Float]] = storage.getFirstSValFor(types.Real)
    println("getFirstSValFor", storage.getFirstSValFor(types.Real))



  }

}
