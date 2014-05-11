package com.sai.pumpkin.domain

import scala.beans.BeanProperty
import java.util.Date

class ReleaseNotes {
  
  @BeanProperty
  var projectArtifact: ArtifactDetail = _
  
  @BeanProperty
  var date: Date = _
  
  @BeanProperty
  var notes: String = _
  
  @BeanProperty
  var components: java.util.List[ArtifactDetail] = new java.util.ArrayList[ArtifactDetail]
  
  @BeanProperty
  var releaseName: String = _
  
  @BeanProperty
  var releaseLead: String = _
  
}