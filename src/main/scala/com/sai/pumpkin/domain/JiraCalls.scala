package com.sai.pumpkin.domain

import scala.beans.BeanProperty
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.ArrayList

@Document(collection = "jiraCalls")
class JiraCalls {

  @BeanProperty
  @Id
  var id: String = _

  @BeanProperty
  var groupId: String = _

  @BeanProperty
  var artifactId: String = _

  @BeanProperty
  var version: String = _

  @BeanProperty
  var classifier: String = _

  @BeanProperty
  var calls: java.util.List[String] = new ArrayList[String]()
}