package com.sai.pumpkin.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import scala.collection.JavaConversions._
import org.springframework.data.mongodb.core.MongoTemplate
import com.sai.pumpkin.domain.ArtifactDetail
import com.sai.pumpkin.domain.ReleaseNotes

@Service
class ReleaseService {

  @Autowired
  var mongoTemplate: MongoTemplate = _

  def findAll() = mongoTemplate.findAll(classOf[ReleaseNotes])

}