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

import simx.core.components.physics.ImplicitEitherConversion._
import simx.core.components.renderer.createparameter.SpotLight
import java.awt.Color
import simx.core.ontology.EntityDescription
import simplex3d.math.floatx.{functions, Mat4x3f, ConstVec3f, ConstMat4f}
import simx.core.worldinterface.naming.NameIt


/**
 * A spot light with a given position and direction.
 *
 * Without a given rotation it points along the negative z axis.
 *
 * @author Dennis Wiebusch
 * @author Stephan Rehfeld
 *
 * @param name The name of the light.
 * @param pos The position of the light.
 * @param rot The rotation
 */
case class Light(name : String, pos : ConstVec3f, rot : ConstVec3f) extends EntityDescription(name,
    SpotLight(
      name           = name,
      transformation = ConstMat4f(Mat4x3f.rotateZ(functions.radians(rot.z)) rotateY functions.radians(rot.y) rotateX functions.radians(rot.x) translate pos),
      diffuseColor   = new Color(0.5f, 0.6f, 0.5f),
      specularColor  = new Color(0.5f, 0.6f, 0.5f),
      castShadow     = true
    ),
    NameIt(name)
  ) {
  require( name != null, "The parameter 'name' must not be null!" )
  require( pos != null, "The parameter 'pos' must not be null!" )
}