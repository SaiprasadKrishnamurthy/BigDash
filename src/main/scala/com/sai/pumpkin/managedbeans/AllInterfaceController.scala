package com.sai.pumpkin.managedbeans

import java.io.Serializable
import java.util.ArrayList
import scala.beans.BeanProperty
import org.primefaces.context.RequestContext
import org.primefaces.event.SelectEvent
import org.primefaces.model.mindmap.DefaultMindmapNode
import org.primefaces.model.mindmap.MindmapNode
import org.springframework.web.context.support.WebApplicationContextUtils
import com.sai.pumpkin.domain.ArtifactDetail
import com.sai.pumpkin.domain.ArtifactDetailDataModel
import com.sai.pumpkin.service.ArtifactDetailService
import javax.faces.context.FacesContext
import javax.servlet.ServletContext
import scala.collection.JavaConversions._
import com.sai.pumpkin.domain.ChangeSet
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria
import org.primefaces.model.chart.PieChartModel
import com.sai.pumpkin.domain.WebserviceInterface
import java.io.File
import scala.xml.XML
import org.apache.commons.io.FileUtils
import com.sai.pumpkin.domain.SampleSOAPRequest
import com.predic8.wsdl.WSDLParser
import com.predic8.wstool.creator.SOARequestCreator
import groovy.xml.MarkupBuilder
import com.predic8.wstool.creator.RequestTemplateCreator
import java.io.StringWriter

class AllInterfaceController extends Serializable {

  @BeanProperty
  var selectedWs: String = _

  @BeanProperty
  var showTabs: Boolean = _

  @BeanProperty
  var all: java.util.List[String] = new ArrayList

  @BeanProperty
  var sampleRequests: java.util.List[SampleSOAPRequest] = new ArrayList

  @BeanProperty
  var selectedInterface: WebserviceInterface = _

  val servletContext = FacesContext.getCurrentInstance().getExternalContext().getContext().asInstanceOf[ServletContext]
  val appContext = WebApplicationContextUtils.getWebApplicationContext(servletContext)

  val service = appContext.getBean(classOf[ArtifactDetailService])

  val mongoTemplate = appContext.getBean(classOf[MongoTemplate])

  all.clear

  val allArtifacts = service.findAll.filter(_.artifactType == "webservice")
  all = allArtifacts.map(artifact => artifact.artifactId + "(" + artifact.version + ")")

  def handleChange() = {
    showTabs = false

    sampleRequests.clear()
    println(selectedWs + " ================ ")
    val artifactId = selectedWs.substring(0, selectedWs.indexOf("("))
    val version = selectedWs.substring(selectedWs.indexOf("(") + 1).replace(")", "")

    println("ArtifactId: " + artifactId)
    println("VErsion: " + version)

    val selectedArtifact = allArtifacts.filter(artifact => artifact.artifactId == artifactId && artifact.version == version).get(0)

    val query5 = new Query()
    query5.addCriteria(Criteria.where("artifactId").is(selectedArtifact.artifactId).andOperator(Criteria.where("groupId").is(selectedArtifact.groupId), Criteria.where("version").is(selectedArtifact.version)))

    selectedInterface = mongoTemplate.find(query5, classOf[WebserviceInterface]).get(0)

    showTabs = true

    /*try {
    val interfacesDir = System.getProperty("user.dir") + File.separator + selectedInterface.artifactId + selectedInterface.version
    var wsdlName = ""

    selectedInterface.xmlInterfaces.foreach(xmlInterface => {
      val name = xmlInterface.name.substring(xmlInterface.name.lastIndexOf("/") + 1)
      println("\t" + name)
      if(name.toLowerCase.endsWith("wsdl")) wsdlName = name
      val contents = xmlInterface.contents
      var canonicalizedContents = contents.trim()
      val xmlContents = XML.loadString(contents)
      val schemaLocationAttributes = xmlContents \\ "@schemaLocation"
      val allImports = schemaLocationAttributes.map(_.text)
      val fileNames = allImports.foreach(schemaLocation => {
        val fileName = if (schemaLocation.contains("/")) schemaLocation.substring(schemaLocation.lastIndexOf("/") + 1) else schemaLocation
        println(" --------- " + fileName)
        canonicalizedContents = canonicalizedContents.replace(schemaLocation, fileName)
      })

      FileUtils.writeStringToFile(new File(interfacesDir + File.separator + name), canonicalizedContents.replaceAll("[^\\x00-\\x7F]", ""))
    })

    val parser = new WSDLParser()
    val wsdl = parser.parse(interfacesDir + File.separator + wsdlName)

    wsdl.getOperations().foreach(operation => {
      val writer = new StringWriter
      val sampleReq = new SampleSOAPRequest
      sampleReq.operationName = operation.getName()

      val creator = new SOARequestCreator(wsdl, new RequestTemplateCreator(), new MarkupBuilder(writer));

      //creator.createRequest(PortType name, Operation name, Binding name);
      creator.createRequest(wsdl.getPortTypes().get(0).getName(), sampleReq.operationName, wsdl.getBindings().get(0).getName)
      
      sampleReq.contents = writer.toString()
      
      sampleRequests.add(sampleReq)
    })
    }catch {
      case ex: Throwable => ex.printStackTrace();println(ex)
    }*/
  }
}