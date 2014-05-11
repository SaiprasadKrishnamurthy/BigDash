package com.sai.scripts

import scala.io.Source
import com.sai.scratchpad.core.Booter
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import scala.collection.JavaConversions._

object FindParent {

  def main(args: Array[String]) = {
    val lines = Source.fromFile("c:/data/coordinates.txt").getLines.toList

    val session = Booter.getRepositorySystemSession
    val system = Booter.getRepositorySystem

    lines.foreach(coordinate => {
      val arr = coordinate.split("\\|")
      val gid = arr(0)
      val aid = arr(1)
      val version = arr(2)
      val result = resolveDependencies(gid, aid, version, "", "")
      result match {
        case None => println("Can't find: "+coordinate)
        case _ => result.get.getArtifact().getBaseVersion()
        
      }
    })
  }

  def resolveDependencies(groupId: String, artifactId: String, version: String, extension: String, classifier: String) = {
    val mavenArtifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version)
    try {
      val collectRequest = new CollectRequest
      collectRequest.setRoot(new Dependency(mavenArtifact, ""))
      collectRequest.setRepositories(Booter.getRepositories.toList);
      Some(Booter.getRepositorySystem.collectDependencies(Booter.getRepositorySystemSession, collectRequest).getRoot())
    } catch {
      case ex: Throwable => println(ex); None
    }
  }

}