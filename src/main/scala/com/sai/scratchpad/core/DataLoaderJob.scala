package com.sai.scratchpad.core

import scala.Option.option2Iterable
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions._
import scala.io.Source
import scala.xml.XML
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.VersionRangeRequest
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.tmatesoft.svn.core.SVNLogEntry
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.wc.SVNClientManager
import com.sai.pumpkin.domain.ArtifactDetail
import com.sai.pumpkin.domain.ArtifactMetadata
import com.sai.pumpkin.domain.ChangeSet
import java.util.ArrayList
import org.tmatesoft.svn.core.io.SVNRepository
import com.sai.pumpkin.domain.ChangeSetEntry
import com.sai.pumpkin.domain.ArtifactMetadata
import javax.servlet.ServletContext
import org.springframework.web.context.support.WebApplicationContextUtils
import javax.faces.context.FacesContext
import org.springframework.context.ApplicationContext
import org.eclipse.aether.util.artifact.JavaScopes

class DataLoaderJob(applicationContext: ApplicationContext) {

  println("Constructor")
  val nexusUrl = "http://10.70.101.186/nexus/service/local/data_index?a=%s&g=%s"

  val nexusUrlForRpm = "http://10.70.101.186/nexus/service/local/data_index?a=%s*&v=%s*"

  var gslSvnRepo = "http://10.70.101.200:7777/svn/gsl"

  val authManager = new BasicAuthenticationManager("skrishnamurthy", "002632");
  println("Constructor 0")
  DAVRepositoryFactory.setup();

  println("Constructor 1")
  SVNRepositoryFactoryImpl.setup();

  println("App context: " + applicationContext)

  def getMongoTemplate = applicationContext.getBean(classOf[MongoTemplate])

  def getAllRegisteredArtifacts = getMongoTemplate.findAll(classOf[ArtifactMetadata])

  def getRepoSession = Booter.getRepositorySystemSession

  def getRepoSystem = Booter.getRepositorySystem

  def getArtifactDetail(groupId: String, artifactId: String, version: String) = {
    val query5 = new Query
    query5.addCriteria(Criteria.where("groupId").is(groupId).and("artifactId").is(artifactId).and("version").is(version))
    getMongoTemplate.find(query5, classOf[ArtifactDetail])
  }

  def getMasterCoordinatesFromNexus(artifactId: String, groupId: String) = {
    //    val xml = XML.loadString(Source.fromFile("c:/pumpkinrepo/repo.xml").mkString)
    val xml = XML.loadString(Source.fromURL(String.format(nexusUrl, artifactId, groupId)).mkString)
    val nodes = xml \\ "artifact"
    val releases = nodes.filter(node => (node \ "contextId").text == "Releases").map(node => ((node \ "groupId").text, (node \ "artifactId").text, (node \ "version").text, (node \ "classifier").text))
    val filteredClassifiers = releases.filter(tuple => !tuple._4.contains("sources") && !tuple._4.contains("javadoc"))
    val dedupedCoordinates = filteredClassifiers.groupBy(tuple => (tuple._1, tuple._2, tuple._3)).flatMap(entry => if (entry._2.size > 1) entry._2.filter(_._4.length > 0) else List((entry._1._1, entry._1._2, entry._1._3, "")))
    dedupedCoordinates.toSeq.sortBy(t => t._3).reverse
  }

  def resolveDependencies(groupId: String, artifactId: String, version: String, extension: String, classifier: String) = {
    val mavenArtifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version)
    try {
      val collectRequest = new CollectRequest
      collectRequest.setRoot(new Dependency(mavenArtifact, ""))
      collectRequest.setRepositories(Booter.getRepositories.toList);
      Some(getRepoSystem.collectDependencies(getRepoSession, collectRequest).getRoot())
    } catch {
      case ex: Throwable => println(ex); None
    }
  }

  def isAlreadyProcessed(groupId: String, artifactId: String, version: String, classifier: String): Boolean = {
    val query5 = new Query
    query5.addCriteria(Criteria.where("groupId").is(groupId).and("artifactId").is(artifactId).and("version").is(version).and("classifier").is(classifier))
    val webserviceArtifacts = getMongoTemplate.find(query5, classOf[ArtifactDetail])
    !webserviceArtifacts.isEmpty
  }

  def transform(dependencyNode: DependencyNode): ArtifactDetail = {
    def getArtifactDetail(dependency: DependencyNode) = {
      val parent = new ArtifactDetail
      parent.groupId = dependency.getArtifact().getGroupId()
      parent.artifactId = dependency.getArtifact().getArtifactId()
      parent.version = dependency.getArtifact().getVersion()
      parent.classifier = dependency.getArtifact().getClassifier()
      parent.artifactType = if (dependency.getArtifact().getArtifactId().contains("ws")) "webservice" else dependencyNode.getArtifact().getExtension()
      parent.artifactReleaseType = if (dependency.getArtifact().getVersion().toLowerCase().contains("snapshot")) "snapshot" else "release"
      if(dependency.getDependency() != null) {
          parent.scope = dependency.getDependency().getScope()
      }
      parent
    }

    val artifactDetail = getArtifactDetail(dependencyNode)
    val dependentArtifactDetails = dependencyNode.getChildren().map(getArtifactDetail _).toList
    artifactDetail.children ++= dependentArtifactDetails
    artifactDetail
  }

  def resolveAndSave(dependency: DependencyNode) = {
    try {
      // resolve only the pom? TODO
      val artifactRequest = new ArtifactRequest
      artifactRequest.setArtifact(dependency.getArtifact)
      artifactRequest.setRepositories(Booter.getRepositories.toList)
      val artifactResult = Booter.getRepositorySystem.resolveArtifact(Booter.getRepositorySystemSession, artifactRequest)
      val converted = transform(dependency)
      converted.classifier = artifactResult.getArtifact().getClassifier()
      converted.filePath = artifactResult.getArtifact().getFile().getAbsolutePath()
      val meta = getMeta(converted.groupId, converted.artifactId)

      println("\t\t Resolving now: " + converted.artifactId + " | " + converted.version + " | " + converted.classifier)

      meta match {
        case None => println("No meta found")
        case _ => {
          val trunkRepo = meta.get.svnLocation.replace("#{gsl.repo}", gslSvnRepo)
          converted.svnTrunk = trunkRepo
          val tagsRepo = trunkRepo.replace("trunk", "tags/" + converted.artifactId + "-" + converted.version)
          converted.svnTag = tagsRepo
          converted.svnRevision = getSvnRevision(converted).toString

          val rpm = getRpm(meta.get, converted.version)
          println("\t\tRPM : " + rpm)
          converted.rpmName = rpm._2
          converted.rpmLocation = rpm._1
        }
      }
      getMongoTemplate.save(converted)
    } catch {
      case ex: Throwable => println("Rogue artifact wrong guess of classifier perhaps!" + dependency + " ===> " + ex)
    }
  }

  def getRpm(meta: ArtifactMetadata, version: String) = {
    try {
      val xml = XML.loadString(Source.fromURL(String.format(nexusUrlForRpm, meta.rpmName, version)).mkString)
      val nodes = (xml \\ "artifact")
      val commaSeparatedLocations = nodes.map(node => (node \ "resourceURI").text).filter(name => !name.contains("SNAPSHOT")).mkString(",")
      val commaSeparatedRpmNames = commaSeparatedLocations.split(",").map(token => token.substring(token.lastIndexOf("/") + 1)).mkString(",")
      (commaSeparatedLocations, commaSeparatedRpmNames)
    } catch {
      case ex: Throwable =>
        println(ex); println("RPM Not found for " + meta.rpmName + "::" + version + ".0")
        ("", "")
    }
  }

  def getMeta(groupId: String, artifactId: String): Option[ArtifactMetadata] = {
    try {
      val query5 = new Query
      query5.addCriteria(Criteria.where("groupId").is(groupId).andOperator(Criteria.where("artifactId").is(artifactId)))
      println("Meta ==> " + getMongoTemplate.find(query5, classOf[ArtifactMetadata]))
      Some(getMongoTemplate.find(query5, classOf[ArtifactMetadata]).get(0))
    } catch {
      case ex: Throwable => println("Meta not found: " + ex); None
    }
  }

  def getSvnRevision(artifact: ArtifactDetail) = {
    try {
      val cm = SVNClientManager.newInstance()
      val dc = cm.getDiffClient()
      val lc = cm.getLogClient()

      val repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(artifact.getSvnTag))
      repository.setAuthenticationManager(authManager);

      val entries = repository.log(Array(""), null, -1,
        0, true, true)
      entries.map(e => {
        val en = e.asInstanceOf[SVNLogEntry]
        en.getRevision
      }).toList.get(0)
    } catch {
      case ex: Throwable => print(ex); 0L
    }
  }

  def run(): Unit = {

    // split into multiple for scaling efficiently TODO
    println("Data loader job: Entered ")
    val allRegisteredArtifacts = getAllRegisteredArtifacts
    println("Data loader job: " + allRegisteredArtifacts)
    val allMasterCoordinates = allRegisteredArtifacts.flatMap(registeredArtifact => getMasterCoordinatesFromNexus(registeredArtifact.artifactId, registeredArtifact.groupId))

    println(allMasterCoordinates)
    allMasterCoordinates.foreach(coordinates => {
      val artifactMetadata = getAllRegisteredArtifacts.filter(meta => meta.groupId == coordinates._1 && meta.artifactId == coordinates._2).get(0)
      for (i <- 0 until artifactMetadata.goBackUpto) {
        println("Processing: " + coordinates)
        if (!isAlreadyProcessed(coordinates._1, coordinates._2, coordinates._3, coordinates._4)) {
          val root = resolveDependencies(coordinates._1, coordinates._2, coordinates._3, artifactMetadata.packaging, coordinates._4)
          root match {
            case None => println("Non existent artifact")
            case _ => resolveAndSave(root.get)
          }
        } else {
          println("Already processed ")
        }
        println("Processed: " + coordinates)
      }
    })
  }
}