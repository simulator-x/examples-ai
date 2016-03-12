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

import simx.components.sound.SoundMaterial
import simx.core.components.renderer.createparameter.{ReadFromElseWhere, ShapeFromFile}
import simx.core.components.physics.ImplicitEitherConversion._
import simx.core.components.physics.PhysSphere
import simx.core.entity.description.SValSet
import simx.core.ontology.{Symbols, EntityDescription, types}
import simplex3d.math.float.{ConstVec3, Mat4x3, ConstMat4}

/**
 * An entity description for a jumping ball.
 *
 * @author Dennis Wiebusch
 * @author Stephan Rehfeld
 *
 * @param name The name of the ball.
 * @param radius The radius of the ball. Must be larger than 0.
 * @param position The inital position of the ball.
 */
case class Ball(name : String, radius : Float, position : ConstVec3) extends
  EntityDescription (
    aspects =
      PhysSphere(restitution = 0.998f, transform = position, radius = radius) ::
      ShapeFromFile(
        file = "assets/vis/ball.dae",
        scale = ConstMat4(Mat4x3.scale(radius*2f)),
        transformation = ReadFromElseWhere) ::
      SoundMaterial("ball") :: Nil,
    name = Symbol(name),
    additionalProperties = SValSet(types.EntityType(Symbols.iceball))
  )
{
  require( name != null, "The parameter 'name' must not be null!")
  require( radius > 0.0, "The parameter 'radius' must be larger than 0.0!")
  require( position != null, "The parameter 'position' must be larger than null!")
}
