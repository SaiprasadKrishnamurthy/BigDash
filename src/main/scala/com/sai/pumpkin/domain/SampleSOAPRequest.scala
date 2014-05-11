package com.sai.pumpkin.domain

import scala.beans.BeanProperty

class SampleSOAPRequest {

  @BeanProperty
  var operationName: String = _
  
  @BeanProperty
  var contents: String = _
}