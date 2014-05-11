package com.sai.pumpkin.managedbeans

import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import com.sai.pumpkin.domain.ArtifactMetadata
import org.primefaces.context.RequestContext
import javax.faces.application.FacesMessage
import scala.beans.BeanProperty
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils
import javax.faces.context.FacesContext
import javax.servlet.ServletContext

class AdminController {

  @BeanProperty
  var meta = new ArtifactMetadata
  
  @BeanProperty
  var groupId: String = _

  @BeanProperty
  var artifactId: String = _

  @BeanProperty
  var packaging: String = _

  
  val servletContext = FacesContext.getCurrentInstance().getExternalContext().getContext().asInstanceOf[ServletContext]
  val appContext = WebApplicationContextUtils.getWebApplicationContext(servletContext)
  val mongoTemplate = appContext.getBean(classOf[MongoTemplate])
  
  def save() = {
    meta.svnLocation = "#{gsl.repo}/"+meta.svnLocation
    meta.goBackUpto = 15
    mongoTemplate.save(meta)
    RequestContext.getCurrentInstance().showMessageInDialog(new FacesMessage("Success", "Saved"))
  }
}