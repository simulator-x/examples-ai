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

package simx.applications.examples.ai.atn

import simx.components.ai.atn.{Events, ImplicitConversions, Functions}
import simx.components.ai.atn.core._
import ImplicitConversions._
import Functions._
import simx.core.components.io.SpeechEvents
import simx.core.entity.typeconversion.ConvertibleTrait
import simx.core.ontology.types
import simx.core.worldinterface.eventhandling.{EventDescription, Event}

class SplitMergeAtn extends AugmentedTransitionNetwork   {

  val cursorMerge = false
  val inputTypes =  ExampleEvents.pointingGestureDetected :: SpeechEvents.token :: Nil
  val outputTypes = ExampleEvents.command :: Nil

  create StartState 'start withArc 'remove toTargetState 'split
  create Split      'split withCondition maxTimeDelta(1) andArc 'gesture toTargetState 'merge andArc 'speech toTargetState 'merge
  create Merge      'merge withOnMergeFunction onMerge withArc 'test toTargetState 'end
  create EndState   'end

  //Shorter variant for reoccurring function sequences
  val copyAndPrint =
  Seq[ArcFunction.FunctionType](
      copyEventDataToRegister(types.String, types.Direction),
      copyPreviousRegisterValues(types.String, types.Direction),
      printInfo
  )
  create Arc 'remove  withCondition checkToken("remove")                                              addFunctions copyAndPrint
  create Arc 'speech  withCondition checkToken("this")                                                addFunctions copyAndPrint
  create Arc 'gesture withCondition checkFor(ExampleEvents.pointingGestureDetected, types.Direction)  addFunctions copyAndPrint
  create Arc 'test    withCondition checkToken("ball")                                                addFunction executeCommand

  def checkToken(validTokens: String*)(in: Event, triggeredArc: ArcRep, current: StateRep, previous: StateRep, atn: ATNMachine): Condition.Result = {
    in.name match {
      case SpeechEvents.token.name =>
        val isValid = validTokens.contains(in.get(types.String).get.toLowerCase)
        ConditionResult(isValid)
      case _ => ConditionResult(doTransition = false)
    }
  }

  /**
   *  Check if the triggering event is of type eventType and if it contains a semantic value of type semanticType
   */
  def checkFor[T](eventType: EventDescription, semanticType: ConvertibleTrait[T])(in: Event, triggeredArc: ArcRep, current: StateRep, previous: StateRep, atn: ATNMachine): Condition.Result = {
    in.name match {
      case eventType.name =>
        val isValid = in.values.contains(semanticType)
        ConditionResult(isValid)
      case _ =>
        ConditionResult(doTransition = false)
    }
  }

  def onMerge(in: Event, current: StateRep, prev: StateRep): List[Event] = {
    println("[SplitMergeAtn] Executing onMerge function")
    prev.register.values.flatten.foreach(entry => current.register.add(entry))
     Nil
  }

  def executeCommand(in: Event, current: StateRep, prev: StateRep): List[Event] = {
    val tokens = prev.register.getAllValuesFor(types.String)
    val dir = prev.register.getFirstValueFor(types.Direction)
    var eventList: List[Event] = Nil
    if(tokens.contains("remove") && tokens.contains("this") && dir.isDefined){
      eventList ::= ExampleEvents.command(prev.register.firstSValFor(types.Direction), types.String(tokens.head))
      eventList ::= Events.reset.createEvent(types.Identifier(SplitMergeAtnExample.atnName))
    }
    eventList
  }
}
