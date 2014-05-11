//package com.sai.scratchpad.core
//
//import org.springframework.context.support.ClassPathXmlApplicationContext
//
//object Client {
//
//  def main(args: Array[String]): Unit = {
//      val ctx = new ClassPathXmlApplicationContext("appContext.xml")
//      val m = ctx.getBean(classOf[MvnArtifactHandler])
//      println(m)
//      m.process
//  }
//}