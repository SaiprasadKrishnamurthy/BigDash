package com.sai.scratchpad.core

import com.sai.pumpkin.domain.ArtifactMetadata
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.resolution.VersionRangeRequest
import scala.collection.JavaConversions._
import com.sai.pumpkin.domain.ArtifactDetail
import org.tmatesoft.svn.core.SVNLogEntry
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.io.SVNRepository
import java.util.ArrayList
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.wc.SVNClientManager
import com.sai.pumpkin.domain.ChangeSet
import com.sai.pumpkin.domain.ChangeSetEntry
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import com.sai.pumpkin.domain.ChangeSet
import javax.servlet.ServletContext
import org.springframework.web.context.support.WebApplicationContextUtils
import javax.faces.context.FacesContext
import org.springframework.context.ApplicationContext

// migration job to load svn logs retrospectively
class SvnLogLoader(applicationContext: ApplicationContext) {

  def run(): Unit = {

    val authManager = new BasicAuthenticationManager("skrishnamurthy", "002632");

    DAVRepositoryFactory.setup();

    SVNRepositoryFactoryImpl.setup();

    def getMongoTemplate = applicationContext.getBean(classOf[MongoTemplate])

    def getArtifactDetail(groupId: String, artifactId: String, version: String) = {
      val query5 = new Query
      query5.addCriteria(Criteria.where("groupId").is(groupId).and("artifactId").is(artifactId).and("version").is(version))
      getMongoTemplate.find(query5, classOf[ArtifactDetail])
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

    def getAvailableVersions(groupId: String, artifactId: String) = {
      val rangeRequest = new VersionRangeRequest
      val artifact = new DefaultArtifact(s"${groupId}:${artifactId}:[0,)");
      rangeRequest.setArtifact(artifact);
      rangeRequest.setRepositories(Booter.getRepositories.toList)
      val rangeResult = Booter.getRepositorySystem.resolveVersionRange(Booter.getRepositorySystemSession, rangeRequest)
      rangeResult.getVersions().filterNot(version => version.toString.contains("SNAPSHOT")).reverse.toList.map(version => version.toString)
    }

    // main to get the log
    def handleSvnLog(artifactDetail: ArtifactDetail, availableMavenVersions: List[String]) = {
      try {
        val currVersion = artifactDetail.version
        val prevVersion = availableMavenVersions.get(availableMavenVersions.indexOf(currVersion) + 1)
        val currSvnRevision = getSvnRevision(artifactDetail)
        val prevSvnRevision = getSvnRevision(getArtifactDetail(artifactDetail.groupId, artifactDetail.artifactId, prevVersion).get(0))
        println("SVN Log to be mined for " + artifactDetail + "Mvn current: " + currVersion + ", Mvn prev: " + prevVersion + "  SVN curr: " + currSvnRevision + ", SVN prev: " + prevSvnRevision)

        val repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(artifactDetail.getSvnTrunk))
        repository.setAuthenticationManager(authManager)
        val entries = repository.log(Array(""), null, prevSvnRevision, currSvnRevision, true, true)
        val changeSets = entries.flatMap(entry => handleChangeSets(entry, repository, artifactDetail))

        println(changeSets.mkString("\n"))
        println("\n\n\n")
        changeSets.toList.foreach(changeSet => getMongoTemplate.save(changeSet))
        println("Saved.." + artifactDetail)
      } catch {
        case ex: Throwable => println(ex + " ==> " + artifactDetail)
      }

    }

    def handleChangeSets(chs: Any, repository: SVNRepository, artifactDetail: ArtifactDetail) = {
      val changeSets = new ArrayList[ChangeSet]
      val svnLog = chs.asInstanceOf[SVNLogEntry]
      val changeSet = ChangeSet()
      changeSet.revision = svnLog.getRevision()
      val userName = svnLog.getAuthor()
      changeSet.committer = userName
      changeSet.commitDate = svnLog.getDate()
      changeSet.commitMessage = svnLog.getMessage()
      changeSet.artifactId = artifactDetail.artifactId

      changeSet.groupId = artifactDetail.groupId
      changeSet.version = artifactDetail.version
      val entries = svnLog.getChangedPaths().flatMap(entry => {
        val key = entry._1
        val svnLogEntry = entry._2
        val entries = new ArrayList[ChangeSetEntry]
        if (svnLogEntry.getPath().contains(repository.getRepositoryPath(""))) {
          val changeSetEntry = ChangeSetEntry()
          changeSetEntry.file = svnLogEntry.getPath()
          changeSetEntry.changeType = svnLogEntry.getType().toString
          entries.add(changeSetEntry)
        }
        entries
      })

      if (!entries.isEmpty) {
        changeSet.entries.addAll(entries)
        changeSets.add(changeSet)
      }
      changeSets
    }

    // start here.
    val allArtifacts = getMongoTemplate.findAll(classOf[ArtifactDetail])

    def isNotProcessed(artifact: ArtifactDetail) = {
      val query5 = new Query
      query5.addCriteria(Criteria.where("groupId").is(artifact.groupId).and("artifactId").is(artifact.artifactId).and("version").is(artifact.version))
      getMongoTemplate.find(query5, classOf[ChangeSet]).isEmpty
    }

    allArtifacts.foreach(artifact => {
      println("Artifact: " + artifact)
      if (isNotProcessed(artifact)) {
        val availableRevisions = getAvailableVersions(artifact.groupId, artifact.artifactId)
        handleSvnLog(artifact, availableRevisions)
      } else {
        println("Already mined..")
      }
    })

  }

}