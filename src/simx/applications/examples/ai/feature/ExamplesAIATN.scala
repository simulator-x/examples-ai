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

import simx.core.ontology.types
import simx.core.components.io.SpeechEvents
import simx.core.worldinterface.eventhandling.Event
import simx.core.entity.description.SValSet
import simx.core.entity.typeconversion.ConvertibleTrait
import simplex3d.math.float._
import scala.reflect.ClassTag
import scala.reflect.classTag
import simx.components.ai.atn.core._
import simx.components.ai.feature.FeatureEventDescription
import simx.components.ai.atn.Events

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 9/11/13
 * Time: 6:44 PM
 */
trait ExamplesAIATN extends AugmentedTransitionNetwork with ExamplesAIDefinitions {

  val atnName = 'atn
  val cursorMerge = true
  val inputTypes =  fireballGestureEvent :: shieldGestureEvent :: handsUpGestureEvent :: pushGestureEvent ::
    SpeechEvents.token :: Nil
  val outputTypes = acceptedEvent :: rejectedEvent :: shieldEvent :: iceballEvent :: fireballEvent :: Nil

  val maxTimeDiffInMillis = 2000

  //Topology
  create StartState 'start withArc 'shieldG    toTargetState 's    withArc 'handsUpG toTargetState 'u  withArc 'fireG toTargetState 'f
  create State      's     withArc 'createT    toTargetState 'sc
  create State      'sc    withArc 'shieldT    toTargetState 'end

  create State      'u     withArc 'handsT     toTargetState 'uh   withArc 'pushG    toTargetState 'up
  create State      'uh    withArc 'ofT        toTargetState 'uho
  create State      'uho   withArc 'timeT      toTargetState 'uhot
  create State      'uhot  withArc 'pushG      toTargetState 'end
  create State      'up    withArc 'handsT     toTargetState 'uph
  create State      'uph   withArc 'ofT        toTargetState 'upho
  create State      'upho  withArc 'timeT      toTargetState 'end

  create State      'f     withArc 'summonT    toTargetState 'fs   withArc 'pushG2   toTargetState 'fp
  create State      'fs    withArc 'purgatoryT toTargetState 'fsp
  create State      'fsp   withArc 'pushG2     toTargetState 'end
  create State      'fp    withArc 'summonT    toTargetState 'fps
  create State      'fps   withArc 'purgatoryT toTargetState 'end

  create Arc 'shieldG    withCondition checkGesture(shieldGestureDesc)                     addFunction updateShieldStep()
  create Arc 'createT    withCondition checkToken("create" :: "creates" :: "great" :: Nil) addFunction updateShieldStep()
  create Arc 'shieldT    withCondition checkToken("shield" :: "field" :: Nil)              addFunction updateShieldStep()

  create Arc 'handsUpG   withCondition checkGesture(handsUpGestureDesc)                    addFunction updateIceballStep()
  create Arc 'handsT     withCondition checkToken("hands" :: "hand" :: "and" :: "end" :: Nil, 2800L) addFunction updateIceballStep()
  create Arc 'ofT        withCondition checkToken("of" :: "off" :: Nil)                    addFunction updateIceballStep()
  create Arc 'timeT      withCondition checkToken("time" :: Nil)                           addFunction updateIceballStep()
  create Arc 'pushG      withCondition checkGesture(pushGestureDesc, 2800L)                addFunction updateIceballStep()

  create Arc 'fireG      withCondition checkGesture(fireballGestureDesc)                   addFunction updateFireballStep()
  create Arc 'summonT    withCondition checkToken("summon" :: "someone" :: Nil, 4000L)     addFunction updateFireballStep()
  create Arc 'purgatoryT withCondition checkToken("purgatory" :: "predatory" :: Nil)       addFunction updateFireballStep()
  create Arc 'pushG2     withCondition checkGesture(pushGestureDesc, 4000L)                addFunction updateFireballStep()

  //Functions
  def checkGesture[T: ClassTag](gestureDesc: ConvertibleTrait[T],  _maxTimeDiffInMillis: Long = maxTimeDiffInMillis)
                               (in: Event, triggeredArc: ArcRep, curState: StateRep, prevState: StateRep, atn: ATNMachine) =
  {
    if(classTag[T] == classTag[Boolean])
      _checkGesture(gestureDesc, _maxTimeDiffInMillis, (v: T) => v == true)(in, triggeredArc, curState, prevState, atn)
    else if(classTag[T] == classTag[ConstVec3])
      _checkGesture(gestureDesc, _maxTimeDiffInMillis, (v: T) => {v != Vec3.Zero})(in, triggeredArc, curState, prevState, atn)
    else if(classTag[T] == classTag[ConstMat4])
      _checkGesture(gestureDesc, _maxTimeDiffInMillis, (v: T) => {v != Mat4.Zero})(in, triggeredArc, curState, prevState, atn)
    else (false, Nil, KeepCursor())
  }

  def _checkGesture[T](gestureDesc: ConvertibleTrait[T], _maxTimeDiffInMillis: Long, validate: T => Boolean)
                      (in: Event, triggeredArc: ArcRep, curState: StateRep, prevState: StateRep, atn: ATNMachine) = {
    val gestureEvent = FeatureEventDescription(gestureDesc)
    in.name match {
      case gestureEvent.name =>
        var resultEvents = List[Event]()
        val timestamp = in.get(types.Time).get
        val isGesture = validate(in.get(gestureDesc).get)
        val meetsTimeReq = prevState.register.getFirstSValFor(types.Time).
          map(prevTimestamp => (timestamp - prevTimestamp.value) < _maxTimeDiffInMillis).getOrElse(true)
        if(meetsTimeReq && isGesture) resultEvents ::= acceptedEvent(types.String("[" + gestureEvent.name.value.toString.replaceAll(""".*\[""", "").replaceAll("""\].*""", "") + "]"))
        //else resultEvents ::= rejectedEvent(types.String("[" + gestureEvent.name.value.toString.replaceAll(""".*\[""", "").replaceAll("""\].*""", "") + "]"))
        (meetsTimeReq && isGesture, resultEvents, KeepCursor())
      case _ => (false, Nil, KeepCursor())
    }
  }

  def checkToken(validTokens: List[String], _maxTimeDiffInMillis: Long = maxTimeDiffInMillis)
                (in: Event, triggeredArc: ArcRep, curState: StateRep, prevState: StateRep, atn: ATNMachine) = {
    in.name match {
      case SpeechEvents.token.name =>
        var resultEvents = List[Event]()
        val timestamp = System.currentTimeMillis
        val isValid = validTokens.contains(in.get(types.String).get.toLowerCase)
        val meetsTimeReq = prevState.register.getFirstSValFor(types.Time).
          map(prevTimestamp => (timestamp - prevTimestamp.value) < _maxTimeDiffInMillis).getOrElse(true)
        if(meetsTimeReq && isValid) resultEvents ::= acceptedEvent(types.String(validTokens(0)))
        else resultEvents ::= rejectedEvent(types.String(in.get(types.String).get.toLowerCase ))
        (meetsTimeReq && isValid, resultEvents, KeepCursor())
      case _ => (false, Nil, KeepCursor())
    }
  }

  def updateShieldStep()(in: Event, triggeredArc: ArcRep, curState: StateRep, prevState: StateRep, atn: ATNMachine) =
    step("create shield", 3, () => {shieldEvent.createEvent()})(in, triggeredArc, curState, prevState, atn)

  def updateFireballStep()(in: Event, triggeredArc: ArcRep, curState: StateRep, prevState: StateRep, atn: ATNMachine) = {
    in.values.getFirstSValFor(pushGestureDesc).collect{case pushDir => curState.register.replaceAllWith(pushDir)}
    prevState.register.getFirstSValFor(pushGestureDesc).collect{case pushDir => curState.register.replaceAllWith(pushDir)}
    step("summon purgatory", 4, () => {
      fireballEvent.createEvent(
        types.Direction(curState.register.firstValueFor(pushGestureDesc)(0).xyz),
        types.Position(curState.register.firstValueFor(pushGestureDesc)(1).xyz)
      )
    })(in, triggeredArc, curState, prevState, atn)
  }

  def updateIceballStep()(in: Event, triggeredArc: ArcRep, curState: StateRep, prevState: StateRep, atn: ATNMachine) = {
    in.values.getFirstSValFor(pushGestureDesc).collect{case pushDir => curState.register.replaceAllWith(pushDir)}
    prevState.register.getFirstSValFor(pushGestureDesc).collect{case pushDir => curState.register.replaceAllWith(pushDir)}
    step("hands of time", 5, () => {
      iceballEvent.createEvent(
        types.Direction(curState.register.firstValueFor(pushGestureDesc)(0).xyz),
        types.Position(curState.register.firstValueFor(pushGestureDesc)(1).xyz)
      )
    })(in, triggeredArc, curState, prevState, atn)
  }

  def step(cmd: String, nrOfSteps: Int, produceEvent: () => Event)
          (in: Event, triggeredArc: ArcRep, curState: StateRep, prevState: StateRep, atn: ATNMachine) = {
    curState.register.replaceAllWith(types.Time(System.currentTimeMillis()))
    curState.register.replaceAllWith(types.Integer(prevState.register.getFirstValueFor(types.Integer).getOrElse(0)+1))
    if(curState.register.firstValueFor(types.Integer) == nrOfSteps) {
      println("################[info][SixtonsCurseATN] Command verified: '" + cmd + "'")
      produceEvent() :: Events.reset.createEvent(types.Identifier(atnName)) :: Nil
    } else Nil
  }
}
