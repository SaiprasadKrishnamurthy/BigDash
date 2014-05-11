package com.sai.pumpkin.managedbeans

import java.io.File
import java.io.FileInputStream
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date

import scala.beans.BeanProperty
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.immutable.TreeMap

import org.primefaces.context.RequestContext
import org.primefaces.event.NodeSelectEvent
import org.primefaces.model.DefaultStreamedContent
import org.primefaces.model.DefaultTreeNode
import org.primefaces.model.StreamedContent
import org.primefaces.model.TreeNode
import org.primefaces.model.chart.PieChartModel
import org.primefaces.model.mindmap.DefaultMindmapNode
import org.primefaces.model.mindmap.MindmapNode
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.web.context.support.WebApplicationContextUtils

import com.sai.pumpkin.domain.ArtifactDetail
import com.sai.pumpkin.domain.ChangeSet
import com.sai.pumpkin.domain.ConsumerDetail
import com.sai.pumpkin.domain.ReleaseNotes

import javax.faces.application.FacesMessage
import javax.faces.context.FacesContext
import javax.servlet.ServletContext

class AllWebservicesController extends Serializable {

  @BeanProperty
  var wsVersions = new java.util.ArrayList[String]()

  @BeanProperty
  var wsConsumers: java.util.List[ConsumerDetail] = new java.util.ArrayList[ConsumerDetail]()

  @BeanProperty
  var selectedWs: String = _

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
  var allMavenIdentifiers: java.util.List[ArtifactDetail] = new java.util.ArrayList[ArtifactDetail]()

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
  var artifactFile: StreamedContent = _

  @BeanProperty
  var currArtifact: ArtifactDetail = _

  @BeanProperty
  var interfacesAvailable: Boolean = _

  @BeanProperty
  var interfacesPath: String = _

  @BeanProperty
  var allReleases: java.util.List[ReleaseNotes] = new java.util.ArrayList[ReleaseNotes]()

  @BeanProperty
  var allChangesets: java.util.List[ChangeSet] = new java.util.ArrayList[ChangeSet]()

  @BeanProperty
  var pie: PieChartModel = _

  val servletContext = FacesContext.getCurrentInstance().getExternalContext().getContext().asInstanceOf[ServletContext]
  val appContext = WebApplicationContextUtils.getWebApplicationContext(servletContext)
  val mongoTemplate = appContext.getBean(classOf[MongoTemplate])

  val query5 = new Query()
  query5.addCriteria(Criteria.where("artifactId").regex("-ws"))
  val webserviceArtifacts = mongoTemplate.find(query5, classOf[ArtifactDetail])

  wsVersions ++= webserviceArtifacts.groupBy(_.getArtifactId).keys.toList

  def handleCityChange() = {
    println(" ----------------" + selectedWs)
    showTabs = selectedWs != null && selectedWs.length > 0

    if (showTabs) createTree()
  }

  def createTree() = {
    val query = new Query()
    query.addCriteria(Criteria.where("artifactId").is(selectedWs))
    val webserviceArtifacts = mongoTemplate.find(query, classOf[ArtifactDetail])

    println(" 1 ==> " + webserviceArtifacts)
    val map = webserviceArtifacts.groupBy(_.getClassifier)
    println("2 ==> " + map)
    root = new DefaultTreeNode("Root", null)

    TreeMap(map.toSeq: _*).foreach(tuple => {
      val node = new DefaultTreeNode(if (tuple._1.isEmpty) "(Versionless)" else tuple._1 + " (service version)", root)
      tuple._2.foreach(a => {
        new DefaultTreeNode(a.version + " (maven artifact version)", node)
      })
    })
  }

  def onNodeSelect(event: NodeSelectEvent) = {
    selectedNode = event.getTreeNode()
    mavenVersionSelected = !selectedNode.getData().toString().contains("V")
    println("maven version selected: " + mavenVersionSelected)
    if (selectedNode.getData().toString().startsWith("V")) serviceVersion = selectedNode.getData().toString().replace(" (service version)", "")

    if (mavenVersionSelected) mavenVersion = selectedNode.getData().toString().replace("(maven artifact version)", "").trim

    if (mavenVersionSelected) {
      println(mavenVersion + " -------------")
      val query6 = new Query()
      query6.addCriteria(Criteria.where("artifactId").is(selectedWs).andOperator(Criteria.where("version").is(mavenVersion)))
      val artifact = mongoTemplate.find(query6, classOf[ArtifactDetail]).get(0)
      currArtifact = artifact
      rootMaven = new DefaultMindmapNode(artifact.artifactId, artifact.groupId + ":" + artifact.artifactId + ":" + artifact.version + ":" + artifact.classifier, "FFCC00", false)
      artifact.children.foreach(child => rootMaven.addNode(new DefaultMindmapNode(child.artifactId + "(" + child.version + ")", "data", "6e9ebf", true)))
      mavenVersion = selectedNode.getData().toString().replace("(maven artifact version)", "").trim
      val context = RequestContext.getCurrentInstance()
      context.execute("info1.show()")

      val query7 = new Query()
      query7.addCriteria(Criteria.where("services.artifactId").is(selectedWs).andOperator(Criteria.where("services.version").is(mavenVersion)))
      wsConsumers = mongoTemplate.find(query7, classOf[ConsumerDetail])

      val query8 = new Query()
      query8.addCriteria(Criteria.where("children.artifactId").is(selectedWs).andOperator(Criteria.where("children.version").is(mavenVersion)))
      val _projects = mongoTemplate.find(query8, classOf[ArtifactDetail])

      projects = _projects.map(a => a.groupId + " | " + a.artifactId + " | " + a.version).toList

      val allIds = mongoTemplate.findAll(classOf[ArtifactDetail])

      allMavenIdentifiers = allIds
      allFilteredMavenIdentifiers = allIds

      download()

      val query = new Query()
      query.addCriteria(Criteria.where("projectArtifact.children.artifactId").is(currArtifact.artifactId).andOperator(Criteria.where("projectArtifact.children.version").is(currArtifact.version)))
      allReleases = mongoTemplate.find(query, classOf[ReleaseNotes])

      println(" ------------- All Releases " + allReleases)

      pie = new PieChartModel()

      allChangesets.clear()

      val query5 = new Query
      query5.addCriteria(Criteria.where("groupId").is(artifact.groupId).and("artifactId").is(artifact.artifactId).and("version").is(artifact.version))
      allChangesets.addAll(mongoTemplate.find(query5, classOf[ChangeSet]).toList)

      val allChangesetsPerAuthor = allChangesets.groupBy(_.committer)

      allChangesetsPerAuthor.map(tuple => pie.set(tuple._1, tuple._2.flatMap(_.entries).size))
    }
  }

  def saveConsumer() = {
    println("$$$$$$$$$$$$$$" + consumerArtifactId)

    val query6 = new Query()
    query6.addCriteria(Criteria.where("artifactId").is(selectedWs).andOperator(Criteria.where("version").is(mavenVersion)))
    val wsartifact = mongoTemplate.find(query6, classOf[ArtifactDetail]).get(0)

    val query7 = new Query()
    query7.addCriteria(Criteria.where("artifactId").is(consumerArtifactId).andOperator(Criteria.where("version").is(consumerVersion)))
    val consumerartifact = mongoTemplate.find(query7, classOf[ArtifactDetail])

    val currArtifact = consumerartifact.size() match {
      case 0 => { val art = new ArtifactDetail; art.groupId = consumerGroupId; art.artifactId = consumerArtifactId; art.version = consumerVersion; art.classifier = consumerClassifier; art }
      case _ => consumerartifact.get(0)
    }

    val query8 = new Query()
    query8.addCriteria(Criteria.where("artifactDetail.artifactId").is(consumerArtifactId).andOperator(Criteria.where("artifactDetail.version").is(consumerVersion)))
    val existingConsumerDetail = mongoTemplate.find(query8, classOf[ConsumerDetail])

    val consumerDetail = existingConsumerDetail.size match {
      case 0 => new ConsumerDetail
      case _ => existingConsumerDetail.get(0)
    }

    consumerDetail.services += wsartifact
    consumerDetail.artifactDetail = currArtifact

    val df = new SimpleDateFormat("dd/mm/yy")
    consumerDetail.fromDate = consumerStartDate
    consumerDetail.toDate = consumerEndDate
    consumerDetail.name = consumerName

    consumerDetail.tags = consumerTag

    val update = new Update()
    update.set("artifactDetail", consumerDetail.artifactDetail)
    update.set("name", consumerDetail.name)
    update.set("artifactDetail", consumerDetail.artifactDetail)
    update.set("fromDate", consumerDetail.fromDate)
    update.set("tags", consumerDetail.tags)
    update.set("services", consumerDetail.services)

    mongoTemplate.upsert(query8, update, classOf[ConsumerDetail])

    RequestContext.getCurrentInstance().showMessageInDialog(new FacesMessage("Success", "Saved"))

  }

  def download() = {
  }
}