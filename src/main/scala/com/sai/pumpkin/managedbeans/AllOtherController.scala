package com.sai.pumpkin.managedbeans

import java.io.Serializable
import java.util.ArrayList
import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import org.primefaces.context.RequestContext
import org.primefaces.event.SelectEvent
import org.primefaces.model.chart.PieChartModel
import org.primefaces.model.mindmap.DefaultMindmapNode
import org.primefaces.model.mindmap.MindmapNode
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.web.context.support.WebApplicationContextUtils
import com.sai.pumpkin.domain.ArtifactDetail
import com.sai.pumpkin.domain.ArtifactDetailDataModel
import com.sai.pumpkin.domain.ChangeSet
import com.sai.pumpkin.service.ArtifactDetailService
import javax.faces.context.FacesContext
import javax.servlet.ServletContext
import com.sai.pumpkin.domain.ArtifactMetadata
import com.sai.pumpkin.domain.JiraCalls
import com.sai.pumpkin.domain.JiraCalls

class AllOtherController extends Serializable {

  @BeanProperty
  var artifactDataModel: ArtifactDetailDataModel = _

  @BeanProperty
  var dependeeDataModel: ArtifactDetailDataModel = _

  @BeanProperty
  var filtered: java.util.List[ArtifactDetail] = new ArrayList[ArtifactDetail]

  @BeanProperty
  var selectedArtifactDetail: ArtifactDetail = _

  @BeanProperty
  var rootMaven: MindmapNode = _

  @BeanProperty
  var showTabs: Boolean = _

  @BeanProperty
  var allChangesets: java.util.List[ChangeSet] = new java.util.ArrayList[ChangeSet]()

  @BeanProperty
  var pie: PieChartModel = _

  @BeanProperty
  var allJira: java.util.List[String] = new java.util.ArrayList[String]()

  val servletContext = FacesContext.getCurrentInstance().getExternalContext().getContext().asInstanceOf[ServletContext]
  val appContext = WebApplicationContextUtils.getWebApplicationContext(servletContext)

  val service = appContext.getBean(classOf[ArtifactDetailService])

  val mongoTemplate = appContext.getBean(classOf[MongoTemplate])

  val params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()

  def allRegisteredArtifacts = mongoTemplate.findAll(classOf[ArtifactMetadata])

  val all = service.findAll
  filtered.clear()
  if (params != null && params.get("category") == null) {
    filtered.addAll(all)
  } else {
    val filteredArtifactsMeta = allRegisteredArtifacts.filter(_.artifactCatgory == params.get("category").trim())
    filtered.addAll(all.filter(artifactDetail => filteredArtifactsMeta.filter(meta => artifactDetail.groupId == meta.groupId && artifactDetail.artifactId == meta.artifactId).size > 0))
  }
  artifactDataModel = new ArtifactDetailDataModel(all)

  def onRowSelect(event: SelectEvent) = {
    selectedArtifactDetail = event.getObject().asInstanceOf[ArtifactDetail]

    rootMaven = new DefaultMindmapNode(selectedArtifactDetail.artifactId, selectedArtifactDetail.groupId + ":" + selectedArtifactDetail.artifactId + ":" + selectedArtifactDetail.version + ":" + selectedArtifactDetail.classifier, "FFCC00", false)
    selectedArtifactDetail.children.foreach(child => rootMaven.addNode(new DefaultMindmapNode(child.artifactId + "(" + child.version + ")", "data", "6e9ebf", true)))

    val dependee = service.dependee(selectedArtifactDetail)

    println("Dependee -" + dependee)
    dependeeDataModel = new ArtifactDetailDataModel(dependee)

    pie = new PieChartModel()

    allChangesets.clear()
    allJira.clear()
    val query5 = new Query
    query5.addCriteria(Criteria.where("groupId").is(selectedArtifactDetail.groupId).and("artifactId").is(selectedArtifactDetail.artifactId).and("version").is(selectedArtifactDetail.version))
    allChangesets.addAll(mongoTemplate.find(query5, classOf[ChangeSet]).toList)

    val allChangesetsPerAuthor = allChangesets.groupBy(_.committer)

    allChangesetsPerAuthor.map(tuple => pie.set(tuple._1, tuple._2.flatMap(_.entries).size))
    
    val query6 = new Query
    query6.addCriteria(Criteria.where("groupId").is(selectedArtifactDetail.groupId).and("artifactId").is(selectedArtifactDetail.artifactId).and("version").is(selectedArtifactDetail.version))
    
    val jiraCall = mongoTemplate.find(query6, classOf[JiraCalls])
    if(!jiraCall.isEmpty()) {
    	allJira.addAll(jiraCall.get(0).calls.toSet.filter(!_.startsWith("-")))
    }
    val context = RequestContext.getCurrentInstance()
    context.execute("info1.show()")
  }
}