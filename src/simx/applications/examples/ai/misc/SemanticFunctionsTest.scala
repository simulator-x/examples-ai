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

package simx.applications.examples.ai.misc

import simplex3d.math.float._
import simx.core.ontology.types._

object SemanticFunctionsTest {

  def main(args: Array[java.lang.String]) {
    println("Starting miPro test")

    implicit object Functions
      extends simx.core.ontology.functions.DefaultSemanticFunctions
      with simx.applications.examples.ai.ontology.functions.DefaultImplementations
    {
      override def +(param1: Length, param2: Length): Length =
        Length(math.floor(param1.value + param1.value).toFloat)
    }

    val result: Length =
      (Length of Vector3(Vec3.One)) + (Length of Vector3(Vec3.One))
    println(result)

    val result2 =
      Time(300) equals Time(400)
    println(result2)

    val result3 =
      Time(300) approximates Time(400)
    println(result3)

    val result4 =
      Angle between(Vector3(Vec3.UnitX), Vector3(Vec3.UnitY))
    println(result4 + " = " + simplex3d.math.float.functions.degrees(result4.value) + " deg")
  }
}
