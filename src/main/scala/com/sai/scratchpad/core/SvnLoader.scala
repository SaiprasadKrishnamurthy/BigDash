package com.sai.scratchpad.core

import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.SVNURL
import scala.collection.JavaConversions._
import org.tmatesoft.svn.core.SVNLogEntry

object SvnLoader {

  def main(args: Array[String]) = {

    DAVRepositoryFactory.setup();

    SVNRepositoryFactoryImpl.setup();

    val cm = SVNClientManager.newInstance()
    val dc = cm.getDiffClient()
    val lc = cm.getLogClient()

    //    val artifactVersion = "1.0-SNAPSHOT"

    //    val authManager = new BasicAuthenticationManager(
    //      "saiprasad.krishnamurthy", "Saiprasad1$");

    val authManager = new BasicAuthenticationManager("skrishnamurthy", "002632");

    val repository = SVNRepositoryFactory.create(SVNURL
      .parseURIEncoded("http://10.70.101.200:7777/svn/gsl/Components/Profiler/Presentation-WS/tags/profiler-ws-presentation-25.5"));
    repository.setAuthenticationManager(authManager);

    println(repository)

    val entries = repository.log(Array(""), null, -1,
      0, true, true)

      entries.foreach(e => {
        val en = e.asInstanceOf[SVNLogEntry]
        println(en.getRevision)
      })
  }

}