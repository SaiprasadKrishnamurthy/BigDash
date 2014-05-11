package com.sai.scratchpad.core

import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.artifact.DefaultArtifact
import scala.collection.JavaConversions._
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.VersionRangeRequest
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import com.sai.pumpkin.domain.ArtifactMetadata

object MavenBootstrap {

  def main(args: Array[String]): Unit = {

    val session = Booter.getRepositorySystemSession
    val system = Booter.getRepositorySystem

    val rangeRequest = new VersionRangeRequest
    val artifact = new DefaultArtifact(s"aero.sita.gsl.eventmanager:eventmgr-ws-presentation:[0,)");
    rangeRequest.setArtifact(artifact);
    rangeRequest.setRepositories(Booter.getRepositories.toList)
    val rangeResult = system.resolveVersionRange(session, rangeRequest)

    println(rangeResult.getVersions().filterNot(version => version.toString.contains("SNAPSHOT")))

    val allCoordinates = List(
      ("aero.sita.gsl.oman", "oman-base", "pom", "#{gsl.repo}/Projects/Oman/Base/trunk", "sitaOman", "", "project", 2, ""),
      ("aero.sita.gsl.profiler", "profiler-ws-presentation", "war", "#{gsl.repo}/Components/Profiler/Presentation-WS/trunk", "sitaProfilerwsPortalRelease", "#{product_releases_yum_repo}", "webservice", 3, "ProfileManagementServiceWSDLTypes.xsd"),
      ("aero.sita.gsl.eventmanager", "eventmgr-ws-presentation", "war", "#{gsl.repo}/Components/EventManager/Presentation-WS/trunk", "sitaEventmgrwsPortalRelease", "#{product_releases_yum_repo}", "webservice", 3, "EventManagementServiceWSDLTypes.xsd"),
      ("aero.sita.gsl.watchlists", "watchlist-ws-presentation", "war", "#{gsl.repo}/Components/WatchLists/Presentation-WS/WatchList-WS/trunk", "sitaWatchlistwsPortalRelease", "#{product_releases_yum_repo}", "webservice", 3, "WatchlistManagementServiceWSDLTypes.xsd"),
      ("aero.sita.gsl.audit", "audit-ws-presentation", "war", "#{gsl.repo}/Components/Audit/Presentation-WS/trunk", "sitaAuditwsPortalRelease", "#{product_releases_yum_repo}", "webservice", 3, "AuditServiceWSDLTypes.xsd"),
      ("aero.sita.gsl.risk", "irisk-ws-presentation", "war", "#{gsl.repo}/Components/Risk/Presentation-WS/trunk", "sitaIriskwsPortalRelease", "#{product_releases_yum_repo}", "webservice", 3, "RiskSourcesServiceWSDLTypes.xsd"),
      ("aero.sita.gsl.cpadmin", "cpadmin-ws-presentation", "war", "#{gsl.repo}/Components/CarrierPortal/CarrierAdmin/Presentation-WS/trunk", "sitaCpadminwsPortalRelease", "#{product_releases_yum_repo}", "webservice", 3, "CarrierAdminManagementServiceWSDLTypes.xsd"),
      ("aero.sita.gsl.app", "app-ws", "war", "#{gsl.repo}/Components/APP/PreClearance/Presentation-ws/trunk", "sitaAppwsPortalRelease", "#{product_releases_yum_repo}", "webservice", 3, ""),
      ("aero.sita.gsl.person", "person-ws-presentation", "war", "#{gsl.repo}/Components/Person/Presentation-WS/trunk", "sitaPersonwsPortalRelease", "#{product_releases_yum_repo}", "webservice", 3, "PersonManagementServiceWSDLTypes.xsd"),
      ("aero.sita.gsl.oman", "OmanComponentConfiguration", "zip", "#{gsl.repo}/Projects/Oman/Configuration/Application/trunk", "sitaOmanComponentConfiguration", "#{oman_releases_yum_repo}", "config", 3, ""),
      ("aero.sita.gsl.oman", "OmanEnvironmentConfiguration", "zip", "#{gsl.repo}/Projects/Oman/Configuration/Environment/trunk", "sitaOmanEnvironmentConfiguration", "#{oman_releases_yum_repo}", "config", 3, ""),
      ("aero.sita.gsl.risk", "EntryExit-Adaptor", "jar", "#{gsl.repo}/Components/Risk/RID/EntryExitAdaptor/trunk", "sitaEntryExitAdaptor", "#{product_releases_yum_repo}", "webservice", 3, ""),
      ("aero.sita.gsl.risk", "App-Adaptor", "jar", "#{gsl.repo}/Components/Risk/RID/AppAdaptor/trunk", "sitaAppAdaptor", "#{product_releases_yum_repo}", "adapter", 3, ""),
      ("aero.sita.gsl.risk", "Interpol-Adaptor", "jar", "#{gsl.repo}/Components/Risk/RID/InterpolAdaptor/trunk", "sitaInterpolAdaptor", "#{product_releases_yum_repo}", "adapter", 3, ""),
      ("aero.sita.gsl.risk", "RIDCore-Adaptor", "jar", "#{gsl.repo}/Components/Risk/RID/RIDCoreAdaptor/trunk", "sitaRIDCoreAdaptor", "#{product_releases_yum_repo}", "adapter", 3, ""),
      ("aero.sita.gsl.risk", "WatchList-Adaptor", "jar", "#{gsl.repo}/Components/Risk/RID/WatchListAdaptor/trunk", "sitaWatchListAdaptor", "#{product_releases_yum_repo}", "adapter", 3, ""),
      ("aero.sita.gsl.watchlists", "wi-core", "zip", "#{gsl.repo}/Components/WatchLists/Core/WarningsIndex-core/trunk", "sitaWiCore.spec", "#{product_releases_yum_repo}", "standalone", 3, ""),
      ("aero.sita.gsl.refdata", "reference-data-api", "jar", "#{gsl.repo}/Components/ReferenceData/API/trunk", "", "#{product_releases_yum_repo}", "standalone", 3, ""),
      ("aero.sita.gsl.refdata", "reference-data-core", "jar", "#{gsl.repo}/Components/ReferenceData/Core/trunk", "", "", "binary", 3, ""))

    val ctx = new ClassPathXmlApplicationContext("appContext.xml") 
    val mongoTemplate = ctx.getBean(classOf[MongoTemplate])

    val metas = allCoordinates.map(t => {
      val meta = new ArtifactMetadata(t._1, t._2, t._3)
      meta.svnLocation = t._4
      meta.rpmName = t._5
      meta.yumLocation = t._6
      meta.artifactCatgory = t._7
      meta.goBackUpto = t._8
      meta.interfaceAnalysis = meta.artifactId.contains("-ws")
      meta.topLevelXmlSchema = t._9
      
      if(meta.artifactCatgory == "project") {
        meta.distributionKey = "oman"
      }
      meta
    })
    metas.foreach(m => { println("Before: " + m); mongoTemplate.save(m); println("After " + m) })
  }

}