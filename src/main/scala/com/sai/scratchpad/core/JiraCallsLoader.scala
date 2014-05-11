package com.sai.scratchpad.core

import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import com.sai.pumpkin.domain.JiraCalls
import com.sai.pumpkin.domain.ArtifactDetail
import scala.collection.JavaConversions._
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria
import com.sai.pumpkin.domain.ChangeSet
import com.sai.pumpkin.domain.JiraCalls
import com.sai.pumpkin.domain.ChangeSet
import com.sai.pumpkin.domain.JiraCalls
import org.springframework.data.mongodb.core.query.Update

class JiraCallsLoader(applicationContext: ApplicationContext) {

  // Jira tickets regex.
  val pattern = "[A-Z]*-[0-9]*.*?".r

  def run(): Unit = {
    println("----------------- Jira calls Loader -------------------- ")

    def getMongoTemplate = applicationContext.getBean(classOf[MongoTemplate])

    def getAllLoadedArtifacts = getMongoTemplate.findAll(classOf[ArtifactDetail])

    def parseCallNumbersFromString(message: String) = {
      pattern.findAllMatchIn(message).filter(_.toString.split("-").length > 1).map(_.toString).toList.filter(_.split("-").length > 1)
    }

    def saveJiraCall(changeSet: ChangeSet) = {
      val query1 = new Query
      query1.addCriteria(Criteria.where("groupId").is(changeSet.groupId).and("artifactId").is(changeSet.artifactId).and("version").is(changeSet.version))
      val existingCalls = getMongoTemplate.find(query1, classOf[JiraCalls])
      if (existingCalls.isEmpty) {
        val jiraCall = new JiraCalls
        jiraCall.groupId = changeSet.groupId
        jiraCall.artifactId = changeSet.artifactId
        jiraCall.version = changeSet.version
        jiraCall.calls.addAll(parseCallNumbersFromString(changeSet.commitMessage))
        getMongoTemplate.save(jiraCall)
      } else {
        val jiraCall = existingCalls.get(0)
        jiraCall.calls.addAll(parseCallNumbersFromString(changeSet.commitMessage))

        val query = new Query();
        query.addCriteria(Criteria.where("id").is(jiraCall.id));

        val update = new Update();
        update.set("id", jiraCall.id)
        update.set("groupId", jiraCall.groupId)
        update.set("artifactId", jiraCall.artifactId)
        update.set("version", jiraCall.version)
        update.set("calls", jiraCall.calls)

        getMongoTemplate.updateMulti(query, update, classOf[JiraCalls])
      }

    }
    getAllLoadedArtifacts.foreach(artifactDetail => {

      val query1 = new Query
      query1.addCriteria(Criteria.where("groupId").is(artifactDetail.groupId).and("artifactId").is(artifactDetail.artifactId).and("version").is(artifactDetail.version))
      val existingCalls = getMongoTemplate.find(query1, classOf[JiraCalls])

      if (existingCalls.isEmpty()) {
        val query5 = new Query
        query5.addCriteria(Criteria.where("groupId").is(artifactDetail.groupId).and("artifactId").is(artifactDetail.artifactId).and("version").is(artifactDetail.version))
        val existingChangesets = getMongoTemplate.find(query5, classOf[ChangeSet])

        existingChangesets.foreach(saveJiraCall)

      } else {
        println("Jira calls already mined for: " + artifactDetail + ". Nothing to do.")
      }

    })
  }
}