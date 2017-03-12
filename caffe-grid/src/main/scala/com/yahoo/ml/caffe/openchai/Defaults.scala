package com.yahoo.ml.caffe.openchai

import java.util.concurrent.ArrayBlockingQueue

import com.yahoo.ml.caffe.{CaffeProcessor, QueueEntry}
import org.apache.spark.sql.Row
import org.openchai.caffeonspark._
import org.openchai.tcp.rpc.{P2pReq, P2pResp, TcpParams}
import org.openchai.tcp.util.TcpUtils

object Defaults {
  type T1Type = Array[String]
  type T2Type = Array[Float]
  val ConPort = 8900
  val XferPort = 8901
  val QPort = 8905

  val HostName = TcpUtils.getLocalHostname
  val ConTcpParams = TcpParams(HostName, ConPort)
  val XferTcpParams = TcpParams(HostName, XferPort)
  val QTcpParams = TcpParams(HostName, QPort)

  val QSize = 1000
  val q = new ArrayBlockingQueue[QueueEntry](QSize)
}

import Defaults._

class CaosConServerImpl extends CaosServerIf(q) {
  override def sync(struct: SyncStruct): SyncRespStruct =

  override def train(struct: TrainStruct, trainData: TrainingData): TrainRespStruct = {
    val processor: CaffeProcessor[T1, T2] = CaffeProcessor.instance[T1, T2]()
      if (processor.solversFinished)
        Iterator()
      else {
        processor.synchronized {
          processor.start(null)
          val res = iter.map { sample => processor.feedQueue(0, sample) }.reduce(_ && _)
          processor.solversFinished = !res
          processor.stopThreads()

          import scala.collection.JavaConversions._
          processor.results.iterator
        }
      }
  }

  override def trainValid(struct: TrainValidStruct, trainData: TrainingData, testData: TrainingData): TrainValidRespStruct = super.trainValid(struct, trainData, testData)

  override def validation(struct: ValidationStruct, validationData: ValidationData): ValidationRespStruct = super.validation(struct, validationData)

  override def testMode(struct: TestModeStruct): TestModeRespStruct = super.testMode(struct)

  override def shutdown(struct: ShutdownStruct): ShutdownRespStruct = super.shutdown(struct)

  override def service(req: P2pReq[_]): P2pResp[_] = super.service(req)
}
class CaosServerImpl extends CaosServer(q.asInstanceOf[ArrayBlockingQueue[AnyQEntry]],
  QTcpParams, ConTcpParams, XferTcpParams) {


}


class CaosClientImpl extends CaosConClient {

}
