package com.sai.pumpkin.domain

import scala.beans.BeanProperty

class DisplayArtifact {
  
   @BeanProperty
  var groupId: String = _

  @BeanProperty
  var artifactId: String = _

  @BeanProperty
  var version: String = _

  @BeanProperty
  var packaging: String = _

  @BeanProperty
  var classifier: String = _
  
  @BeanProperty
  var label: String = _ 
}