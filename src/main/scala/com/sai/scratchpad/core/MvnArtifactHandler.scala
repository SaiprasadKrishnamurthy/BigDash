//package com.sai.scratchpad.core
//
//import scala.collection.JavaConversions._
//import org.eclipse.aether.artifact.DefaultArtifact
//import org.eclipse.aether.collection.CollectRequest
//import org.eclipse.aether.graph.Dependency
//import org.eclipse.aether.graph.DependencyNode
//import org.eclipse.aether.resolution.ArtifactRequest
//import org.eclipse.aether.resolution.VersionRangeRequest
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.data.mongodb.core.MongoTemplate
//import org.springframework.data.mongodb.core.query.Criteria
//import org.springframework.data.mongodb.core.query.Query
//import org.springframework.stereotype.Component
//import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
//import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory
//import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
//import org.tmatesoft.svn.core.io.SVNRepositoryFactory
//import org.tmatesoft.svn.core.wc.SVNClientManager
//import com.sai.pumpkin.domain.ArtifactDetail
//import com.sai.pumpkin.domain.ArtifactMetadata
//import org.tmatesoft.svn.core.SVNURL
//import org.tmatesoft.svn.core.SVNLogEntry
//
//@Component
//class MvnArtifactHandler {
//
//  @Autowired
//  var mongoTemplate: MongoTemplate = _
//
//  var gslSvnRepo = "http://10.70.101.200:7777/svn/gsl"
//
//  var productYum = "http://10.70.101.28/yum/product_RELEASES/trunk/"
//
//  var omanYum = "http://10.70.101.28/yum/oman_RELEASES/trunk/"
//
//  DAVRepositoryFactory.setup();
//
//  SVNRepositoryFactoryImpl.setup();
//
//  //  @Scheduled(cron = "0 0 * * * ?")
//  def process() = {
//
//    println("Started JOB ------------------------ ")
//    val session = Booter.getRepositorySystemSession
//    val system = Booter.getRepositorySystem
//    val artifactMeta = mongoTemplate.findAll(classOf[ArtifactMetadata])
//    val allCoordinates = artifactMeta.map(meta => (meta.groupId, meta.artifactId, meta.packaging))
//
//    val guessedVersions = List("", "V1", "V2", "V3", "V4")
//    val allMultipliedCoordinates = allCoordinates.flatMap(c => guessedVersions.map(g => (c._1, c._2, c._3, g)))
//
//    val coordinatesWithClassifiers = allMultipliedCoordinates.map(t => if (!t._2.contains("-ws-")) (t._1, t._2, t._3, "") else (t._1, t._2, t._3, t._4))
//
//    def getAvailableVersions(coordinate: (String, String, String, String)) = {
//      val rangeRequest = new VersionRangeRequest
//      val artifact = new DefaultArtifact(s"${coordinate._1}:${coordinate._2}:[0,)");
//      rangeRequest.setArtifact(artifact);
//      rangeRequest.setRepositories(Booter.getRepositories.toList)
//      val rangeResult = system.resolveVersionRange(session, rangeRequest)
//      rangeResult.getVersions().filterNot(version => version.toString.contains("SNAPSHOT")).reverse.toList.map(version => version.toString)
//    }
//
//    def resolveDependencies(groupId: String, artifactId: String, version: String, extension: String, classifier: String) = {
//      val mavenArtifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version)
//      try {
//        val collectRequest = new CollectRequest
//        collectRequest.setRoot(new Dependency(mavenArtifact, ""))
//        collectRequest.setRepositories(Booter.getRepositories.toList);
//        Some(system.collectDependencies(session, collectRequest).getRoot())
//      } catch {
//        case ex: Throwable => println(ex); None
//      }
//    }
//
//    def isAlreadyProcessed(groupId: String, artifactId: String, version: String, classifier: String): Boolean = {
//      val query5 = new Query
//      query5.addCriteria(Criteria.where("groupId").is(groupId).and("artifactId").is(artifactId).and("version").is(version).and("classifier").is(classifier))
//      val webserviceArtifacts = mongoTemplate.find(query5, classOf[ArtifactDetail])
//      !webserviceArtifacts.isEmpty
//    }
//
//    def transform(dependencyNode: DependencyNode): ArtifactDetail = {
//      def getArtifactDetail(dependency: DependencyNode) = {
//        val parent = new ArtifactDetail
//        parent.groupId = dependency.getArtifact().getGroupId()
//        parent.artifactId = dependency.getArtifact().getArtifactId()
//        parent.version = dependency.getArtifact().getVersion()
//        parent.classifier = dependency.getArtifact().getClassifier()
//        parent.artifactType = if (dependency.getArtifact().getArtifactId().contains("ws")) "webservice" else dependencyNode.getArtifact().getExtension()
//        parent.artifactReleaseType = if (dependency.getArtifact().getVersion().toLowerCase().contains("snapshot")) "snapshot" else "release"
//        parent
//      }
//
//      val artifactDetail = getArtifactDetail(dependencyNode)
//      val dependentArtifactDetails = dependencyNode.getChildren().map(getArtifactDetail _).toList
//      artifactDetail.children ++= dependentArtifactDetails
//      artifactDetail
//    }
//
//    val versionsPerCoordinates = coordinatesWithClassifiers.map(c => (c._1, c._2, c._3, c._4, getAvailableVersions(c).take(1)))
//
//    println(versionsPerCoordinates)
//
//    // orchestrator (sub main function) TODO, really imperative and I don't like it. Make it more functional!!!
//    def _process(identifier: (String, String, String, String, List[String])) = {
//      val groupId = identifier._1
//      val artifactId = identifier._2
//      val extension = identifier._3
//      val classifier = identifier._4
//      val availableVersions = identifier._5
//
//      println(s" Processing: $identifier")
//
//      availableVersions.foreach(version => {
//        if (!isAlreadyProcessed(groupId, artifactId, version, classifier)) {
//          println(s"\t\t$groupId, $artifactId, $version, $classifier")
//          val root = resolveDependencies(groupId, artifactId, version, extension, classifier)
//          root match {
//            case None => println("Non existent artifact")
//            case _ => resolveAndSave(root.get)
//          }
//        } else {
//          println("Already processed " + version)
//        }
//      })
//
//      def resolveAndSave(dependency: DependencyNode) = {
//        try {
//          val artifactRequest = new ArtifactRequest
//          artifactRequest.setArtifact(dependency.getArtifact)
//          artifactRequest.setRepositories(Booter.getRepositories.toList)
//          val artifactResult = Booter.getRepositorySystem.resolveArtifact(Booter.getRepositorySystemSession, artifactRequest)
//          val converted = transform(dependency)
//          converted.classifier = artifactResult.getArtifact().getClassifier()
//          converted.filePath = artifactResult.getArtifact().getFile().getAbsolutePath()
//          val meta = getMeta(converted.groupId, converted.artifactId)
//          
//          println("\t\t Resolving now: "+converted.artifactId+" | "+converted.version + " | "+converted.classifier)
//
//          meta match {
//            case None => println("No meta found")
//            case _ => {
//              val trunkRepo = meta.get.svnLocation.replace("#{gsl.repo}", gslSvnRepo)
//              converted.svnTrunk = trunkRepo
//              val tagsRepo = trunkRepo.replace("trunk", "tags/" + converted.artifactId + "-" + converted.version)
//              converted.svnTag = tagsRepo
//              converted.yumRepoUrl = if (meta.get.yumLocation.equals("#{product_releases_yum_repo}")) productYum else omanYum
//              converted.svnRevision = getSvnRevision(converted).toString
//              
//              handleYum(converted, meta.get)
//            }
//          }
//          mongoTemplate.save(converted)
//        } catch {
//          case ex: Throwable => println("Rogue artifact wrong guess of classifier perhaps!" + dependency)
//        }
//
//      }
//
//      def getMeta(groupId: String, artifactId: String): Option[ArtifactMetadata] = {
//        try {
//          val query5 = new Query
//          query5.addCriteria(Criteria.where("groupId").is(groupId).andOperator(Criteria.where("artifactId").is(artifactId)))
//          println("Meta ==> " + mongoTemplate.find(query5, classOf[ArtifactMetadata]))
//          Some(mongoTemplate.find(query5, classOf[ArtifactMetadata]).get(0))
//        } catch {
//          case ex: Throwable => println("Meta not found: "+ex); None
//        }
//      }
//
//      println("\n\n--------------------\n\n")
//    }
//
//    // starting point.
//    versionsPerCoordinates.foreach(_process _)
//
//  }
//
//  def getSvnRevision(artifact: ArtifactDetail) = {
//    try {
//      val cm = SVNClientManager.newInstance()
//      val dc = cm.getDiffClient()
//      val lc = cm.getLogClient()
//
//      // TODO remove hard coded
//      val authManager = new BasicAuthenticationManager("skrishnamurthy", "002632");
//
//      val repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(artifact.getSvnTag))
//      repository.setAuthenticationManager(authManager);
//
//      val entries = repository.log(Array(""), null, -1,
//        0, true, true)
//      entries.map(e => {
//        val en = e.asInstanceOf[SVNLogEntry]
//        en.getRevision
//      }).toList.get(0)
//    } catch {
//      case ex: Throwable => print(ex); 0L
//    }
//  }
//
//  def handleYum(detail: ArtifactDetail, meta: ArtifactMetadata) = {
//    
//    try {
//    val pattern = "href=\"([^\"]*)\"".r
//    def retrieveYumRepoContents(url: String) = (f: (String => String)) => f(url)
//    def retrieveYumProductReleasesFromUrl = retrieveYumRepoContents(detail.yumRepoUrl)(file => scala.io.Source.fromURL(file).mkString)
//
//    def regexGroupToString(regexMatch: scala.util.matching.Regex.Match): Option[String] = {
//      regexMatch.group(1) match {
//        case token if (token.contains(".rpm")) => Some(token)
//        case _ => None
//      }
//    }
//
//    val allProductRPMs = pattern.findAllMatchIn(retrieveYumProductReleasesFromUrl).flatMap(regexGroupToString _).toList
//
//    val rpmName = allProductRPMs.filter(rpm => (rpm.contains(meta.getRpmName) && rpm.contains(detail.getVersion))).get(0)
//    detail.rpmName = rpmName
//    } catch {
//      case ex: Throwable => println("RPM resolve failed"+ex)
//    }
//
//  }
//}