package com.sai.pumpkin.domain

import org.primefaces.model.SelectableDataModel
import javax.faces.model.ListDataModel

class ConsumerDetailDataModel(data: java.util.List[ConsumerDetail]) extends ListDataModel[ConsumerDetail](data) with SelectableDataModel[ConsumerDetail] {

  
  override def getRowData(rowKey: String) = {
    getWrappedData().asInstanceOf[java.util.ArrayList[ConsumerDetail]].get(0)
  }
  
  override def getRowKey(consumer: ConsumerDetail) = {
    consumer.getName()
  }
}