package com.sai.scratchpad.core

import java.util.Date

import scala.beans.BeanProperty

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OrchestratorJob {

  @Autowired
  @BeanProperty
  var applicationContext: ApplicationContext = _

  // hourly wait.
  @Scheduled(fixedDelay = 3600000)
  def run() = {
    val start = System.currentTimeMillis()
    println("Job started at: " + new Date)
    println("\n\n\n\n\n\n\n\n\nSAI\n\n\n\n\n\n\n\n")

    new DataLoaderJob(applicationContext).run()
    println("\n\n\n\n\n\n\n\n\nSAI DL FINISHED\n\n\n\n\n\n\n\n")

    new SvnLogLoader(applicationContext).run()
    println("\n\n\n\n\n\n\n\n\nSAI SVN FINISHED\n\n\n\n\n\n\n\n")

    new DistributionLoader(applicationContext).run()
    println("\n\n\n\n\n\n\n\n\nSAI DIST LOADER FINISHED\n\n\n\n\n\n\n\n")

    new JiraCallsLoader(applicationContext).run()
    println("\n\n\n\n\n\n\n\n\nSAI JIRA CALLS LOADER FINISHED\n\n\n\n\n\n\n\n")

    try {
      new WebserviceInterfaceLoader(applicationContext).run()
      println("\n\n\n\n\n\n\n\n\nSAI WS FINISHED\n\n\n\n\n\n\n\n")
    } catch {
      case ex: Throwable => ex.printStackTrace()
    }

    val end = System.currentTimeMillis()
    println("\n\n\n\n\n\n\n\n\nALL LOADERS FINISHED TIME TAKEN: " + ((end - start) / 1000) + " seconds \n\n\n\n\n\n\n\n")

  }
}