package com.sai.pumpkin.managedbeans

import java.io.Serializable
import java.util.ArrayList
import scala.beans.BeanProperty
import org.primefaces.context.RequestContext
import org.primefaces.event.SelectEvent
import org.primefaces.model.mindmap.DefaultMindmapNode
import org.primefaces.model.mindmap.MindmapNode
import org.springframework.web.context.support.WebApplicationContextUtils
import com.sai.pumpkin.domain.ArtifactDetail
import com.sai.pumpkin.domain.ArtifactDetailDataModel
import com.sai.pumpkin.service.ArtifactDetailService
import javax.faces.context.FacesContext
import javax.servlet.ServletContext
import scala.collection.JavaConversions._
import com.sai.pumpkin.service.ArtifactMetaService
import org.primefaces.model.DualListModel
import org.primefaces.event.TransferEvent
import com.sai.pumpkin.domain.ColumnModel
import com.sai.pumpkin.domain.ColumnModel
import org.primefaces.model.TreeNode
import org.primefaces.model.DefaultTreeNode

class DiffController extends Serializable {

  @BeanProperty
  var showTabs: Boolean = _

  @BeanProperty
  var projectArtifactIds: java.util.List[String] = new ArrayList

  @BeanProperty
  var selectedProject: String = _

  @BeanProperty
  var projectVersions: java.util.List[String] = new ArrayList

  @BeanProperty
  var selectedVersions: java.util.List[String] = new ArrayList

  @BeanProperty
  var pick: DualListModel[String] = _

  @BeanProperty
  var columns: java.util.List[ColumnModel] = new ArrayList[ColumnModel]

  @BeanProperty
  var root: TreeNode = _

  val servletContext = FacesContext.getCurrentInstance().getExternalContext().getContext().asInstanceOf[ServletContext]
  val appContext = WebApplicationContextUtils.getWebApplicationContext(servletContext)

  val artifactService = appContext.getBean(classOf[ArtifactDetailService])
  val artifactMetaService = appContext.getBean(classOf[ArtifactMetaService])

  val all = artifactMetaService.findAll.filter(_.artifactCatgory == "project")

  projectArtifactIds.addAll(all.map(_.artifactId))

  def onChange() = {
    println(selectedProject)
    projectVersions.clear()
    projectVersions.addAll(artifactService.findAll.filter(_.artifactId == selectedProject).map(_.version))
    if (selectedProject != null && !selectedProject.toLowerCase.contains("select")) {
      showTabs = true
      pick = new DualListModel(projectVersions, new ArrayList[String])
    } else {
      showTabs = false
      root = null
    }
  }

  def onVersionChange() = {
    println(selectedVersions)
  }

  def onTransfer(event: TransferEvent) = {
  }

  def doDiff() = {
    val projectVersions = pick.getTarget()
    val filteredArtifacts = artifactService.findAll.filter(artifact => artifact.artifactId == selectedProject && (projectVersions.contains(artifact.version)))
    println(" ==> "+ filteredArtifacts.mkString("\n"))

    val allArtifactIdUnion = filteredArtifacts.flatMap(detail => detail.children.map(_.artifactId)).distinct
    root = new DefaultTreeNode("Diff Results", null)

    root.setExpanded(true)

    allArtifactIdUnion.foreach(artifactId => {
      val artifactNode = new DefaultTreeNode(artifactId, root)
      artifactNode.setExpanded(true)
      filteredArtifacts.foreach(projectVersion => {
          val projectNode = new DefaultTreeNode(projectVersion.artifactId+"("+projectVersion.version+")", artifactNode)
          projectNode.setExpanded(true)
          val versionOfThisArtifact = projectVersion.children.filter(artifact => artifact.artifactId == artifactId)
          if(!versionOfThisArtifact.isEmpty) {
            val leaf = new DefaultTreeNode(versionOfThisArtifact.get(0).version, projectNode)
            leaf.setExpanded(true)
          } else {
            val leaf = new DefaultTreeNode("NOT AVAILABLE", projectNode)
            leaf.setExpanded(true)
          }
      })
    })

  }
}