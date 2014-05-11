package com.sai.pumpkin.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import scala.collection.JavaConversions._
import org.springframework.data.mongodb.core.MongoTemplate
import com.sai.pumpkin.domain.ArtifactDetail
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria

@Service
class ArtifactDetailService {

  @Autowired
  var mongoTemplate: MongoTemplate = _

  def findAll() = mongoTemplate.findAll(classOf[ArtifactDetail])

  def dependee(artifactDetail: ArtifactDetail) = {
    val query8 = new Query()
    query8.addCriteria(Criteria.where("children.artifactId").is(artifactDetail.artifactId).andOperator(Criteria.where("children.version").is(artifactDetail.version), Criteria.where("children.groupId").is(artifactDetail.groupId)))
    mongoTemplate.find(query8, classOf[ArtifactDetail])
  }
}