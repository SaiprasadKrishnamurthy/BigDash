package com.sai.scratchpad.core

import java.net.URL
import scala.collection.JavaConversions.asScalaBuffer
import scala.io.Source
import scala.xml.XML
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import com.sai.pumpkin.domain.ArtifactDetail
import com.sai.pumpkin.domain.ArtifactMetadata
import com.sai.pumpkin.domain.Distribution
import com.sai.pumpkin.domain.DistributionEntry

class DistributionLoader(applicationContext: ApplicationContext) {

  def run(): Unit = {
    println("----------------- Distribution Loader -------------------- ")
    val searchUrl = "http://10.70.101.186/nexus/service/local/data_index?g=distribution.%s&v=%s*"

    def getMongoTemplate = applicationContext.getBean(classOf[MongoTemplate])

    def getAllRegisteredArtifacts = getMongoTemplate.findAll(classOf[ArtifactMetadata])

    def process(artifact: ArtifactMetadata) = {

      val query5 = new Query
      query5.addCriteria(Criteria.where("groupId").is(artifact.groupId).and("artifactId").is(artifact.artifactId))
      val projectArtifactVersions = getMongoTemplate.find(query5, classOf[ArtifactDetail]).map(_.version)

      println("Project artifact versions: " + projectArtifactVersions)
      projectArtifactVersions.foreach(handleArtifact)

      def handleArtifact(version: String) = {
        val xml = XML.loadString(Source.fromURL(String.format(searchUrl, artifact.distributionKey, version)).mkString)
        println("Xml result: " + xml)
        val nodes = (xml \\ "artifact").foreach(artifactNode => {
          println(artifactNode)

          val tarFileLink = artifactNode \ "resourceURI" text

          println("Tarfile link: " + tarFileLink)
          val groupId = artifactNode \ "groupId" text
          val artifactId = artifactNode \ "artifactId" text
          val version = artifactNode \ "version" text
          val classifier = artifactNode \ "classifier" text
          
          println("\n\n\nExtracted attributes: "+(groupId+"|"+artifactId+"|"+version+"|"+classifier)+"\n\n\n")
          val query5 = new Query
          query5.addCriteria(Criteria.where("groupId").is(groupId).and("artifactId").is(artifactId).and("version").is(version).and("classifier").is(classifier))
          val existinDistribution = getMongoTemplate.find(query5, classOf[Distribution])

          if (existinDistribution.isEmpty()) {

            val in = new TarArchiveInputStream(new GzipCompressorInputStream(new URL(tarFileLink).openConnection().getInputStream()))
            println("Size: "+in.getBytesRead())
            val distribution = new Distribution
            distribution.groupId = groupId
            distribution.artifactId = artifactId
            distribution.version = version
            distribution.fileUrl = tarFileLink
            distribution.classifier = classifier
            distribution.masterGroupId = artifact.groupId
            distribution.masterArtifactId = artifact.artifactId
            distribution.masterVersion = version
            getMongoTemplate.save(distribution)
          } else {
            println("Distribution already mined..")
          }
        })
      }
    }
    getAllRegisteredArtifacts.filter(_.artifactCatgory == "project").foreach(process)
  }

}