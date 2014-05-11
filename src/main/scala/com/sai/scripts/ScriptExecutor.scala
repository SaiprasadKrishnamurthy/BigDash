package com.sai.scripts

object ScriptExecutor {

  def main(args: Array[String]): Unit = {
    val allRepos = Array("http://10.70.101.19/yum/sita",
      "http://10.70.101.53/yum/sitainf",
      "http://10.70.101.28/yum/product_RELEASES/trunk",
      "http://10.70.101.28/yum/oman_RELEASES/trunk")
      
      println(allRepos)
      
      val result = SshUtil.run("config", "config", "10.70.101.17", "repoquery -a --tree-requires    sitaOmanUserPortal | grep sita")
      println(result)
      
  }

}