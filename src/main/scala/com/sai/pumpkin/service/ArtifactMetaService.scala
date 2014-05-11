package com.sai.pumpkin.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import scala.collection.JavaConversions._
import org.springframework.data.mongodb.core.MongoTemplate
import com.sai.pumpkin.domain.ArtifactDetail
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria
import com.sai.pumpkin.domain.ArtifactMetadata

@Service
class ArtifactMetaService {

  @Autowired
  var mongoTemplate: MongoTemplate = _

  def findAll() = mongoTemplate.findAll(classOf[ArtifactMetadata])
}