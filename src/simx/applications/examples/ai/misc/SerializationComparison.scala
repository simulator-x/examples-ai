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

package simx.applications.examples.ai.misc

import java.io.File

import simx.components.ai.feature.recording.storage.{Persistence, PersistenceOptimized, Storage}
import simx.core.helper.{IO, Jfx}

/**
 * Created by martin 
 * on 10/09/15.
 */
object SerializationComparison {

    val write_test = false
    val read_test = false
    val convert_test = true

    val stop_watch = new StopWatch()
    Persistence.print_to_console = false
    PersistenceOptimized.print_to_console = false


  def main (args: Array[String]) {


    println(Console.BLUE + "[SerializationComparison] Start testing with write_test: "
                    +write_test+", read_test: "+read_test+", convert_test: "+convert_test + Console.RESET)

    if(write_test) {
      val inputFile = askForFile("Choose a standard file, to create a test storage, which can be written into xml files!")
      val testStorage = Persistence.fromXml(inputFile)
      val outputFile_standard = IO.changeExtension(inputFile, "output_standard.xml")
      val outputFile_optimized = IO.changeExtension(inputFile, "output_optimized.xml")
      println(Console.BLUE + "[SerializationComparison] Start write_test standard " + Console.RESET)
      writeStandardXML(testStorage, outputFile_standard)
      println(Console.BLUE + "[SerializationComparison] Start write_test optimized" + Console.RESET)
      writeOptimizedXML(testStorage, outputFile_optimized)
    }

    if(read_test) {
      println(Console.BLUE + "[SerializationComparison] Start read_test standard " + Console.RESET)
      val inputFile_standard = askForFile("Choose a standard file, to test reading it!")
      // readStandardXML(inputFile_standard, testCreatedStorage = true)  // for testing if deserializing has correct data
      readStandardXML(inputFile_standard)

      println(Console.BLUE + "[SerializationComparison] Start read_test optimized" + Console.RESET)
      val inputFile_optimized = askForFile("Choose a optimized file, to test reading it!")
      // readOptimizedXML(inputFile_optimized, testCreatedStorage = true) // for testing if deserializing has correct data
      readOptimizedXML(inputFile_optimized)
    }

    if(convert_test){
      println(Console.BLUE + "[SerializationComparison] Start convert_test from standard to optimized" + Console.RESET)
      stop_watch.start()
      val inputFile_standard = askForFile("Choose a standard file, to convert it to optimized file")
      val outpuFile_opimized = IO.changeExtension(inputFile_standard, "converted.xml")
      convertStandardInOptimized(inputFile_standard, outpuFile_opimized)
      println(Console.MAGENTA + "converted from standard to optimized file with: "+stop_watch.timeString +Console.RESET)
    }

    System.exit(0)
  }




  def askForFile(text:String) : File ={
    var inputFile : Option[File] = Jfx.askForFile(text, Some("xml"))
    inputFile match{
      case Some(file) => file
      case None => askForFile(text)
    }
  }


  def readStandardXML(file:File, testCreatedStorage : Boolean = false) : Storage = {
    stop_watch.start()
    val storage = Persistence.fromXml(file)
    println(Console.CYAN+ "[Standard]" +Console.RESET +Console.UNDERLINED + "[Finished] reading from File with: "+stop_watch.timeString+" " + Console.RESET )
    if(testCreatedStorage){testReading(storage, optimized =  false)}
    storage
  }

  def readOptimizedXML(file:File, testCreatedStorage : Boolean = false) : Storage ={
    stop_watch.start()
    val storage = PersistenceOptimized.fromXml(file)
    println(Console.MAGENTA+ "[Optimized]" +Console.RESET+ Console.UNDERLINED + "[Finished] reading from File with: "+stop_watch.timeString+" " + Console.RESET )
    if(testCreatedStorage){testReading(storage, optimized =  true)}
    storage
  }


  def writeStandardXML( storage:Storage, outputFile : File)={
    stop_watch.start()
    Persistence.saveToFile(storage, outputFile)
    println(Console.CYAN+ "[Standard]" +Console.RESET +Console.UNDERLINED + "[Finished] writing to File with: "+stop_watch.timeString+ Console.RESET )
  }

  def writeOptimizedXML(storage:Storage, outputFile:File)={
    stop_watch.start()
    PersistenceOptimized.saveToFile(storage, outputFile)
    println(Console.MAGENTA+ "[Optimized]" +Console.RESET+ Console.UNDERLINED + " [Finished] writing to File with: "+ stop_watch.timeString +" " + Console.RESET )
  }

  def convertStandardInOptimized(inputFile : File, outputFile: File)={
    val storage = readStandardXML(inputFile) // read inputfile
    writeOptimizedXML(storage, outputFile) // write a new file
  }


  private def testReading(storage: Storage, optimized: Boolean) = {
    storage.entities.foreach(head =>
      head.properties.foreach(prop => {
        prop.values.headOption.foreach(value => {
          if(optimized){
            println(Console.MAGENTA+ "[Optimized]" +Console.RESET + Console.MAGENTA + " Value is there: " + value._1+" / "+value._2+" "+Console.RESET)
          }else{
            println(Console.CYAN+ "[Standard]" +Console.RESET +Console.MAGENTA + " Value is there: " + value._1+" / "+value._2+" "+Console.RESET)
          }
        })
      }))
  }

}
class StopWatch() {

  private var start_time : Long = 0L
  private var stop_time : Long = 0L

  def start() : Unit = {
    start_time = 0L
    stop_time = 0L
    start_time = System.currentTimeMillis()
  }

  def stop() = stop_time = System.currentTimeMillis()

  def timeInMilliSeconds : Long = {
    if(stop_time != 0L){
      stop_time - start_time
    }else{
      stop()
      timeInMilliSeconds
    }
  }

  def timeInSeconds = timeInMilliSeconds / 1000

  def time = (timeInMilliSeconds, timeInSeconds)

  def timeString : String= {
    if(time._1 >= 1000L){
      time._2 + " s"
    } else{
      time._1 +" ms"
    }
  }

}

