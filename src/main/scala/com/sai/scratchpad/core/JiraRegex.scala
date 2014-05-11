package com.sai.scratchpad.core

object JiraRegex {

  def main(args: Array[String]): Unit = {
    val pattern = "[A-Z]*-[0-9]*.*?".r
    val str = " OMAN-1234,GSLSEC-1:djsljk GSLUTIL-12344 -- d thr kldsklj sdljaslj"

    println(pattern.findAllMatchIn(str).filter(_.toString.split("-").length > 1).mkString("\n"))

  }

}