package com.sai.pumpkin.domain

import scala.beans.BeanProperty
import org.springframework.data.mongodb.core.index._
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.CompoundIndex
import java.util.Date

@Document(collection= "consumer_detail")
class ConsumerDetail {

  @BeanProperty
  var artifactDetail: ArtifactDetail = _

  @BeanProperty
  var name: String = _

  @BeanProperty
  var fromDate: Date = _

  @BeanProperty
  var toDate: Date = _

  @BeanProperty
  var tags: String = _
  
  @BeanProperty
  var services: java.util.List[ArtifactDetail] = new java.util.ArrayList[ArtifactDetail]()

  
}