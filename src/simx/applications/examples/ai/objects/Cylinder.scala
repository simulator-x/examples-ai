/*
 * Copyright 2016 The SIRIS Project
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

import simplex3d.math.float._
import simx.core.components.renderer.createparameter.ShapeFromFile
import simx.core.entity.description.SValSet
import simx.core.ontology.EntityDescription

/**
 * Created by martin 
 * on 21/08/15.
 */
case class Cylinder(pos: ConstVec3, name: String = "Cylinder", additionalProperties: SValSet = SValSet()) extends
  EntityDescription(
    aspects = ShapeFromFile(
      file = "assets/vis/blue-1x0.05x0.05-positiveZAxis-cylinder.dae",
      transformation = Right(ConstMat4(Mat4x3.translate(pos))),
      scale = ConstMat4(Mat4x3.scale(Vec3(0.25f,0.25f,3)))
    ) :: Nil,
    name = Symbol(name),
    additionalProperties = additionalProperties
  )