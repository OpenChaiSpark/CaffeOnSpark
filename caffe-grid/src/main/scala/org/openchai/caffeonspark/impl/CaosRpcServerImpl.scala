package com.yahoo.ml.caffe.openchai

import java.util.concurrent.ArrayBlockingQueue

import com.yahoo.ml.caffe.{CaffeNetDataSource, CaffeProcessor, OutputType, QueueEntry}
import org.apache.spark.sql.Row
import org.openchai.caffeonspark._
import org.openchai.tcp.rpc.{P2pReq, P2pResp, TcpParams}
import org.openchai.tcp.util.TcpUtils
import Defaults._

class SyncStructC(val nPartitions: Int = 1, ds: CaffeNetDataSource[T1,T2]) extends SyncStruct("blah")
class TrainValidStructC(val sample: QueueEntry) extends SyncStruct("blah")
class SyncRespStructC(val nPartitions: Int = 1) extends SyncRespStruct(1,0,"")
class TrainRespStructC(val outputs: Seq[OutputType]) extends TrainRespStruct(1,0,"")

class CaosServerIfImpl[T1,T2] extends CaosServerIf(q) {
  var processor: CaffeProcessor[T1, T2] = _
  override def sync(struct: SyncStructC): SyncRespStruct = {
    if (processor == null) {
      processor =  CaffeProcessor.instance[T1, T2]()
    if (processor.solversFinished) {
      processor.sync()
    }
      new SyncRespStructC()
//          Iterator(nPartitions)
  }

  override def train(struct: TrainStruct, trainData: TrainingData): TrainRespStruct = {
    val processor: CaffeProcessor[T1, T2] = CaffeProcessor.instance[T1, T2]()
      val res = if (processor.solversFinished)
        Seq.empty[OutputType]
      else {
        processor.synchronized {
          processor.start(null)
          processor.solversFinished = !res
          processor.stopThreads()

          import scala.collection.JavaConversions._
          processor.results.iterator
        }
      }
    new TrainRespStructC(res)
  }

  // TODO: using trainValid for chunked training: later make new method trainBatch
  override def trainValid(struct: TrainValidStructC, trainData: TrainingData, testData: TrainingData): TrainValidRespStruct = {
    processor.feedQueue(0, struct.sample._2)
  }

  override def validation(struct: ValidationStruct, validationData: ValidationData): ValidationRespStruct = super.validation(struct, validationData)

  override def testMode(struct: TestModeStruct): TestModeRespStruct = super.testMode(struct)

  override def shutdown(struct: ShutdownStruct): ShutdownRespStruct = super.shutdown(struct)

  override def service(req: P2pReq[_]): P2pResp[_] = super.service(req)
}

class CaosServerImpl(caosServerIfImpl: CaosServerIfImpl[T1Type, T2Type]) extends CaosServer(q.asInstanceOf[ArrayBlockingQueue[AnyQEntry]],
  QTcpParams, ConTcpParams, XferTcpParams) {

}


class CaosClientImpl extends CaosConClient {

}
