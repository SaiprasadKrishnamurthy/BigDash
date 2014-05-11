package com.sai.pumpkin.managedbeans

import org.primefaces.extensions.model.timeline.TimelineModel
import java.util.ArrayList
import scala.beans.BeanProperty
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.web.context.support.WebApplicationContextUtils
import javax.faces.context.FacesContext
import javax.servlet.ServletContext
import com.sai.pumpkin.domain.ReleaseNotes
import com.sai.pumpkin.domain.ArtifactDetail
import java.util.LinkedHashMap
import scala.collection.JavaConversions._
import org.primefaces.extensions.model.timeline.TimelineEvent
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria
import com.sai.pumpkin.domain.ChangeSet

class ReleaseTimelineController {

  @BeanProperty
  var timelineModel: TimelineModel = _

  @BeanProperty
  var allArtifacts = new ArrayList[String]()

  @BeanProperty
  var selectedArtifactId: String = _

  val servletContext = FacesContext.getCurrentInstance().getExternalContext().getContext().asInstanceOf[ServletContext]
  val appContext = WebApplicationContextUtils.getWebApplicationContext(servletContext)
  val mongoTemplate = appContext.getBean(classOf[MongoTemplate])

  val artifactIds = mongoTemplate.findAll(classOf[ArtifactDetail]).map(_.artifactId).toSet

  allArtifacts.addAll(artifactIds)

  def onChange() {
    println("Selected artifact " + selectedArtifactId)

    val query5 = new Query
    query5.addCriteria(Criteria.where("artifactId").is(selectedArtifactId))
    val versionMap = mongoTemplate.find(query5, classOf[ChangeSet]).toList.groupBy(_.version)
    val sortedByDate = versionMap.mapValues(_.sortBy(_.commitDate).map(_.commitDate))
    

    timelineModel = new TimelineModel()
    
    println(sortedByDate.mkString("\n"))
    
    sortedByDate.foreach(tuple => timelineModel.add(new TimelineEvent(tuple._1, tuple._2.get(tuple._2.size - 1))))
    
    

  }

}