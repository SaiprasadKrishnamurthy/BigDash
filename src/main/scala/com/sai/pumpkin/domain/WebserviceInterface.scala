/*
 * This code contains copyright information which is the proprietary property
 * of SITA Advanced Travel Solutions. No part of this code may be reproduced,
 * stored or transmitted in any form without the prior written permission of
 * SITA Advanced Travel Solutions.
 *
 * Copyright SITA Advanced Travel Solutions 2013
 * All rights reserved.
 */
package com.sai.pumpkin.domain;

import scala.beans.BeanProperty
import java.util.ArrayList
import org.springframework.data.annotation.Id

class WebserviceInterface {
  
  @Id
  @BeanProperty
  var id: String = _

  @BeanProperty
  var groupId: String = _

  @BeanProperty
  var artifactId: String = _

  @BeanProperty
  var version: String = _
  
  @BeanProperty
  var xmlInterfaces: java.util.List[XmlInterface] = new ArrayList
}
