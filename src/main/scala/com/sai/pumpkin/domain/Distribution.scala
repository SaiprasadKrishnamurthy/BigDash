package com.sai.pumpkin.domain

import scala.beans.BeanProperty
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.ArrayList

@Document(collection = "distribution")
class Distribution {

  @BeanProperty
  @Id
  var id: String = _

  @BeanProperty
  var entries: java.util.ArrayList[DistributionEntry] = new ArrayList

  @BeanProperty
  var groupId: String = _

  @BeanProperty
  var artifactId: String = _

  @BeanProperty
  var version: String = _

  @BeanProperty
  var classifier: String = _

  @BeanProperty
  var fileUrl: String = _

  @BeanProperty
  var masterGroupId: String = _

  @BeanProperty
  var masterArtifactId: String = _

  @BeanProperty
  var masterVersion: String = _

}