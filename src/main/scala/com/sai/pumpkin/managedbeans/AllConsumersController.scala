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
import javax.servlet.ServletContext
import org.springframework.web.context.support.WebApplicationContextUtils

class AllConsumersController extends Serializable {

  @BeanProperty
  var consumerNames: java.util.List[String] = new java.util.ArrayList[String]()

  @BeanProperty
  var wsConsumers: java.util.List[ConsumerDetail] = new java.util.ArrayList[ConsumerDetail]()

  @BeanProperty
  var selectedCon: String = _

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
  var consumerDataModel: ConsumerDetailDataModel = _

  @BeanProperty
  var selectedConsumerDetail: ConsumerDetail = _

  @BeanProperty
  var serviceDependencies: java.util.ArrayList[ArtifactDetail] = _

  val servletContext = FacesContext.getCurrentInstance().getExternalContext().getContext().asInstanceOf[ServletContext]
  val appContext = WebApplicationContextUtils.getWebApplicationContext(servletContext)
  val mongoTemplate = appContext.getBean(classOf[MongoTemplate])

  val consumerCollection = mongoTemplate.getCollection("consumer_detail")

  consumerNames = consumerCollection.distinct("name").asInstanceOf[java.util.List[String]]

  def handleCityChange() = {
    println(" ----------------" + selectedCon)
    showTabs = selectedCon != null && selectedCon.length > 0

    val query = new Query()
    query.addCriteria(Criteria.where("name").is(selectedCon))
    wsConsumers = mongoTemplate.find(query, classOf[ConsumerDetail])
    val list = new java.util.ArrayList[ConsumerDetail]()
    wsConsumers.foreach(list.add(_)) 
    consumerDataModel = new ConsumerDetailDataModel(list)
  }

  def onRowSelect(event: SelectEvent) = {
    selectedConsumerDetail = event.getObject().asInstanceOf[ConsumerDetail]
    val context = RequestContext.getCurrentInstance()
    context.execute("info1.show()")

    val query = new Query()
    query.addCriteria(Criteria.where("name").is(selectedCon))
    wsConsumers = mongoTemplate.find(query, classOf[ConsumerDetail])
  }

}