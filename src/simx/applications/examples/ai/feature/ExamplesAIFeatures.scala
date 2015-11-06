/*
 * Copyright 2013 The SIRIS Project
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

package simx.applications.examples.ai.feature

import simx.components.ai.feature.{InputFeature, Feature}
import simx.core.ontology.{types, Symbols}
import simplex3d.math.float._
import simplex3d.math.floatx.functions._
import simx.core.entity.component.EntityCreationHandling
import simx.components.vrpn.devices.SimpleTarget
import simx.components.ai.feature.collection.BufferedSValSet
import simx.components.ai.feature.ImplicitEitherConversion._

/**
 * User: martin
 * Date: 9/11/13
 */
trait ExamplesAIFeatures extends EntityCreationHandling with ExamplesAIDefinitions {

  def realizeSCFeatures() {
    new InputFeature(types.Transformation, Symbols.torso) {
      def relativeTo = None

      def source =
        //SimpleTarget("iotracker@132.187.8.144", Symbol("2")).desc  //IOServer
      SimpleTarget("Tracker0@132.187.8.160", Symbol("2")).desc //Kinect, Anke's notebook
    }.realize()

    /**
     * Right Hand
     */

    new InputFeature(types.Transformation, Symbols.hand, Symbols.right) {
      def relativeTo = Some(Seq(Symbols.torso))

      def source =
        //SimpleTarget("iotracker@132.187.8.144", Symbol("12")).desc  //IOServer
      SimpleTarget("Tracker0@132.187.8.160", Symbol("14")).desc //Kinect, Anke's notebook
    }.realize()



    new Feature(types.Velocity.withAnnotations(Symbols.hand, Symbols.right)) {

      val rhPos = types.Position.withAnnotations(Symbols.hand, Symbols.right)

      def requirements = rhPos :: Nil

      def production(in: BufferedSValSet) = {
        val timeWindow = 60
        ((in(rhPos) at timeWindow) - (in(rhPos) at 0)) / (timeWindow.toFloat/1000f)
      }
    }.realize()

    new Feature(types.Acceleration.withAnnotations(Symbols.hand, Symbols.right)) {

      val rhVelocity = types.Velocity.withAnnotations(Symbols.hand, Symbols.right)

      def requirements = rhVelocity :: Nil

      def production(in: BufferedSValSet) = {
        val timeWindow = 60
        ((in(rhVelocity) at timeWindow) - (in(rhVelocity) at 0)) / (timeWindow.toFloat/1000f)
      }
    }.realize()

    /**
     * Left Hand
     */

    new InputFeature(types.Transformation, Symbols.hand, Symbols.left) {
      def relativeTo = Some(Seq(Symbols.torso))

      def source =
        //SimpleTarget("iotracker@132.187.8.144", Symbol("8")).desc  //IOServer
      SimpleTarget("Tracker0@132.187.8.160", Symbol("8")).desc //
    }.realize()

    new Feature(types.Velocity.withAnnotations(Symbols.hand, Symbols.left)) {

      val lhPos = types.Position.withAnnotations(Symbols.hand, Symbols.left)

      def requirements = lhPos :: Nil

      def production(in: BufferedSValSet) = {
        val timeWindow = 60
        ((in(lhPos) at timeWindow) - (in(lhPos) at 0)) / (timeWindow.toFloat/1000f)
      }
    }.realize()

    new Feature(types.Acceleration.withAnnotations(Symbols.hand, Symbols.left)) {

      val lhVelocity = types.Velocity.withAnnotations(Symbols.hand, Symbols.left)

      def requirements = lhVelocity :: Nil

      def production(in: BufferedSValSet) = {
        val timeWindow = 60
        ((in(lhVelocity) at timeWindow) - (in(lhVelocity) at 0)) / (timeWindow.toFloat/1000f)
      }
    }.realize()

    /**
     * Combinations
     */

    new Feature(shieldGestureDesc) {

      val accThreshold = 15f
      val handDist = 0.5f

      val lhPos = types.Position.withAnnotations(Symbols.hand, Symbols.left, Symbols.relative)
      val rhPos = types.Position.withAnnotations(Symbols.hand, Symbols.right, Symbols.relative)
      val lhAcc = types.Acceleration.withAnnotations(Symbols.hand, Symbols.left)
      val rhAcc = types.Acceleration.withAnnotations(Symbols.hand, Symbols.right)

      def requirements = lhAcc :: rhAcc :: rhPos :: lhPos :: Nil

      def production(in: BufferedSValSet) =
        (length(in(lhAcc) at 0) > accThreshold) && (length(in(rhAcc) at 0) > accThreshold) &&
          ((in(rhPos) at 0).x > handDist) && ((in(lhPos) at 0).x < -handDist)

    }.realize()

    new Feature(handsUpGestureDesc) {

      val lhPos = types.Position.withAnnotations(Symbols.hand, Symbols.left, Symbols.relative)
      val rhPos = types.Position.withAnnotations(Symbols.hand, Symbols.right, Symbols.relative)

      def requirements = rhPos :: lhPos :: Nil

      def production(in: BufferedSValSet) =
        ((in(rhPos) at 0).y > 0.7f) && ((in(lhPos) at 0).y > 0.7f)

    }.realize()

    new Feature(fireballGestureDesc) {

      val maxArmDistance = 0.20f
      val maxArmHeight = -0.1f

      val lhPos = types.Position.withAnnotations(Symbols.hand, Symbols.left, Symbols.relative)
      val rhPos = types.Position.withAnnotations(Symbols.hand, Symbols.right, Symbols.relative)

      def requirements = rhPos :: lhPos :: Nil

      def production(in: BufferedSValSet) =
        (length((in(rhPos) at 0) - (in(lhPos) at 0)) < maxArmDistance) &&
          ((in(rhPos) at 0).y < maxArmHeight) && ((in(lhPos) at 0).y < maxArmHeight)

    }.realize()

    new Feature(pushGestureDesc) {

      val torsoToLeftShoulderOffset = ConstVec3(-0.2f, 0.35f, 0f)
      val accThreshold = 20f
      val armLength = 0.3f

      val lhPos = types.Position.withAnnotations(Symbols.hand, Symbols.left, Symbols.relative)
      val lhPosAbs = types.Position.withAnnotations(Symbols.hand, Symbols.left)
      val lhAcc = types.Acceleration.withAnnotations(Symbols.hand, Symbols.left)
      val torsoOri = types.Orientation.withAnnotations(Symbols.torso)

      def requirements = lhPosAbs :: lhPos :: lhAcc :: torsoOri :: Nil

      def production(in: BufferedSValSet) = {
        val timeStepSize = 10
        val lookBack = 200
        val maximumAtLeastBefore = 100

        def isValid(t: Long) =
          (length(in(lhAcc) at t) > accThreshold) && ((in(lhPos) at t).z < -armLength)

        val potentialMaxima =
          (for(i <- 0 to lookBack by timeStepSize) yield if(isValid(i)) Some(i, length(in(lhAcc) at i)) else None).
            filter(_.isDefined).map(_.get)

        var maximum: Option[(Int, Float)] = None

        potentialMaxima.foreach(pm => {
          if(pm._2 > maximum.fold(Float.MinValue)(_._2)) maximum = Some(pm)
        })

        maximum match {
          case Some(m) if m._1 >= maximumAtLeastBefore =>
            ConstMat4(
              Vec4(inverse(rotationMat(in(torsoOri) at m._1)) * ConstVec3((in(lhPos) at m._1) - torsoToLeftShoulderOffset), 0),
              Vec4(in(lhPosAbs) at m._1, 1),
              Vec4.Zero,
              Vec4.Zero
            )
          case _ =>
            Mat4.Zero
        }
      }

    }.realize()
  }

}
