/*
 * Copyright 2012 The SIRIS Project
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

package simx.applications.examples.ai.objects

import simx.core.components.renderer.createparameter.{ReadFromElseWhere, ShapeFromFile}
import simx.core.components.physics.ImplicitEitherConversion._
import simx.core.components.physics.PhysBox
import simx.core.ontology.EntityDescription
import simplex3d.math.floatx.{Mat4x3f, ConstMat4f, ConstVec3f, Vec3f}
import simx.core.worldinterface.naming.NameIt

/**
 * TODO: Document Dennis
 *
 * @author Dennis Wiebusch
 * @author Stephan Rehfeld
 *
 * @param name The name of the table.
 * @param size The size of the table. The standard size is a box with 1 as length of edges.
 * @param position The position of the box.
 */
case class Table(name : String, size : Vec3f, position : ConstVec3f) extends EntityDescription(name,
  PhysBox(
    halfExtends    = size*0.5f,
    transform      = position,
    restitution    = 0.998f,
    mass           = 0f
  ),
  ShapeFromFile(
    transformation = ReadFromElseWhere,
    scale          = ConstMat4f(Mat4x3f.translate(ConstVec3f(0,1,0)).scale(ConstVec3f(size.x/3f, size.y/2f, size.z))),
    file           = "assets/vis/table.dae"
  ),
  NameIt(name)
) {
  require( name != null, "The parameter 'name' must not be null!" )
  require( size != null, "The parameter 'position' must not be null!" )
  require( position != null, "The parameter 'position' must not be null!" )
}
