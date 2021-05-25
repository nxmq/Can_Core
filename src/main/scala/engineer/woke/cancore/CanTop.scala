package engineer.woke.cancore

import chisel3._

class CanTop extends Module with RequireAsyncReset {
  val io = IO(new Bundle {
    val wbClkI : Clock = Input(Clock())
    val wbDatI : UInt = Input(UInt(8.W))
    val wbDatO : UInt = Output(UInt(8.W))
    val wbCycI : Bool = Input(Bool())
    val wbStbI : Bool = Input(Bool())
    val wbWeI : Bool = Input(Bool())
    val wbAddrI : UInt = Input(UInt(8.W))
    val wbAckO : Bool = Output(Bool())
    val canRx : Bool = Input(Bool())
    val canTx : Bool = Output(Bool())
    val busOffOn : Bool = Output(Bool())
    val irqOn : Bool = Output(Bool())
    val clkout : Bool = Output(Bool())
  })
  val dataOutFifo : UInt = Wire(UInt(8.W))
  val dataOutRegs : UInt = Wire(UInt(8.W))

  val resetMode : Bool = Wire(Bool())
  val listenOnlyMode : Bool = Wire(Bool())
  val acceptanceFilterMode : Bool = Wire(Bool())
  val selfTestMode : Bool = Wire(Bool())

  val releaseBuffer : Bool = Wire(Bool())
  val txRequest : Bool = Wire(Bool())
  val abortTx : Bool = Wire(Bool())
  val selfRxRequest : Bool = Wire(Bool())
  val singleShotTransmission : Bool = Wire(Bool())
  val txState : Bool = Wire(Bool())
  val txStateQ : Bool = Wire(Bool())
  val overloadRequest : Bool = Wire(Bool())
  val overloadFrame : Bool = Wire(Bool())

  val readArbitrationLostCaptureReg : Bool = Wire(Bool())

  val readErrorCodeCaptureReg : Bool = Wire(Bool())
  val errorCaptureCode : UInt = Wire(UInt(8.W))

  val baudRatePrescaler : UInt = Wire(UInt(6.W))
  val syncJumpWidth : UInt = Wire(UInt(2.W))
  val timeSegment1 : UInt = Wire(UInt(4.W))
  val timeSegment2 : UInt = Wire(UInt(3.W))
  val tripleSampling : Bool = Wire(Bool())

  val errorWarningLimit : UInt = Wire(UInt(8.W))

  val writeEnReceiveErrorCounter : Bool = Wire(Bool())

  val writeEnTransmitErrorCounter : Bool = Wire(Bool())

  val extendedMode : Bool = Wire(Bool())

  val cs : Bool = Wire(Bool())

  val samplePoint : Bool = Wire(Bool())
  val sampledBit : Bool = Wire(Bool())
  val sampledBitQ : Bool = Wire(Bool())
  val txPoint : Bool = Wire(Bool())
  val hardSync : Bool = Wire(Bool())

  val rxIdle : Bool = Wire(Bool())
  val transmitting : Bool = Wire(Bool())
  val transmitter : Bool = Wire(Bool())
  val goRxInter : Bool = Wire(Bool())
  val rxInter : Bool = Wire(Bool())
  val notFirstBitOfInter : Bool = Wire(Bool())
  val setResetMode : Bool = Wire(Bool())
  val nodeBusOff : Bool = Wire(Bool())
  val errorStatus: Bool = Wire(Bool())
  val rxErrorCount : UInt = Wire(UInt(8.W))
  val txErrorCount : UInt = Wire(UInt(8.W))
  val transmitStatus : Bool = Wire(Bool())
  val receiveStatus : Bool = Wire(Bool())
  val txSuccessful : Bool = Wire(Bool())
  val needToTx : Bool = Wire(Bool())
  val overrun : Bool = Wire(Bool())
  val infoEmpty : Bool = Wire(Bool())
  val setBusErrorIrq : Bool = Wire(Bool())
  val setArbitrationLostIrq : Bool = Wire(Bool())
  val arbitrationLostCapture : UInt = Wire(UInt(5.W))
  val nodeErrorPassive : Bool = Wire(Bool())
  val nodeErrorActive : Bool = Wire(Bool())
  val rxMessageCounter : UInt = Wire(UInt(7.W))
  val txNext : Bool = Wire(Bool())

  val goOverloadFrame : Bool = Wire(Bool())
  val goErrorFrame : Bool = Wire(Bool())
  val goTx : Bool = Wire(Bool())
  val sendAck : Bool = Wire(Bool())
  val writeEn : Bool = Wire(Bool())
  val addr : UInt = Wire(UInt(8.W))
  val dataIn : UInt = Wire(UInt(8.W))
  val dataOut : UInt = Reg(UInt(8.W))
  val rxSyncTmp : Bool = RegNext(io.canRx,true.B)
  val rxSync : Bool = RegNext(rxSyncTmp,true.B)

  val canRegisters : CanRegisters = Module(new CanRegisters())
  canRegisters.io.cs := cs
  canRegisters.io.writeEn := writeEn
  canRegisters.io.addr := addr
  canRegisters.io.dataIn := dataIn
  dataOutRegs := canRegisters.io.dataOut
  io.irqOn := canRegisters.io.irqN
  canRegisters.io.samplePoint := samplePoint
  canRegisters.io.transmitting := transmitting
  canRegisters.io.setResetMode := setResetMode
  canRegisters.io.nodeBusOff := nodeBusOff
  canRegisters.io.errorStatus := errorStatus
  canRegisters.io.rxErrorCount := rxErrorCount
  canRegisters.io.txErrorCount := txErrorCount
  canRegisters.io.transmitStatus := transmitStatus
  canRegisters.io.receiveStatus := receiveStatus
  canRegisters.io.needToTx := needToTx
  canRegisters.io.overrun := overrun
  canRegisters.io.infoEmpty := infoEmpty
  canRegisters.io.setBusErrorIrq := setBusErrorIrq
  canRegisters.io.setArbitrationLostIrq := setArbitrationLostIrq
  canRegisters.io.arbitrationLostCapture := arbitrationLostCapture
  canRegisters.io.nodeErrorPassive := nodeErrorPassive
  canRegisters.io.nodeErrorActive := nodeErrorActive
  canRegisters.io.rxMessageCounter := rxMessageCounter

  resetMode := canRegisters.io.resetMode
  listenOnlyMode := canRegisters.io.listenOnlyMode
  acceptanceFilterMode := canRegisters.io.acceptanceFilterMode
  selfTestMode := canRegisters.io.selfTestMode
  releaseBuffer := canRegisters.io.releaseBuffer
  abortTx := canRegisters.io.abortTx
  txRequest := canRegisters.io.txRequest
  selfRxRequest := canRegisters.io.selfRxRequest
  singleShotTransmission := canRegisters.io.singleShotTransmission
  canRegisters.io.txState := txState
  canRegisters.io.txStateQ := txStateQ
  canRegisters.io.txSuccessful := txSuccessful
  overloadRequest := canRegisters.io.overloadRequest
  canRegisters.io.overloadFrame := overloadFrame

  readArbitrationLostCaptureReg := canRegisters.io.readArbitrationLostCaptureReg

  readErrorCodeCaptureReg := canRegisters.io.readErrorCodeCaptureReg
  canRegisters.io.errorCaptureCode := errorCaptureCode

  baudRatePrescaler := canRegisters.io.baudRatePrescaler
  syncJumpWidth := canRegisters.io.syncJumpWidth

  timeSegment1 := canRegisters.io.timeSegment1
  timeSegment2 := canRegisters.io.timeSegment2
  tripleSampling := canRegisters.io.tripleSampling

  errorWarningLimit := canRegisters.io.errorWarningLimit
  writeEnReceiveErrorCounter := canRegisters.io.writeEnReceiveErrorCounter
  writeEnTransmitErrorCounter := canRegisters.io.writeEnTransmitErrorCounter

  extendedMode := canRegisters.io.extendedMode
  io.clkout := canRegisters.io.clkout

  val canBtl : CanBtl = Module(new CanBtl())
  canBtl.io.rx := rxSync
  canBtl.io.tx := io.canTx

  canBtl.io.baudRatePrescaler := baudRatePrescaler
  canBtl.io.syncJumpWidth := syncJumpWidth

  canBtl.io.timeSegment1 := timeSegment1
  canBtl.io.timeSegment2 := timeSegment2
  canBtl.io.tripleSampling := tripleSampling

  samplePoint := canBtl.io.samplePoint
  sampledBit := canBtl.io.sampledBit
  sampledBitQ := canBtl.io.sampledBitQ
  txPoint := canBtl.io.txPoint
  hardSync := canBtl.io.hardSync

  canBtl.io.rxIdle := rxIdle
  canBtl.io.rxInter := rxInter
  canBtl.io.transmitting := transmitting
  canBtl.io.transmitter := transmitter
  canBtl.io.goRxInter := goRxInter
  canBtl.io.txNext := txNext
  canBtl.io.goOverloadFrame := goOverloadFrame
  canBtl.io.goErrorFrame := goErrorFrame
  canBtl.io.goTx := goTx
  canBtl.io.sendAck := sendAck
  canBtl.io.nodeErrorPassive := nodeErrorPassive

  val canBsp : CanBsp = Module(new CanBsp())
  canBsp.io.samplePoint := canBtl.io.samplePoint
  canBsp.io.sampledBit := canBtl.io.sampledBit
  canBsp.io.sampledBitQ := canBtl.io.sampledBitQ
  canBsp.io.txPoint := canBtl.io.txPoint
  canBsp.io.hardSync := canBtl.io.hardSync
  canBsp.io.addr := addr
  canBsp.io.dataIn := dataIn
  dataOutFifo := canBsp.io.dataOut
  canBsp.io.resetMode := resetMode
  canBsp.io.listenOnlyMode := listenOnlyMode
  canBsp.io.acceptanceFilterMode := acceptanceFilterMode
  canBsp.io.selfTestMode := selfTestMode
  canBsp.io.releaseBuffer := canRegisters.io.releaseBuffer
  canBsp.io.abortTx := canRegisters.io.abortTx
  canBsp.io.txRequest := canRegisters.io.txRequest
  canBsp.io.selfRxRequest := canRegisters.io.selfRxRequest
  canBsp.io.singleShotTransmission := canRegisters.io.singleShotTransmission
  txState := canBsp.io.txState
  txStateQ := canBsp.io.txStateQ
  canBsp.io.overloadRequest := canRegisters.io.overloadRequest
  overloadFrame := canBsp.io.overloadFrame

  canBsp.io.readArbitrationLostCaptureReg := canRegisters.io.readArbitrationLostCaptureReg

  canBsp.io.readErrorCodeCaptureReg := canRegisters.io.readErrorCodeCaptureReg
  errorCaptureCode :=  canBsp.io.errorCaptureCode

  canBsp.io.errorWarningLimit := errorWarningLimit
  canBsp.io.writeEnReceiveErrorCounter := canRegisters.io.writeEnReceiveErrorCounter
  canBsp.io.writeEnTransmitErrorCounter := canRegisters.io.writeEnTransmitErrorCounter

  canBsp.io.extendedMode := canRegisters.io.extendedMode
  rxIdle := canBsp.io.rxIdle
  transmitting := canBsp.io.transmitting
  goRxInter := canBsp.io.goRxInter
  notFirstBitOfInter := canBsp.io.notFirstBitOfInter
  rxInter := canBsp.io.rxInter
  setResetMode := canBsp.io.setResetMode
  nodeBusOff := canBsp.io.nodeBusOff
  errorStatus := canBsp.io.errorStatus
  rxErrorCount := canBsp.io.rxErrorCount(7,0)
  txErrorCount := canBsp.io.txErrorCount(7,0)
  transmitStatus := canBsp.io.transmitStatus
  receiveStatus := canBsp.io.receiveStatus
  txSuccessful := canBsp.io.txSuccessful
  needToTx := canBsp.io.needToTx
  overrun := canBsp.io.overrun
  infoEmpty := canBsp.io.infoEmpty
  setBusErrorIrq := canBsp.io.setBusErrorIrq
  setArbitrationLostIrq := canBsp.io.setArbitrationLostIrq
  arbitrationLostCapture := canBsp.io.arbitrationLostCapture
  nodeErrorPassive := canBsp.io.nodeErrorPassive
  nodeErrorActive := canBsp.io.nodeErrorActive
  rxMessageCounter := canBsp.io.rxMessageCounter
  io.canTx := canBsp.io.tx
  txNext := canBsp.io.txNext
  io.busOffOn := canBsp.io.busOffOn
  goOverloadFrame := canBsp.io.goOverloadFrame
  goErrorFrame := canBsp.io.goErrorFrame
  goTx := canBsp.io.goTx
  sendAck := canBsp.io.sendAck
  transmitter := canBsp.io.transmitter
  transmitting := canBsp.io.transmitting
  txSuccessful := canBsp.io.txSuccessful

  canBsp.io.acceptanceCode := canRegisters.io.acceptanceCode
  canBsp.io.acceptanceMask := canRegisters.io.acceptanceMask

  canBsp.io.txData := canRegisters.io.txData

  val dataOutFifoSelected : Bool = WireDefault(extendedMode & (!resetMode) & ((addr >= 16.U) && (addr <= 28.U)) | (!extendedMode) & ((addr >= 20.U) && (addr <= 29.U)))

  when(cs & !writeEn) {
      dataOut := Mux(dataOutFifoSelected,dataOutFifo,dataOutRegs)
  }

  var cs_sync1: Bool = RegInit(false.B)
  val cs_sync2: Bool = RegNext(cs_sync1,false.B)
  val cs_sync3: Bool = RegNext(cs_sync2,false.B)

  val cs_ack1: Bool = withClock(io.wbClkI) { RegNext(cs_sync3,false.B) }
  val cs_ack2: Bool = withClock(io.wbClkI) { RegNext(cs_ack1,false.B) }
  val cs_ack3: Bool = withClock(io.wbClkI) { RegNext(cs_ack2,false.B) }

  val cs_sync_rst1: Bool = RegNext(cs_ack3,false.B)
  val cs_sync_rst2: Bool = RegNext(cs_sync_rst1,false.B)
  cs_sync1 := io.wbCycI & io.wbStbI & !cs_sync_rst2
  cs := cs_sync2 & (~cs_sync3)

  val wbAckO = withClock(io.wbClkI) {
    RegNext(cs_ack2 & (~cs_ack3),false.B)
  }

  io.wbAckO := wbAckO
  writeEn := io.wbWeI
  addr := io.wbAddrI
  dataIn := io.wbDatI
  io.wbDatO := dataOut
}

