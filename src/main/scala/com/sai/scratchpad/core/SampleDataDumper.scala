package com.sai.scratchpad.core

import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import com.sai.pumpkin.domain.ArtifactDetail

object SampleDataDumper {

  def main(args: Array[String]): Unit = {

    val ctx = new ClassPathXmlApplicationContext("appContext.xml")

    val mongoTemplate = ctx.getBean(classOf[MongoTemplate])

//    val project = new ArtifactDetail
//
//    project.groupId = "aero.sita.gsl.project"
//    project.artifactId = "oman-base"
//    project.version = "21.2"
//
//    project.packaging = "pom"
//
//    project.artifactReleaseType = "release"
//    project.nexusUrl = "http://nexus/"
//    project.yumRepoUrl = "http:/yum"
//
//    project.artifactType = "project"
//    project.children = List()

    // children 1
    val profilerws = new ArtifactDetail
    profilerws.groupId = "aero.sita.gsl.profiler"
    profilerws.artifactId = "profiler-ws-presentation"
    profilerws.version = "21.2"
    profilerws.packaging = "war"
    profilerws.classifier = "V3"
    profilerws.artifactReleaseType = "release"
    profilerws.nexusUrl = "http://nexus/"
    profilerws.yumRepoUrl = "http:/yum"
    profilerws.contractZipFileLocation = "c:/contracts/profilerws.zip"

    profilerws.artifactType = "webservice"

    // children 2
    val eventws = new ArtifactDetail
    eventws.groupId = "aero.sita.gsl.eventws"
    eventws.artifactId = "event-ws-presentation"
    eventws.version = "21.1"
    eventws.packaging = "war"
    eventws.classifier = "V3"
    eventws.artifactReleaseType = "release"
    eventws.nexusUrl = "http://nexus/"
    eventws.yumRepoUrl = "http:/yum"
    eventws.contractZipFileLocation = "c:/contracts/eventws.zip"

    eventws.artifactType = "webservice"

//    project.children = project.children ++ List(profilerws, eventws)
    
    mongoTemplate.save(profilerws)
    mongoTemplate.save(eventws)
    
    

  }

}