package com.sai.scratchpad.core

import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import com.sai.pumpkin.domain.ArtifactDetail
import scala.collection.JavaConversions._
import collection.JavaConverters._
import com.sai.pumpkin.domain.ArtifactMetadata
import java.io.File
import org.apache.commons.io.FileUtils
import java.util.zip.ZipFile
import org.apache.commons.io.IOUtils
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import scala.xml.XML
import java.util.ArrayList
import com.sai.pumpkin.domain.WebserviceInterface
import com.sai.pumpkin.domain.ChangeSet
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria
import com.sai.pumpkin.domain.XmlInterface
import javax.servlet.ServletContext
import org.springframework.web.context.support.WebApplicationContextUtils
import javax.faces.context.FacesContext
import org.springframework.context.ApplicationContext

class WebserviceInterfaceLoader(applicationContext: ApplicationContext) {

  val schemaExclusions = List("spring", "activemq", "string", "jasper")
  val topLevelSchemaPattern = "WSDLTypes"

  def run() {

    def getMongoTemplate = applicationContext.getBean(classOf[MongoTemplate])

    val allMetaForInterfaceAnalysis = getMongoTemplate.findAll(classOf[ArtifactMetadata]).filter(_.interfaceAnalysis)

    val allArtifacts = getMongoTemplate.findAll(classOf[ArtifactDetail])

    def isNotProcessed(artifact: ArtifactDetail) = {
      val query5 = new Query
      query5.addCriteria(Criteria.where("groupId").is(artifact.groupId).and("artifactId").is(artifact.artifactId).and("version").is(artifact.version))
      getMongoTemplate.find(query5, classOf[WebserviceInterface]).isEmpty
    }

    val allArtifactsForInterfaceAnalysis = allArtifacts.filter(artifact => isNotProcessed(artifact) && allMetaForInterfaceAnalysis.filter(meta => meta.groupId == artifact.groupId && meta.artifactId == artifact.artifactId).size > 0)
    def getWsdlStringContent(artifact: ArtifactDetail) = new ZipFile(artifact.filePath).entries.asScala.filter(entry => entry.getName().endsWith(".wsdl")).map(e => IOUtils.toString(new ZipFile(artifact.filePath).getInputStream(e))).mkString("")

    def getWsdlName(artifact: ArtifactDetail) = new ZipFile(artifact.filePath).entries.asScala.filter(entry => entry.getName().endsWith(".wsdl"))

    val fileAndDirectory = allArtifactsForInterfaceAnalysis.map(artifact => (new ZipFile(artifact.filePath), new File(artifact.filePath).getParent(), artifact))

    fileAndDirectory.foreach(tuple => {
      val query5 = new Query
      query5.addCriteria(Criteria.where("groupId").is(tuple._3.groupId).and("artifactId").is(tuple._3.artifactId).and("version").is(tuple._3.version))

      if (getMongoTemplate.find(query5, classOf[WebserviceInterface]).isEmpty) {
        try {
          println("---- Start ---------" + tuple._2)
          val topLevelSchemaName = allMetaForInterfaceAnalysis.filter(meta => tuple._3.groupId == meta.groupId && tuple._3.artifactId == meta.artifactId).get(0).topLevelXmlSchema

          val allJars = tuple._1.entries.filter(entry => entry.getName().endsWith(".jar"))

          val zipInputStream = allJars.map(toZipInputStream(tuple._1, _))

          val allSchemas = zipInputStream.flatMap(collectSchemas(_)).toList

          val resolvedSchemas = resolveSchemas(allSchemas, topLevelSchemaName)

          val distinctResolved = resolvedSchemas.distinct

          println(distinctResolved.map(_._1).mkString("\n"))

          val interface = new WebserviceInterface
          interface.groupId = tuple._3.groupId
          interface.artifactId = tuple._3.artifactId
          interface.version = tuple._3.version

          val wsdlContent = getWsdlStringContent(tuple._3)
          val wsdlName = getWsdlName(tuple._3).map(e => e.getName())

          distinctResolved.foreach(kv => {
            val xmlSchema = new XmlInterface
            xmlSchema.name = kv._1
            xmlSchema.contents = kv._2
            interface.xmlInterfaces.add(xmlSchema)
          })

          val xmlSchema = new XmlInterface
          xmlSchema.name = wsdlName.mkString("")
          xmlSchema.contents = wsdlContent
          interface.xmlInterfaces.add(xmlSchema)

          getMongoTemplate.save(interface)

          println("---- Finished ---------" + tuple._2)
          println("\n\n")
        } catch {
          case ex: Throwable => println("Could not resolve: " + ex); ex.printStackTrace()
        }
      } else {
        println("Interface already mined for: " + tuple._3)
      }
    })

    def resolveSchemas(allSchemas: List[(String, String)], topLevelSchemaName: String) = {
      val topLevelXsds = allSchemas.filter(tuple => tuple._1.equals(topLevelSchemaName))

      var collected = new ArrayList[(String, String)]()

      def resolveImports(currentTuple: (String, String)): Unit = {
        val xmlContents = XML.loadString(normalize(currentTuple._2.trim))
        collected.add(currentTuple)
        val schemaLocationAttributes = xmlContents \\ "@schemaLocation"
        val schemaLocations = schemaLocationAttributes.map(node => node.text)

        schemaLocations.foreach(location => {
          var fileName = location
          if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1)
          }

          val imported = allSchemas.filter(tuple => tuple._1.endsWith(fileName))
          if (!imported.isEmpty) {
            resolveImports(imported.get(0))
          }
        })
      }
      topLevelXsds.foreach(resolveImports _)
      collected
    }

    def normalize(xml: String) = {
      xml.substring(xml.indexOf("<"))
    }

    def collectSchemas(zipInputStream: ZipInputStream) = {
      var schemasMap = scala.collection.mutable.Map[String, String]()
      var entry = zipInputStream.getNextEntry
      while (entry != null) {
        if (entry.getName.endsWith(".xsd") && schemaExclusions.filter(exclusion => entry.getName.contains(exclusion)).size == 0) {
          val content = new StringBuilder
          val buffer = new Array[Byte](1024)
          var len = zipInputStream.read(buffer)
          while (len > 0) {
            content.append(new String(buffer, 0, len))
            schemasMap.put(entry.getName(), content.toString)
            len = zipInputStream.read(buffer)
          }
        }
        entry = zipInputStream.getNextEntry
      }
      schemasMap
    }

    def toZipInputStream(parentZip: ZipFile, entry: ZipEntry) = {
      new ZipInputStream(parentZip.getInputStream(entry))
    }

  }

}