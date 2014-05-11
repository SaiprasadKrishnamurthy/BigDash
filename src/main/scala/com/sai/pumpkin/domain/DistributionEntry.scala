package com.sai.pumpkin.domain

import scala.beans.BeanProperty

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "distributionEntry")
class DistributionEntry {

  @BeanProperty
  @Id
  var id: String = _

  @BeanProperty
  var rpmName: String = _

  @BeanProperty
  var groupId: String = _

  @BeanProperty
  var artifactId: String = _

  @BeanProperty
  var version: String = _

}