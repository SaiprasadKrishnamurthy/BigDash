package com.sai.pumpkin.managedbeans

import scala.collection.JavaConversions._
import scala.beans.BeanProperty
import javax.servlet.ServletContext
import org.springframework.web.context.support.WebApplicationContextUtils
import javax.faces.context.FacesContext
import org.springframework.data.mongodb.core.MongoTemplate
import com.sai.pumpkin.domain.ArtifactDetail
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria
import java.util.Date
import com.sai.pumpkin.domain.ReleaseNotes
import java.util.ArrayList
import org.primefaces.context.RequestContext
import javax.faces.application.FacesMessage
import com.sai.pumpkin.domain.Distribution
import com.sai.pumpkin.domain.Distribution
import com.sai.pumpkin.domain.ArtifactDetailDataModel
import com.sai.pumpkin.domain.Distribution
import com.sai.pumpkin.domain.JiraCalls
import com.sai.pumpkin.domain.Distribution

class ReleaseNotesController {

  @BeanProperty
  var allProjectCoordinates: java.util.List[String] = _

  @BeanProperty
  var selectedProj: String = _

  @BeanProperty
  var showGrid: Boolean = _

  @BeanProperty
  var additionalArtifacts: java.util.List[String] = new java.util.ArrayList[String]

  @BeanProperty
  var additionalArtifactsSelected: java.util.List[String] = new java.util.ArrayList[String]

  @BeanProperty
  var date: Date = _

  @BeanProperty
  var releaseLead: String = _

  @BeanProperty
  var releaseName: String = _

  @BeanProperty
  var notes: String = _

  @BeanProperty
  var allReleaseNames: java.util.List[String] = new java.util.ArrayList[String]

  @BeanProperty
  var currRelease: ReleaseNotes = _

  @BeanProperty
  var showRelease: Boolean = _

  @BeanProperty
  var allArtifacts: java.util.List[ArtifactDetail] = new java.util.ArrayList[ArtifactDetail]()

  @BeanProperty
  var distributions: java.util.List[Distribution] = new java.util.ArrayList[Distribution]()

  @BeanProperty
  var allJira: java.util.List[String] = new java.util.ArrayList[String]()

  val servletContext = FacesContext.getCurrentInstance().getExternalContext().getContext().asInstanceOf[ServletContext]
  val appContext = WebApplicationContextUtils.getWebApplicationContext(servletContext)
  val mongoTemplate = appContext.getBean(classOf[MongoTemplate])

  val query5 = new Query()
  query5.addCriteria(Criteria.where("artifactType").is("pom"))
  val projectArtifacts = mongoTemplate.find(query5, classOf[ArtifactDetail])

  allProjectCoordinates = projectArtifacts.map(a => a.getArtifactId + "(" + a.getVersion + ")")

  val allReleases = mongoTemplate.findAll(classOf[ReleaseNotes])
  allReleaseNames ++= allReleases.map(_.releaseName)

  def handleChange() = {
    allJira.clear()
    if (selectedProj.contains("(")) {
      val selectedArtifactId = selectedProj.split("\\(")(0)
      val selectedVersion = selectedProj.split("\\)")(0).split("\\(")(1)

      println("Selected artifact id: " + selectedArtifactId)
      println("Selected version: " + selectedVersion)
      showGrid = true

      val query = new Query()
      query.addCriteria(Criteria.where("artifactId").is(selectedArtifactId).andOperator(Criteria.where("version").is(selectedVersion)))
      val projectArtifact = mongoTemplate.find(query5, classOf[ArtifactDetail]).get(0)

      val allArtifacts = mongoTemplate.findAll(classOf[ArtifactDetail])

      val filtered = allArtifacts.filter(a => projectArtifact.children.filter(p => a.artifactId.equals(p.artifactId) && a.version.equals(p.version)).size == 0 && !a.artifactId.equals(projectArtifact.artifactId))

      filtered.foreach(a => additionalArtifacts.add(a.getArtifactId + "(" + a.getVersion + ")"))

    } else {
      additionalArtifacts.clear
    }

  }

  def onChange() = {
    allJira.clear()
    distributions.clear()
    val query = new Query()
    query.addCriteria(Criteria.where("releaseName").is(releaseName))
    val r = mongoTemplate.find(query, classOf[ReleaseNotes])
    if (!r.isEmpty()) {
      currRelease = r.get(0)
      showRelease = true
    } else {
      showRelease = false
    }

    allArtifacts.add(currRelease.getProjectArtifact)

    val fullArtifacts = currRelease.getProjectArtifact.getChildren.map(a => {
      val query1 = new Query()
      query1.addCriteria(Criteria.where("artifactId").is(a.artifactId).andOperator(Criteria.where("version").is(a.version)))
      try {
        mongoTemplate.find(query1, classOf[ArtifactDetail]).get(0)
      } catch {
        case ex: Throwable => a
      }
    })

    allArtifacts.addAll(fullArtifacts)

    val belongingArtifact = currRelease.getProjectArtifact

    val distQuery = new Query()
    distQuery.addCriteria(Criteria.where("masterGroupId").is(belongingArtifact.groupId).and("masterArtifactId").is(belongingArtifact.artifactId).and("masterVersion").is(belongingArtifact.version))

    val dists = mongoTemplate.find(distQuery, classOf[Distribution])

    distributions.addAll(dists)

    val allJiraCalls = mongoTemplate.findAll(classOf[JiraCalls])

    println(" --- Zero ---- "+allJiraCalls.mkString("\n"))
    println(currRelease.projectArtifact.children.size())
    
    println(" --- One ---")
    println(currRelease.projectArtifact.children.map(a => allJiraCalls.filter(j => j.groupId == a.groupId && j.version == a.version && j.artifactId == a.artifactId)).mkString("\n\n"))
    println(" --- Two ---")
    
    
    val jiraCallsForRelease = currRelease.projectArtifact.children.flatMap(artifactDetail => allJiraCalls.filter(jira => jira.groupId == artifactDetail.groupId && jira.artifactId == artifactDetail.artifactId && jira.version == artifactDetail.version).flatMap(_.calls))

    allJira.addAll(jiraCallsForRelease.toSet.filter(!_.startsWith("-")))

  }

  def save() = {

    val query = new Query()
    query.addCriteria(Criteria.where("releaseName").is(releaseName))

    if (!mongoTemplate.find(query, classOf[ReleaseNotes]).isEmpty()) {
      RequestContext.getCurrentInstance().showMessageInDialog(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Release name should be unique"))
    } else {
      val releaseNote = new ReleaseNotes
      releaseNote.date = date
      releaseNote.releaseName = releaseName
      releaseNote.releaseLead = releaseLead
      releaseNote.notes = notes

      if (selectedProj.contains("(")) {
        val selectedArtifactId = selectedProj.split("\\(")(0)
        val selectedVersion = selectedProj.split("\\)")(0).split("\\(")(1)
        val query = new Query()
        query.addCriteria(Criteria.where("artifactId").is(selectedArtifactId).andOperator(Criteria.where("version").is(selectedVersion)))
        val projectArtifact = mongoTemplate.find(query, classOf[ArtifactDetail]).get(0)
        releaseNote.projectArtifact = projectArtifact
      }

      val listAdditional = new ArrayList[ArtifactDetail]()

      additionalArtifactsSelected.foreach(a => {
        val selectedArtifactId = a.split("\\(")(0)
        val selectedVersion = a.split("\\)")(0).split("\\(")(1)
        val query = new Query()
        query.addCriteria(Criteria.where("artifactId").is(a).andOperator(Criteria.where("version").is(selectedVersion)))
        val projectArtifact = mongoTemplate.find(query, classOf[ArtifactDetail]).get(0)
        listAdditional.add(projectArtifact)
      })

      releaseNote.components ++= listAdditional
      mongoTemplate.save(releaseNote)

      RequestContext.getCurrentInstance().showMessageInDialog(new FacesMessage("Success", "Release notes saved"))
    }
  }

}