package com.sai.pumpkin.rest

import org.springframework.stereotype.Controller
import com.sai.pumpkin.service.ArtifactDetailService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RequestMethod

@Controller
class ArtifactResource {

  @Autowired
  var artifactService: ArtifactDetailService = _

  @RequestMapping(value = Array("/allartifacts"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  @ResponseBody
  def all() = artifactService.findAll

  @RequestMapping(value = Array("/allartifactsxml"), method = Array(RequestMethod.GET), produces = Array("application/xml"))
  @ResponseBody
  def allxml() = artifactService.findAll
}