package com.sai.pumpkin.domain

import scala.beans.BeanProperty
import org.springframework.data.mongodb.core.index._
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.eclipse.aether.artifact.Artifact

@Document(collection = "artifact_detail_new")
class ArtifactDetail {

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
  var artifactType: String = _

  @BeanProperty
  var webserviceVersion: String = _

  @BeanProperty
  var nexusUrl: String = _

  @BeanProperty
  var yumRepoUrl: String = _

  @BeanProperty
  var contractZipFileLocation: String = _

  @BeanProperty
  var artifactReleaseType: String = _

  @BeanProperty
  var children: java.util.List[ArtifactDetail] = new java.util.ArrayList

  @BeanProperty
  var filePath: String = _

  @BeanProperty
  var svnRevision: String = _

  @BeanProperty
  var svnTag: String = _

  @BeanProperty
  var svnTrunk: String = _

  @BeanProperty
  var rpmLocation: String = _
  
  @BeanProperty
  var rpmName: String = _
  
  @BeanProperty
  var scope: String = _
  
  override def toString = groupId+"|"+artifactId+"|"+version+"|"+classifier

}