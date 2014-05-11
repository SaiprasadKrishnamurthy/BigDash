package com.sai.pumpkin.domain

import org.primefaces.model.SelectableDataModel
import javax.faces.model.ListDataModel

class ArtifactDetailDataModel(data: java.util.List[ArtifactDetail]) extends ListDataModel[ArtifactDetail](data) with SelectableDataModel[ArtifactDetail] {

  
  override def getRowData(rowKey: String) = {
    println(rowKey+" : rowkey")
    getWrappedData().asInstanceOf[java.util.ArrayList[ArtifactDetail]].get(0)
  }
  
  override def getRowKey(consumer: ArtifactDetail) = {
    consumer.groupId+consumer.artifactId+consumer.version
  }
}