package com.sai.scratchpad.core

import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import com.sai.pumpkin.domain.ChangeSet 
import scala.collection.JavaConversions._
import org.springframework.data.mongodb.core.query.Update
import com.sai.pumpkin.domain.ChangeSet
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria

object Migration {

  def main(args: Array[String]): Unit = {

    lazy val applicationContext = new ClassPathXmlApplicationContext("appContext.xml")

    def getMongoTemplate = applicationContext.getBean(classOf[MongoTemplate])

    val allChangeSets = getMongoTemplate findAll (classOf[ChangeSet])

    val modified = allChangeSets.map(cs => {

      cs.artifactId = cs.artifact.artifactId
      cs.groupId = cs.artifact.groupId
      cs.version = cs.artifact.version
      cs.artifact = null;
      cs
    })

    modified.foreach(getMongoTemplate.save(_))

  }

}