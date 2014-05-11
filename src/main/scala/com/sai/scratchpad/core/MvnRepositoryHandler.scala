//package com.sai.scratchpad.core
//
//import org.eclipse.aether.resolution.VersionRangeRequest
//import org.eclipse.aether.artifact.DefaultArtifact
//import scala.collection.JavaConversions._
//import org.eclipse.aether.version.Version
//import org.eclipse.aether.collection.CollectRequest
//import org.eclipse.aether.graph.{ Dependency => AetherDependency }
//import org.eclipse.aether.graph.DependencyNode
//import com.sai.pumpkin.domain.ArtifactDetail
//import org.springframework.context.support.ClassPathXmlApplicationContext
//import org.springframework.data.mongodb.core.MongoTemplate
//import org.springframework.data.mongodb.core.query.Query
//import org.springframework.data.mongodb.core.query.Criteria
//import org.springframework.data.domain.Sort
//
//class MvnRepositoryHandler {
//
//  def handle(mavenCoordinates: List[(String, String, String)]): Unit = {
//    val ctx = new ClassPathXmlApplicationContext("appContext.xml")
//    val mongoTemplate = ctx.getBean(classOf[MongoTemplate])
//
//    def findInDB(groupId: String, artifactId: String, version: String, classifier: String): Option[ArtifactDetail] = {
//      val query5 = new Query()
//      query5.addCriteria(Criteria.where("groupId").is(groupId).and("artifactId").is(artifactId).and("version").is(version).and("classifier").is(classifier))
//      val webserviceArtifacts = mongoTemplate.find(query5, classOf[ArtifactDetail])
//      if (webserviceArtifacts.isEmpty) None else Option(webserviceArtifacts.get(0))
//    }
//
//    def save(artifactDetail: ArtifactDetail) = mongoTemplate.save(artifactDetail)
//    val session = Booter.getRepositorySystemSession
//    val system = Booter.getRepositorySystem
//
//    // resolve versions for each coordinates.
//    mavenCoordinates.foreach(c => {
//      val rangeRequest = new VersionRangeRequest
//      println(" aero.sita.gsl.oman:oman-base:[0,)")
//      println(s"${c._1}:${c._2}:[0,)")
//
//      val artifact = new DefaultArtifact(s"${c._1}:${c._2}:[0,)");
//      rangeRequest.setArtifact(artifact);
//      rangeRequest.setRepositories(Booter.getRepositories.toList)
//      val rangeResult = system.resolveVersionRange(session, rangeRequest)
//      val allReleasedMavenArtifactVersions = rangeResult.getVersions().filterNot(version => version.toString.contains("SNAPSHOT")).reverse.toList
//      allReleasedMavenArtifactVersions.take(2).foreach(version => processArtifactVersionAndSave(c._1, c._2, version.toString, "", c._3))
//    })
//
//    def processArtifactVersionAndSave(groupId: String, artifactId: String, version: String, classifier: String, extension: String) = {
//
//      val existingArtifact = findInDB(groupId, artifactId, version, classifier)
//
//      println(existingArtifact + " ------------- ")
//      existingArtifact match {
//        case None => process
//        case _ => println(s"Not processing $groupId, $artifactId, $version, $classifier as it exists in the db")
//      }
//
//      def process = {
//        val rootNode = resolveDependencies(groupId, artifactId, version, classifier, extension)
//
//        rootNode match {
//          case None => println("Not resolving the dependencies for " + rootNode)
//          case _ => processChildren(rootNode)
//        }
//
//        def processChildren(root: Option[DependencyNode]) = {
//          val artifactDetail = convert(root)
//
//          // save the parent artifact
//          save(artifactDetail)
//
//          def collectDependencies(dependency: DependencyNode) = resolveDependencies(dependency.getArtifact().getGroupId(), dependency.getArtifact().getArtifactId(), dependency.getArtifact().getVersion(), dependency.getArtifact().getClassifier(), dependency.getArtifact().getExtension())
//
//          val allDependencies = root.getChildren().map(dependency => convert(collectDependencies _))
//
//          // save the dependencies
//          allDependencies.foreach(save _)
//        }
//      }
//    }
//
//    def resolveDependencies(groupId: String, artifactId: String, version: String, classifier: String, extension: String): Option[DependencyNode] = {
//      val existingArtifact = findInDB(groupId, artifactId, version, classifier)
//      if (existingArtifact == None) {
//        val mavenArtifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version)
//        val collectRequest = new CollectRequest
//        collectRequest.setRoot(new AetherDependency(mavenArtifact, ""))
//        collectRequest.setRepositories(Booter.getRepositories.toList);
//        println(mavenArtifact)
//        Option(system.collectDependencies(session, collectRequest).getRoot())
//      } else {
//        None
//      }
//    }
//
//    def transform(dependencyNode: DependencyNode): ArtifactDetail = {
//      val parent = new ArtifactDetail
//      parent.groupId = dependencyNode.getArtifact().getGroupId()
//      parent.artifactId = dependencyNode.getArtifact().getArtifactId()
//      parent.version = dependencyNode.getArtifact().getVersion()
//      parent.classifier = dependencyNode.getArtifact().getClassifier()
//      parent.artifactType = if (dependencyNode.getArtifact().getArtifactId().contains("ws")) "webservice" else dependencyNode.getArtifact().getExtension()
//      parent.artifactReleaseType = if (dependencyNode.getArtifact().getVersion().toLowerCase().contains("snapshot")) "snapshot" else "release"
//      parent
//    }
//
//    def convert(root: Option[DependencyNode]): Option[ArtifactDetail] = {
//
//      root match {
//        case None => None
//        case _ => {
//          val rootArtifactDetail = transform(root.get)
//          val children = root.get.getChildren().map(transform _)
//          rootArtifactDetail.children ++= children
//          println(rootArtifactDetail.children.size)
//          Some(rootArtifactDetail)
//        }
//      }
//
//    }
//  }
//}