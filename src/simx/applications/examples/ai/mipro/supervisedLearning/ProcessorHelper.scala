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

package simx.applications.examples.ai.mipro.supervisedLearning

import simplex3d.math.floatx.{ConstVec3f, Vec3f}
import simx.applications.examples.ai.ontology.functions.Interpolators
import simx.components.ai.mipro.{EntityCreationDSL, SemanticEntityReference, Processor}
import simx.core.ontology.types
import simx.core.ontology.types.{Acceleration, Velocity}
import simx.core.ontology.functions.DefaultInterpolators._


/**
 * ProcessorHelper provides functions for the processor classes for better reusability.
 * @author Mischa Rojkov, Claudia Mehn, Kristof Korwisi
 */
trait ProcessorHelper extends Processor with Interpolators with EntityCreationDSL {

  //ACCELERATION

  /**
   * Acceleration (values)
   * @param times         Time(steps) in ms
   * @param objectAccel   Object for history value extraction at given timesteps
   * @return              Values at given timesteps
   */
  def accelerationOverTimeValues(times: List[IdType], objectAccel: SemanticEntityReference) : List[ConstVec3f] = {
    val accSVals: List[Acceleration] = times.map{t =>  types.Acceleration of objectAccel at types.Milliseconds(t)}
    accSVals.map(_.value)
  }

  /**
   * Acceleration (mean)
   * @param times         Time(steps) in ms
   * @param objectAccel   Object for history value extraction at given timesteps
   * @return              Mean of values extracted at given timesteps
   */
  def accelerationOverTimeMean(times: List[IdType], objectAccel: SemanticEntityReference) : Vec3f = {
    val accValuesSum = accelerationOverTimeValues(times, objectAccel).foldLeft(Vec3f.Zero)(_+_)
    val res = accValuesSum / times.size.toFloat
    res
  }

  /**
   * Acceleration (delta)
   * @param times       Times (steps) in ms
   * @param objectAccel Object for history value extraction at given timesteps
   * @return            Difference between the consecutive vector positions (delta) at given timesteps
   */
  def accelerationOverTimeDelta(times: List[IdType], objectAccel: SemanticEntityReference) : List[Vec3f] = {
    vectorListBetweenConsecutiveAcceleration(times, objectAccel)
  }


  //CURVATURE

  /**
   * Curvature (Sum)
   * @param times     Times (steps) in ms
   * @param objectAny Object for history value extraction at given timesteps
   * @return          Sum of angle deltas between the consecutive positions at given timesteps
   */
  def curvatureOverTimeSum(times: List[IdType], objectAny: SemanticEntityReference) : Float = {
    val vectorList: List[simplex3d.math.double.Vec3] = vectorListBetweenConsecutivePositions(times, objectAny).
      map(v => simplex3d.math.double.Vec3(v)).
      filterNot(v => simplex3d.math.double.functions.length(v) < 0.000001).
      map(simplex3d.math.double.functions.normalize)
    //angles between consecutive vectors
    var angleSum : Double = 0
    import simplex3d.math.double.functions.dot
    for (i<-1 to vectorList.size-1) angleSum += Math.acos(Math.max(-1,Math.min(1,dot(vectorList(i), vectorList(i-1)))))
    angleSum.toFloat //return
  }


  //DIRECTION

  /**
   * Direction (delta)
   * @param times         Time(steps) in ms
   * @param objectRel     Object for history value extraction at given timesteps
   * @return              Difference between the consecutive vector positions (delta) at given timesteps
   */
  def directionOverTimeDelta(times: List[IdType], objectRel: SemanticEntityReference) : List[ConstVec3f] = {
    val vecs = vectorListBetweenConsecutivePositions(times, objectRel)
    vecs.map(vec => ConstVec3f(vec.x, vec.y, vec.z))
  }


  //DISTANCE

  /**
   * Distance between two objects
   * @param object1       Object 1
   * @param object2       Object 2
   * @return              Distance between two given objects
   */
  def distanceBetweenObjects(object1: SemanticEntityReference, object2: SemanticEntityReference) : List[Double] = {
    val o1 = (types.Position of object1).value
    val o2 = (types.Position of object2).value
    var retList: List[Double] = List()
    retList = retList .:: (distanceBetweenTwoPoints(o1, o2))
    retList
  }

  /**
   * Distance between two objects (delta)
   * @param times         Time(steps) in ms
   * @param object1       Object 1 for history value extraction at given timesteps
   * @param object2       Object 2 for history value extraction at given timesteps
   * @return              Difference between the consecutive distances (delta) at given timesteps
   */
  def distanceBetweenObjectsOverTimeDelta (times: List[IdType], object1: SemanticEntityReference, object2: SemanticEntityReference) : List[Double] = {
    val distSValsO1 = times.map{t => types.Position of object1 at types.Milliseconds(t)}
    val distSValsO2 = times.map{t => types.Position of object2 at types.Milliseconds(t)}
    val distValuesO1 = distSValsO1.map(_.value)
    val distValuesO2 = distSValsO2.map(_.value)
    var retList: List[Double] = List()
    for (i<-1 to distValuesO1.size-1) retList = retList .:: (distanceBetweenTwoPoints(distValuesO1(i), distValuesO2(i)) - distanceBetweenTwoPoints(distValuesO1(i-1), distValuesO2(i-1)))
    retList //return
  }


  //ELBOWANGLE

  def elbowAngleOverTimeDeltaMean(times: List[IdType], objectVelo: SemanticEntityReference) : Float = {
    val normalVectorValues = elbowAngleOverTimeDelta(times, objectVelo).sum.toFloat
    normalVectorValues / times.size.toFloat
  }

  /**
   * Elbow angle (delta)
   * @param times         Time(steps) in ms
   * @param elbowAngle    Object for history value extraction at given timesteps
   * @return              Difference between the consecutive angles (delta) at given timesteps
   */
  def elbowAngleOverTimeDelta(times: List[IdType], elbowAngle: SemanticEntityReference) : List[Double] = {
    val angleSVals = times.map{t => types.Angle of elbowAngle at types.Milliseconds(t)}
    val angleValues = angleSVals.map(_.value)
    var retList: List[Double] = List()
    for (i <- 1 to angleValues.size-1) retList = retList .:: (angleValues(i) - angleValues(i-1))
    retList //return
  }


  //NORMALVECTOR

  def normalVectorOverTimeDeltaMean(times: List[IdType], objectVelo: SemanticEntityReference) : Vec3f = {
    val normalVectorValues = normalVectorOverTimeDelta(times, objectVelo).foldLeft(Vec3f.Zero)(_+_)
    normalVectorValues / times.size.toFloat
  }

  /**
   * Normal vector (delta)
   * @param times         Time(steps) in ms
   * @param objectAny     Object 1 for history value extraction at given timesteps
   * @return              Difference between the normal vectors (delta) at given timesteps
   */
  def normalVectorOverTimeDelta(times: List[IdType], objectAny: SemanticEntityReference) : List[ConstVec3f] = {
    val vectorList = vectorListBetweenConsecutivePositions(times, objectAny)
    //cross product between consecutive vectors (normal vectors of their plane)
    var normalvectorList: List[ConstVec3f] = List()
    for (i<-1 to vectorList.size-1) normalvectorList = normalvectorList .:: (crossProduct(vectorList(i), vectorList(i-1)))
    normalvectorList //return
  }


  //ORIENTATION

  /**
   * Orientation
   * @param objectOrient  Object for orientation value extraction
   * @return              Extracted orientation values
   */
  def orientationOfObject(objectOrient: SemanticEntityReference) : List[Double] = {
    val objectOrientation = (types.Orientation of objectOrient).value
    List(objectOrientation.a, objectOrientation.b, objectOrientation.c, objectOrientation.d)
  }


  //VELOCITY

  /**
   * Velocity (values)
   * @param times         Time(steps) in ms
   * @param objectVelo    Object for history value extraction at given timesteps
   * @return              Values at given timesteps
   */
  def velocityOverTimeValues(times: List[IdType], objectVelo: SemanticEntityReference) : List[ConstVec3f] = {
    val veloSVals: List[Velocity] = times.map{t => types.Velocity of objectVelo at types.Milliseconds(t)}
    veloSVals.map(_.value)
  }

  /**
   * Velociy (mean)
   * @param times         Time(steps) in ms
   * @param objectVelo    Object for history value extraction at given timesteps
   * @return              Mean of values extracted at given timesteps
   */
  def velocityOverTimeMean(times: List[IdType], objectVelo: SemanticEntityReference) : Vec3f = {
    val veloValuesSum = velocityOverTimeValues(times, objectVelo).foldLeft(Vec3f.Zero)(_+_)
    veloValuesSum / times.size.toFloat
  }





  //##################################### HELPER METHODS #####################################

  /**
   * Distance between two points (delta)
   * @param point1        Point 1
   * @param point2        Point 2
   * @return              Distance between given points
   */
  private def distanceBetweenTwoPoints(point1: ConstVec3f, point2: ConstVec3f) = Math.sqrt(Math.pow(point1.x-point2.x,2) + Math.pow(point1.y-point2.y,2) + Math.pow(point1.z- point2.z,2))

  /**
   * dot product
   * @param vector1   Vector 1
   * @param vector2   Vector 2
   * @return          Dot product of given vectors
   */
  private def dotProduct(vector1: Vec3f, vector2: Vec3f) : Double = {
    val product = vector1.x * vector2.x + vector1.y * vector2.y + vector1.z * vector2.z
    product.toDouble // return
  }

  /**
   * Cross product
   * @param vec1          Vector 1
   * @param vec2          Vector 2
   * @return              Cross product of given vectors
   */
  private def crossProduct(vec1 : Vec3f, vec2: Vec3f) : Vec3f = {
    val x = vec1.y*vec2.z - vec1.z*vec2.y
    val y = vec1.z*vec2.x - vec1.x*vec2.z
    val z = vec1.x*vec2.y - vec1.y*vec2.x
    Vec3f(x,y,z) //return
  }

  /**
   * Vectors between consecutive positions
   * @param times     Time(steps) in ms
   * @param objectAny Object for history value extraction at given timesteps
   * @return          Vectors between the consecutive positions at given timesteps
   */
  private def vectorListBetweenConsecutivePositions(times: List[IdType], objectAny: SemanticEntityReference) : List[Vec3f] = {
    val posSVals = times.map{t => types.Position of objectAny at types.Milliseconds(t)}
    val posValues: List[ConstVec3f] = posSVals.map(_.value)
    //vectors between the consecutive positions
    vectorListBetweenConsecutiveVectors(posValues)
  }

  /**
   * Vectors between consecutive accelerations
   * @param times     Time(steps) in ms
   * @param objectAny Object for history value extraction at given timesteps
   * @return          Vectors between the consecutive accelerations at given timesteps
   */
  private def vectorListBetweenConsecutiveAcceleration(times: List[IdType], objectAny: SemanticEntityReference) : List[Vec3f] = {
    val accelerationSVals = times.map{t => types.Acceleration of objectAny at types.Milliseconds(t)}
    val accelerationValues: List[ConstVec3f] = accelerationSVals.map(_.value)
    //vectors between the consecutive acceleration vectors
    vectorListBetweenConsecutiveVectors(accelerationValues)
  }

  /**
   * Vectors between consecutive vectors
   * @param vecValues consecutive Vectors
   * @return          Vectors between the consecutive Vectors
   */
  private def vectorListBetweenConsecutiveVectors(vecValues: List[ConstVec3f]) : List[Vec3f] = {
    var vectorList: List[Vec3f] = List()
    for (i<-1 to vecValues.size-1) vectorList = vectorList .:: (vecValues(i) - vecValues(i-1))
    vectorList //return
  }
}
