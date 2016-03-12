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

import simx.components.sound.MaterialBasedSound
import simx.core.components.physics.PhysicsEvents
import simx.core.entity.component.EntityCreationHandling
import simx.core.entity.description.SValSet
import simx.core.ontology.types.SoundProperties
import simx.core.ontology.{types, EntityDescription}

/**
 * Created by hci-admin on 22/01/2016.
 */
object Sounds {
  def init()(implicit entityCreationContext : EntityCreationHandling){
    ballWoodCollision.realize()
    ballBallCollision.realize()
  }

  val ballWoodCollision = new EntityDescription(
    MaterialBasedSound(
      fileName = "assets/audio/ball-wood.wav",
      eventDesc = PhysicsEvents.collision,
      parameters = SValSet(types.Material("ball"), types.Material("wood")),
      soundprops = SoundProperties(gain = 1.0f)
    )
  )

  val ballBallCollision = new EntityDescription(
    MaterialBasedSound(
      fileName = "assets/audio/ball-ball.wav",
      eventDesc = PhysicsEvents.collision,
      parameters = SValSet(types.Material("ball"), types.Material("ball")),
      soundprops = SoundProperties(gain = 1.0f)
    )
  )
}