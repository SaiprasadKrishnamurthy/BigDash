package com.sai.pumpkin.managedbeans

import scala.beans.BeanProperty
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.domain.Sort
import com.sai.pumpkin.domain.ArtifactDetail
import scala.collection.JavaConversions._
import java.io.Serializable
import org.primefaces.model.TreeNode
import org.primefaces.model.DefaultTreeNode
import org.primefaces.event.NodeSelectEvent
import org.primefaces.model.mindmap.MindmapNode
import org.primefaces.model.mindmap.DefaultMindmapNode
import org.primefaces.context.RequestContext
import com.sai.pumpkin.domain.ConsumerDetail
import scala.collection.immutable.TreeMap
import java.util.Date
import java.text.SimpleDateFormat
import javax.faces.context.FacesContext
import javax.faces.application.FacesMessage
import org.springframework.data.mongodb.core.query.Update
import com.sai.pumpkin.domain.ConsumerDetailDataModel
import com.sai.pumpkin.domain.ConsumerDetailDataModel
import org.primefaces.event.SelectEvent
import com.sai.pumpkin.domain.ArtifactDetailDataModel
import com.sai.pumpkin.domain.ArtifactDetailDataModel
import javax.servlet.ServletContext
import org.springframework.web.context.support.WebApplicationContextUtils
import com.sai.pumpkin.domain.ReleaseNotes
import org.primefaces.model.tagcloud.TagCloudModel
import org.primefaces.model.tagcloud.DefaultTagCloudModel
import com.sai.pumpkin.domain.ChangeSet
import org.primefaces.model.tagcloud.DefaultTagCloudItem

class AllProjectsController extends Serializable {

  @BeanProperty
  var projectNames: java.util.List[String] = new java.util.ArrayList[String]()

  @BeanProperty
  var selectedProj: String = _

  @BeanProperty
  var showTabs: Boolean = _

  @BeanProperty
  var root: TreeNode = _

  @BeanProperty
  var selectedNode: TreeNode = _

  @BeanProperty
  var mavenVersionSelected: Boolean = _

  @BeanProperty
  var serviceVersion: String = _

  @BeanProperty
  var mavenVersion: String = _

  @BeanProperty
  var rootMaven: MindmapNode = _

  @BeanProperty
  var projects: java.util.List[String] = new java.util.ArrayList[String]()

  @BeanProperty
  var allProjects: java.util.List[ArtifactDetail] = new java.util.ArrayList[ArtifactDetail]()

  @BeanProperty
  var allFilteredMavenIdentifiers: java.util.List[ArtifactDetail] = new java.util.ArrayList[ArtifactDetail]()

  @BeanProperty
  var consumerArtifactIdentifier: String = _

  @BeanProperty
  var consumerGroupId: String = _

  @BeanProperty
  var consumerArtifactId: String = _

  @BeanProperty
  var consumerVersion: String = _

  @BeanProperty
  var consumerClassifier: String = _

  @BeanProperty
  var consumerStartDate: Date = _

  @BeanProperty
  var consumerEndDate: Date = _

  @BeanProperty
  var consumerTag: String = _

  @BeanProperty
  var consumerName: String = _

  @BeanProperty
  var projectDataModel: ArtifactDetailDataModel = _

  @BeanProperty
  var selectedProjectDetail: ArtifactDetail = _

  @BeanProperty
  var serviceDependencies: java.util.ArrayList[ArtifactDetail] = _

  @BeanProperty
  var allReleases: java.util.List[ReleaseNotes] = new java.util.ArrayList[ReleaseNotes]()

  @BeanProperty
  var tag: TagCloudModel = _

  val servletContext = FacesContext.getCurrentInstance().getExternalContext().getContext().asInstanceOf[ServletContext]
  val appContext = WebApplicationContextUtils.getWebApplicationContext(servletContext)
  val mongoTemplate = appContext.getBean(classOf[MongoTemplate])

  val query5 = new Query()
  query5.addCriteria(Criteria.where("artifactType").is("pom"))
  val projectArtifacts = mongoTemplate.find(query5, classOf[ArtifactDetail])

  projectNames ++= projectArtifacts.groupBy(_.getArtifactId).keys.toList

  def handleCityChange() = {
    println(" ----------------" + selectedProj)
    showTabs = selectedProj != null && selectedProj.length > 0

    val query = new Query()
    query.addCriteria(Criteria.where("artifactId").is(selectedProj))
    allProjects = mongoTemplate.find(query, classOf[ArtifactDetail])
    val tempList = new java.util.ArrayList[ArtifactDetail]
    allProjects.foreach(tempList.add(_))
    projectDataModel = new ArtifactDetailDataModel(tempList)

  }

  def onRowSelect(event: SelectEvent) = {
    selectedProjectDetail = event.getObject().asInstanceOf[ArtifactDetail]
    println(" --------------- " + selectedProjectDetail.version)
    val context = RequestContext.getCurrentInstance()
    context.execute("info1.show()")

    rootMaven = new DefaultMindmapNode(selectedProjectDetail.artifactId, selectedProjectDetail.groupId + ":" + selectedProjectDetail.artifactId + ":" + selectedProjectDetail.version + ":" + selectedProjectDetail.classifier, "FFCC00", false)
    selectedProjectDetail.children.foreach(child => rootMaven.addNode(new DefaultMindmapNode(child.artifactId + "(" + child.version + ")", "data", "6e9ebf", true)))

    allFilteredMavenIdentifiers = selectedProjectDetail.children.filter(_.getArtifactId.contains("-ws"))

    val query = new Query()
    query.addCriteria(Criteria.where("projectArtifact.artifactId").is(selectedProjectDetail.artifactId).andOperator(Criteria.where("projectArtifact.version").is(selectedProjectDetail.version)))
    allReleases = mongoTemplate.find(query, classOf[ReleaseNotes])

    tag = new DefaultTagCloudModel()

    selectedProjectDetail.children.foreach(artifact => {
      val query5 = new Query
      query5.addCriteria(Criteria.where("groupId").is(artifact.groupId).and("artifactId").is(artifact.artifactId).and("version").is(artifact.version))
      val changeSets = mongoTemplate.find(query5, classOf[ChangeSet])
      tag.addTag(new DefaultTagCloudItem(artifact.artifactId+"("+artifact.version+")", changeSets.flatMap(_.entries).size));  
    })
  }
}