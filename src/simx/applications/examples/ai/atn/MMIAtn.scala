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

package simx.applications.examples.ai.atn

import simx.components.ai.atn.{Functions, ImplicitConversions, Events}
import simx.components.ai.atn.core._
import simx.core.components.io.SpeechEvents
import simx.core.ontology.types
import simx.core.worldinterface.eventhandling.{Event, EventDescription}
import ImplicitConversions._
import Functions._

/**
 * Created by martin 
 * on 06/07/15.
 */
class MMIAtn extends AugmentedTransitionNetwork {
  override val cursorMerge: Boolean = true
  override val outputTypes: List[EventDescription] = Commands.createSphere :: Nil
  override val inputTypes: List[EventDescription] = SpeechEvents.token :: Nil

  val copyAndPrint =
    Seq[ArcFunction.FunctionType](
      copyEventDataToRegister(types.String),
      copyPreviousRegisterValues(types.String),
      printInfo
    )


  //Topology
  create StartState 'start        withArc 'verb toTargetState 'awaitingDet
  create State      'awaitingDet  withArc 'det      toTargetState 'awaitingNoun
  create State      'awaitingNoun withArc 'noun toTargetState 'end
  create State      'end

  create Arc 'verb withCondition  checkToken("create") addFunction addAndCopyRegister
  create Arc 'det withCondition   checkToken("a") addFunction addAndCopyRegister
  create Arc 'noun withCondition  checkToken("ball") addFunction (addAndCopyRegister, complete)


  def checkToken(validTokens: String*)(in: Event): Condition.Result = {
    in.name match {
      case SpeechEvents.token.name =>
        val token = in.get(types.String).get
        var isValid = validTokens.contains(token.toLowerCase)
        if(token != "a"){
          isValid = isValid && in.get(types.Real).get > 0.3
        }
        ConditionResult(isValid)
      case _ => ConditionResult(doTransition = false)
    }
  }

  def complete(in: Event, curReg: StateRep, prevReg: StateRep) = {
    println("Command completed successfully")
    Events.reset(types.Identifier(SimpleAtnExample.atnName)) :: Commands.createSphere() :: Nil
  }

  def addAndCopyRegister(in: Event, triggeredArc: ArcRep, curReg: StateRep, prevReg: StateRep, atn: ATNMachine) = {
    copyRegister(in, triggeredArc, curReg, prevReg, atn)
    copyEventDataToRegister(types.String)(in, triggeredArc, curReg, prevReg, atn)
    println("Current Register at " + curReg.id + " containing " + curReg.register)
    Nil
  }
}