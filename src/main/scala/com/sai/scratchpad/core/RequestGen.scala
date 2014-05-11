package com.sai.scratchpad.core

import com.predic8.wsdl.WSDLParser
import scala.collection.JavaConversions._
import java.io.StringWriter
import com.sai.pumpkin.domain.SampleSOAPRequest
import com.predic8.wstool.creator.SOARequestCreator
import groovy.xml.MarkupBuilder
import com.predic8.wstool.creator.RequestTemplateCreator

object RequestGen {

  def main(args: Array[String]): Unit = {
    val parser = new WSDLParser()
    val wsdl = parser.parse("C:\\pumpkin\\person-ws-presentation20.7\\PersonManagementService.wsdl")

    wsdl.getOperations().foreach(operation => {
      val writer = new StringWriter
      val sampleReq = new SampleSOAPRequest
      sampleReq.operationName = operation.getName()

      val creator = new SOARequestCreator(wsdl, new RequestTemplateCreator(), new MarkupBuilder(writer));

      //creator.createRequest(PortType name, Operation name, Binding name);
      creator.createRequest(wsdl.getPortTypes().get(0).getName(), sampleReq.operationName, wsdl.getBindings().get(0).getName)

      sampleReq.contents = writer.toString()

      println(sampleReq.contents)
    })
  }

}