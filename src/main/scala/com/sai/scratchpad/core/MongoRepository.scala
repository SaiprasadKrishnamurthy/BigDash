package com.sai.scratchpad.core

import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria
import com.sai.pumpkin.domain.ArtifactDetail
import scala.collection.JavaConversions._

object MongoRepository {

  def main(args: Array[String]) = {
    val ctx = new ClassPathXmlApplicationContext("appContext.xml")
    val mongoTemplate = ctx.getBean(classOf[MongoTemplate])

    val query5 = new Query()
    query5.addCriteria(Criteria.where("artifactId").regex("-ws"))
    val webserviceArtifacts = mongoTemplate.find(query5, classOf[ArtifactDetail])
    
    println(webserviceArtifacts.map(a => s"${a.groupId} | ${a.artifactId} | ${a.version} | ${a.classifier}").mkString("\n"))
    
    
    
  }

}