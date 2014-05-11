package com.sai.scratchpad.core

import collection.JavaConverters._
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.apache.commons.io.IOUtils
import java.util.zip.ZipInputStream
import java.io.InputStream
import scala.xml.XML
import org.apache.commons.io.FileUtils
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.Seq
import java.io.File
import scala.beans.BeanProperty

class WarParser {

  @BeanProperty
  var wsArtifactId = ""

  @BeanProperty
  var wsArtifactVersion = ""
  @BeanProperty
  var wsArtifact = ""

  @BeanProperty
  var wsCanonicalModelWorkingDir = System.getProperty("user.dir")

  // assumes one wsdl per webservice module.
  def parse(): Unit = {

    val rootZip = new ZipFile(wsArtifact)
    println(wsArtifact)
    println(rootZip.entries.asScala.filter(entry => entry.getName().endsWith(".wsdl")).mkString("\n"))
    val wsdlStringContent = rootZip.entries.asScala.filter(entry => entry.getName().endsWith(".wsdl")).map(e => IOUtils.toString(rootZip.getInputStream(e))).mkString("")
    println(wsdlStringContent)
    val wsdlXml = XML.loadString(wsdlStringContent)
    println(wsdlXml)
    val schemaLocationAttributes = wsdlXml \\ "@schemaLocation"
    val schemaLocations = schemaLocationAttributes.map(node => node.text)
    val schemaArtifactJars = rootZip.entries.asScala.filter(entry => entry.getName().endsWith(".jar")).map(toZipInputStream(rootZip, _))
    val allSchemasMaps = schemaArtifactJars.map(collectSchemas(_))
    val allSchemas = allSchemasMaps.reduce(_ ++ _)
    println(allSchemas.keys.mkString(""))

    val resolvedSchemas = resolveSchemas(schemaLocations, allSchemas)
    println(s"Resolved schemas:$resolvedSchemas")

    // write all the schemas.
    allSchemas.filter(tuple => resolvedSchemas.contains(tuple._1)).foreach(tuple => writeToFile(tuple._1, tuple._2))

    // write the wsdl.
    writeToFile(wsArtifactId + ".wsdl", wsdlStringContent)
  }

  def resolveSchemas(schemaLocationAttributes: Seq[String], allSchemas: scala.collection.mutable.Map[String, String]): scala.collection.mutable.Set[String] = {
    val resolved = scala.collection.mutable.Set[String]()
    resolved.++=(schemaLocationAttributes)

    // recursive function.

    def resolve(locations: Seq[String]): Unit = {
      locations.foreach(schemaLocation => {
        val schemaContent = allSchemas.get(schemaLocation).get
        val schemaLocationAttributes = XML.loadString(schemaContent) \\ "@schemaLocation"
        val schemaLocations = schemaLocationAttributes.map(node => node.text)
        resolved.++=(schemaLocations)
        resolve(schemaLocations)
      })
    }
    resolve(schemaLocationAttributes)
    resolved
  }

  def writeToFile(filePath: String, fileContents: String) = {
    val fileDir = if (filePath.contains("/")) filePath.substring(0, filePath.lastIndexOf("/")) else ""
    val fileName = filePath.substring(filePath.lastIndexOf("/") + 1)
    val schemaDir = new File(wsCanonicalModelWorkingDir + File.separator + wsArtifactId + wsArtifactVersion + File.separator + fileDir)
    FileUtils.writeStringToFile(new File(schemaDir + File.separator + fileName), fileContents)
  }

  def toZipInputStream(parentZip: ZipFile, entry: ZipEntry) = {
    new ZipInputStream(parentZip.getInputStream(entry))
  }

  def collectSchemas(zipInputStream: ZipInputStream) = {
    var schemasMap = scala.collection.mutable.Map[String, String]()
    var entry = zipInputStream.getNextEntry
    while (entry != null) {
      if (entry.getName.endsWith(".xsd")) {
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
}