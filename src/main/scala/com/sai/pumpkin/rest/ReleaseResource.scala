package com.sai.pumpkin.rest

import org.springframework.stereotype.Controller
import com.sai.pumpkin.service.ArtifactDetailService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RequestMethod
import com.sai.pumpkin.service.ReleaseService

@Controller
class ReleaseResource {

  @Autowired
  var releaseService: ReleaseService = _

  @RequestMapping(value = Array("/allreleases"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  @ResponseBody
  def all() = releaseService.findAll
}