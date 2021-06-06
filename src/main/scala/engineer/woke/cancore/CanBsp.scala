package engineer.woke.cancore

import chisel3.util._
import chisel3._

//VERIFIES MOSTLY CORRECT?!?! OVERRUN DOESNT MATCH THO, EVEN THOUGH CANFIFO ITSELF IS FINE
//WTF IS THE ISSUE
class CanBsp extends Module with RequireAsyncReset {
  val io = IO(new Bundle {
    val samplePoint : Bool = Input(Bool())
    val sampledBit : Bool =  Input(Bool())
    val sampledBitQ : Bool = Input(Bool())
    val txPoint : Bool = Input(Bool())
    val hardSync : Bool = Input(Bool())
    val addr : UInt = Input(UInt(8.W))
    val dataIn : UInt = Input(UInt(8.W))
    val dataOut : UInt = Output(UInt(8.W))
    val resetMode : Bool = Input(Bool())
    val listenOnlyMode : Bool = Input(Bool())
    val acceptanceFilterMode : Bool = Input(Bool())
    val extendedMode : Bool = Input(Bool())
    val selfTestMode : Bool = Input(Bool())
    val releaseBuffer : Bool = Input(Bool())
    val txRequest : Bool = Input(Bool())
    val abortTx : Bool = Input(Bool())
    val selfRxRequest : Bool = Input(Bool())
    val singleShotTransmission : Bool = Input(Bool())
    val txState : Bool = Output(Bool())
    val txStateQ : Bool = Output(Bool())
    val overloadRequest : Bool = Input(Bool())
    val overloadFrame : Bool = Output(Bool())
    val readArbitrationLostCaptureReg : Bool = Input(Bool())
    val readErrorCodeCaptureReg : Bool = Input(Bool())
    val errorCaptureCode : UInt = Output(UInt(8.W))
    val errorWarningLimit: UInt = Input(UInt(8.W))
    val writeEnReceiveErrorCounter : Bool = Input(Bool())
    val writeEnTransmitErrorCounter : Bool = Input(Bool())
    val rxIdle : Bool = Output(Bool())
    val transmitting : Bool = Output(Bool())
    val transmitter : Bool = Output(Bool())
    val goRxInter : Bool = Output(Bool())
    val notFirstBitOfInter : Bool = Output(Bool())
    val rxInter : Bool = Output(Bool())
    val setResetMode : Bool = Output(Bool())
    val nodeBusOff : Bool = Output(Bool())
    val errorStatus : Bool = Output(Bool())
    val rxErrorCount : UInt = Output(UInt(9.W))
    val txErrorCount : UInt = Output(UInt(9.W))
    val transmitStatus : Bool = Output(Bool())
    val receiveStatus : Bool = Output(Bool())
    val txSuccessful : Bool = Output(Bool())
    val needToTx : Bool = Output(Bool())
    val overrun : Bool = Output(Bool())
    val infoEmpty : Bool = Output(Bool())
    val setBusErrorIrq : Bool = Output(Bool())
    val setArbitrationLostIrq : Bool = Output(Bool())
    val arbitrationLostCapture : UInt = Output(UInt(5.W))
    val nodeErrorPassive : Bool = Output(Bool())
    val nodeErrorActive : Bool = Output(Bool())
    val rxMessageCounter : UInt = Output(UInt(7.W))
    val acceptanceCode : Vec[UInt] = Input(Vec(4,UInt(8.W)))
    val acceptanceMask : Vec[UInt] = Input(Vec(4,UInt(8.W)))
    val txData : Vec[UInt] = Input(Vec(13,UInt(8.W)))
    val tx : Bool = Output(Bool())
    val txNext : Bool = Output(Bool())
    val busOffOn : Bool = Output(Bool())
    val goOverloadFrame : Bool = Output(Bool())
    val goErrorFrame : Bool = Output(Bool())
    val goTx : Bool = Output(Bool())
    val sendAck : Bool = Output(Bool())
  })

  val txState : Bool = RegInit(false.B)
  io.txState := txState
  val txStateQ : Bool = RegNext(Mux(io.resetMode,false.B,txState), false.B)
  io.txStateQ := txStateQ
  val overloadFrame : Bool = RegInit(false.B)
  io.overloadFrame := overloadFrame
  val errorCaptureCode : UInt = RegInit(0.U(8.W))
  io.errorCaptureCode := errorCaptureCode
  val rxIdle : Bool = RegInit(false.B)
  io.rxIdle := rxIdle
  val transmitting : Bool = RegInit(false.B)
  io.transmitting := transmitting
  val transmitter : Bool = RegInit(false.B)
  io.transmitter := transmitter
  val rxInter : Bool = RegInit(false.B)
  io.rxInter := rxInter
  val nodeBusOff : Bool = RegInit(false.B)
  io.nodeBusOff := nodeBusOff
  val needToTx : Bool = RegInit(false.B)
  io.needToTx := needToTx
  val rxErrorCount : UInt = RegInit(0.U(9.W))
  io.rxErrorCount := rxErrorCount
  val txErrorCount : UInt = RegInit(0.U(9.W))
  io.txErrorCount := txErrorCount
  val arbitrationLostCapture : UInt = RegInit(0.U(5.W))
  io.arbitrationLostCapture := arbitrationLostCapture
  val nodeErrorPassive : Bool = RegInit(false.B)
  io.nodeErrorPassive := nodeErrorPassive
  val tx : Bool = RegInit(true.B)
  io.tx := tx
  val resetModeQ : Bool = RegNext(io.resetMode,false.B)
  val bitCnt : UInt = RegInit(0.U(6.W))
  val dataLen : UInt = RegInit(0.U(4.W))
  val id : UInt = RegInit(0.U(29.W))
  val bitStuffCnt : UInt = RegInit(1.U(3.W))
  val bitStuffCntTx : UInt = RegInit(1.U(3.W))
  val txPointQ : Bool = RegNext(Mux(io.resetMode,false.B,io.txPoint), false.B)
  val rxId1 : Bool = RegInit(false.B)
  val rxRtr1 : Bool = RegInit(false.B)
  val rxIde : Bool = RegInit(false.B)
  val rxId2 : Bool = RegInit(false.B)
  val rxRtr2 : Bool = RegInit(false.B)
  val rxR0 : Bool = RegInit(false.B)
  val rxR1: Bool = RegInit(false.B)
  val rxDlc : Bool = RegInit(false.B)
  val rxData : Bool = RegInit(false.B)
  val rxCrc : Bool = RegInit(false.B)
  val rxCrcLim : Bool = RegInit(false.B)
  val rxAck : Bool = RegInit(false.B)
  val rxAckLim : Bool = RegInit(false.B)
  val rxEof : Bool = RegInit(false.B)
  val goEarlyTxLatched : Bool = RegInit(false.B)
  val rtr1 : Bool = RegInit(false.B)
  val ide : Bool = RegInit(false.B)
  val rtr2 : Bool = RegInit(false.B)
  val crcIn : UInt = RegInit(0.U(15.W))
  val tmpData : UInt = RegInit(0.U(8.W))
  val tmpFifo : Mem[UInt] = Mem(8,UInt(8.W))
  val writeDataToTmpFifo : Bool = RegInit(false.B)
  val byteCnt : UInt = RegInit(0.U(3.W))
  val bitStuffCntEn : Bool = RegInit(false.B)
  val crcEnable : Bool = RegInit(false.B)
  val eofCnt : UInt = RegInit(0.U(3.W))
  val passiveCnt : UInt = RegInit(1.U(3.W))
  val errorFrame : Bool = RegInit(false.B)
  val enableErrorCnt2 : Bool = RegInit(false.B)
  val errorCnt1 : UInt = RegInit(0.U(3.W))
  val errorCnt2 : UInt = RegInit(0.U(3.W))
  val delayedDominantCnt : UInt = RegInit(0.U(3.W))
  val enableOverloadCnt2 : Bool = RegInit(false.B)
  val overloadFrameBlocked : Bool = RegInit(false.B)
  val overloadRequestCnt : UInt = RegInit(0.U(2.W))
  val overloadCnt1 : UInt = RegInit(0.U(3.W))
  val overloadCnt2 : UInt = RegInit(0.U(3.W))
  val crcErr : Bool = RegInit(false.B)
  val arbitrationLost : Bool = RegInit(false.B)
  val arbitrationLostQ : Bool = RegNext(arbitrationLost,false.B)
  val arbitrationFieldD : Bool = RegInit(false.B)
  val arbitrationCnt : UInt = RegInit(0.U(5.W))
  val arbitrationBlocked : Bool = RegInit(false.B)
  val txQ : Bool = RegInit(false.B)
  val dataCnt : UInt = RegInit(0.U(4.W))
  val headerCnt : UInt = RegInit(0.U(3.W))
  val wrFifo : Bool = RegInit(false.B)
  val dataForFifo : UInt = Wire(UInt(8.W))
  val txPointer : UInt = RegInit(0.U(6.W))
  val txBit : Bool = Wire(Bool())
  val finishMsg : Bool = RegInit(false.B)
  val busFreeCnt : UInt = RegInit(0.U(4.W))
  val busFreeCntEn : Bool = RegInit(false.B)
  val waitingForBusFree : Bool = RegInit(true.B)
  val busFree : Bool = RegNext(io.samplePoint & io.sampledBit & (busFreeCnt === 10.U) & waitingForBusFree,false.B)
  val nodeBusOffQ : Bool = RegNext(io.nodeBusOff,false.B)
  val ackErrLatched : Bool = RegInit(false.B)
  val bitErrLatched : Bool = RegInit(false.B)
  val stuffErrLatched : Bool = RegInit(false.B)
  val formErrLatched : Bool = RegInit(false.B)
  val rule3Exc1 : Vec[Bool] = RegInit(VecInit(Seq.fill(2)(0.B)))
  val suspend : Bool = RegInit(false.B)
  val suspendCntEn : Bool = RegInit(false.B)
  val suspendCnt : UInt = RegInit(0.U(3.W))
  val errorFlagOverLatched : Bool = RegInit(false.B)
  val errorCaptureCodeType : UInt = Wire(UInt(2.W))
  val errorCaptureCodeBlocked : Bool = RegInit(false.B)
  val firstCompareBit : Bool = RegInit(false.B)

  val errorCaptureCodeSegment : UInt = Wire(UInt(5.W))
  val errorCaptureCodeDirection : Bool = !io.transmitting

  val bitDeStuff : Bool = bitStuffCnt === 5.U
  val bitDeStuffTx : Bool = bitStuffCntTx === 5.U

  val rule5 : Bool = Wire(Bool())
  val lastBitOfInter : Bool = rxInter & (bitCnt(1,0) === 2.U)

  val goRxIdle : Bool = io.samplePoint & io.sampledBit & lastBitOfInter | busFree & !io.nodeBusOff
  val goRxId1 : Bool = io.samplePoint & !io.sampledBit & (io.rxIdle | lastBitOfInter)
  val goRxRtr1 : Bool = (!bitDeStuff) & io.samplePoint & rxId1 & (bitCnt(3,0) === 10.U)
  val goRxIde : Bool = (!bitDeStuff) & io.samplePoint & rxRtr1
  val goRxId2 : Bool = (!bitDeStuff) & io.samplePoint & rxIde & io.sampledBit
  val goRxRtr2 : Bool = (!bitDeStuff) & io.samplePoint & rxId2 & (bitCnt(4,0) === 17.U)
  val goRxR1 : Bool = (!bitDeStuff) & io.samplePoint &  rxRtr2
  val goRxR0 : Bool = (!bitDeStuff) & io.samplePoint & (rxIde & (!io.sampledBit) | rxR1)
  val goRxDlc : Bool = (!bitDeStuff) & io.samplePoint & rxR0
  val goRxData : Bool = Wire(Bool())
  val goRxCrc : Bool = Wire(Bool())
  val goRxCrcLim : Bool = (!bitDeStuff) & io.samplePoint & rxCrc & (bitCnt(3,0) === 14.U)
  val goRxAck : Bool = (!bitDeStuff) & io.samplePoint & rxCrcLim
  val goRxAckLim : Bool = io.samplePoint & rxAck
  val goRxEof : Bool = io.samplePoint & rxAckLim
  val remoteRq : Bool = ((!ide) & rtr1) | (ide & rtr2)

  val goCrcEnable : Bool = io.hardSync | io.goTx
  val rstCrcEnable : Bool = goRxCrc

  val bitDeStuffSet : Bool = goRxId1 & (!io.goErrorFrame)
  val bitDeStuffReset : Bool = goRxAck | io.goErrorFrame | io.goOverloadFrame

  val goEarlyTx : Bool = (!io.listenOnlyMode) & io.needToTx & (!io.txState) & (!suspend | (suspendCnt === 7.U)) & io.samplePoint & (!io.sampledBit) & (io.rxIdle | lastBitOfInter)

  val calculatedCrc : UInt = Wire(UInt(15.W))
  val rCalculatedCrc : UInt = Reverse(calculatedCrc)
  val limitedDataLen : UInt = Mux(dataLen < 8.U, dataLen , 8.U)(3,0)
  val formErr : Bool = io.samplePoint & (((!bitDeStuff) & rxCrcLim & (!io.sampledBit)) |
    (rxAckLim & (!io.sampledBit)) | ((eofCnt < 6.U)& rxEof & (!io.sampledBit) & (!io.transmitter)) | (rxEof & (!io.sampledBit) & io.transmitter))

  val bitErrCompGoRxCrc : Bool = bitCnt(5,0) === ((limitedDataLen(2,0)<<3).asUInt() - 1.U)
  goRxData := (!bitDeStuff) & io.samplePoint & rxDlc & (bitCnt(1,0) === 3.U) & (io.sampledBit | dataLen(2,0).orR()) & (!remoteRq)
  goRxCrc := (!bitDeStuff) & io.samplePoint & (rxDlc & (bitCnt(1,0) === 3.U) & ((!io.sampledBit) & (!dataLen(2,0).orR()) | remoteRq) | rxData & bitErrCompGoRxCrc)

  val errorFrameEnded : Bool = (errorCnt2 === 7.U) & io.txPoint
  val overloadFrameEnded : Bool = (overloadCnt2 === 7.U) & io.txPoint
  val bitErr : Bool = Wire(Bool())
  val ackErr : Bool = rxAck & io.samplePoint & io.sampledBit & io.txState & (!io.selfTestMode)
  val stuffErr : Bool = io.samplePoint & bitStuffCntEn & bitDeStuff & (io.sampledBit === io.sampledBitQ)

  io.goRxInter := ((io.samplePoint &  rxEof  & (eofCnt === 6.U)) | errorFrameEnded | overloadFrameEnded) & (!io.overloadRequest)

  val idOk : Bool = Wire(Bool())
  val noByte0 : Bool = rtr1 | (dataLen < 1.U)
  val noByte1 : Bool = rtr1 | (dataLen < 2.U)

  val headerLen : UInt = Mux(io.extendedMode, Mux(ide,5.U,3.U),2.U)
  val storingHeader : Bool = headerCnt < headerLen
  val limitedDataLenSubOne : UInt = Mux(remoteRq, 0xf.U, Mux(dataLen < 8.U, dataLen - 1.U, 7.U))
  val resetWrFifo : Bool = dataCnt === (limitedDataLenSubOne + headerLen) | io.resetMode
  val err : Bool = formErr | stuffErr | bitErr | ackErr | formErrLatched | stuffErrLatched | bitErrLatched | ackErrLatched | crcErr

  val arbitrationField : Bool = rxId1 | rxRtr1 | rxIde | rxId2 | rxRtr2

  val rTxData : Vec[UInt] = VecInit(io.txData.map {
    v => Reverse(v)
  })


  val basicChain : UInt = Cat(rTxData(1)(7,4), 0.U(2.W), rTxData(1)(3,0), rTxData(0), 0.U(1.W))
  val basicChainData : UInt = Cat(rTxData(9), rTxData(8), rTxData(7), rTxData(6), rTxData(5), rTxData(4), rTxData(3), rTxData(2))
  val extendedChainStd : UInt = Cat(rTxData(0)(7,4), 0.U(2.W), rTxData(0)(1), rTxData(2)(2,0), rTxData(1)(7,0), 0.U(1.W))
  val extendedChainExt : UInt = Cat(rTxData(0)(7,4), 0.U(2.W), rTxData(0)(1), rTxData(4)(4,0), rTxData(3)(7,0), rTxData(2)(7,3), 3.U(2.W), rTxData(2)(2,0), rTxData(1)(7,0), 0.U(1.W))
  val extendedChainDataStd : UInt = Cat(rTxData(10), rTxData(9), rTxData(8), rTxData(7), rTxData(6), rTxData(5), rTxData(4), rTxData(3))
  val extendedChainDataExt : UInt = Cat(rTxData(12), rTxData(11), rTxData(10), rTxData(9), rTxData(8), rTxData(7), rTxData(6), rTxData(5))

  io.sendAck := (!io.txState) & rxAck & (!err) & (!io.listenOnlyMode)
  val bitErrExc1 : Bool = io.txState & arbitrationField & io.tx
  val bitErrExc2 : Bool = rxAck & io.tx
  val bitErrExc3 : Bool = errorFrame & io.nodeErrorPassive & (errorCnt1 < 7.U)
  val bitErrExc4 : Bool = (errorFrame & (errorCnt1 === 7.U) & (!enableErrorCnt2)) | (io.overloadFrame & (overloadCnt1 === 7.U) & (!enableOverloadCnt2))
  val bitErrExc5 : Bool = (errorFrame & (errorCnt2 === 7.U)) | (io.overloadFrame & (overloadCnt2 === 7.U))
  val bitErrExc6 : Bool = (eofCnt === 6.U) & rxEof & (!io.transmitter)
  val errorFlagOver : Bool = ((!io.nodeErrorPassive) & io.samplePoint & (errorCnt1 === 7.U) | io.nodeErrorPassive & io.samplePoint & (passiveCnt === 6.U)) & (!enableErrorCnt2)
  val overloadFlagOver : Bool = io.samplePoint & (overloadCnt1 === 7.U) & (!enableOverloadCnt2)
  bitErr := (io.txState | errorFrame | io.overloadFrame | rxAck) & io.samplePoint & (io.tx =/= io.sampledBit) & (!bitErrExc1) & (!bitErrExc2) & (!bitErrExc3) & (!bitErrExc4) & (!bitErrExc5) & (!bitErrExc6) & (!io.resetMode)
  rule5 := bitErr &     ((!io.nodeErrorPassive) & errorFrame & (errorCnt1 < 7.U) | io.overloadFrame & (overloadCnt1 < 7.U))
  val limitedTxCntExt : UInt = Mux(io.txData(0)(3), 0x3f.U, (io.txData(0)(2,0) << 3).asUInt() - 1.U)
  val limitedTxCntStd : UInt = Mux(io.txData(1)(3), 0x3f.U, (io.txData(1)(2,0) << 3).asUInt() - 1.U)

  val rstTxPointer : Bool = ((!bitDeStuffTx) & io.txPoint & (!rxData) &   io.extendedMode &  rTxData(0)(0) & txPointer === 38.U) |   // arbitration + control for extended format
                            ((!bitDeStuffTx) & io.txPoint & (!rxData) &   io.extendedMode & !rTxData(0)(0) & txPointer === 18.U) |   // arbitration + control for extended format
                            ((!bitDeStuffTx) & io.txPoint & (!rxData) &  !io.extendedMode                & txPointer === 18.U) |     // arbitration + control for standard format
                            ((!bitDeStuffTx) & io.txPoint &   rxData  &   io.extendedMode                & txPointer === limitedTxCntExt) |   // data       (overflow is OK here)
                            ((!bitDeStuffTx) & io.txPoint &   rxData  &  !io.extendedMode                & txPointer === limitedTxCntStd) |   // data       (overflow is OK here)
                            (                  io.txPoint &   rxCrcLim                                                                  ) |   // crc
                            (goRxIdle | errorFrame | io.resetMode | io.overloadFrame)                                                         // at the end

  io.goOverloadFrame := (io.samplePoint & ((!io.sampledBit) | io.overloadRequest) & (rxEof & (!io.transmitter) & (eofCnt === 6.U) | errorFrameEnded | overloadFrameEnded) |
                         io.samplePoint & (!io.sampledBit) & io.rxInter & (bitCnt(1,0) < 2.U)                                                            |
                         io.samplePoint & (!io.sampledBit) & ((errorCnt2 === 7.U) | (overloadCnt2 === 7.U)) ) & (!overloadFrameBlocked)
  io.notFirstBitOfInter := io.rxInter & (bitCnt(1,0) =/= 0.U)
  io.goErrorFrame := (formErr | stuffErr | bitErr | ackErr | (crcErr & goRxEof))
  io.setResetMode := nodeBusOff & !nodeBusOffQ
  io.txSuccessful := transmitter & io.goRxInter & (!io.goErrorFrame) & (!errorFrameEnded) & (!overloadFrameEnded) & (!arbitrationLost)

  when(goRxId1 | io.goErrorFrame) {
    rxIdle := false.B
  }.elsewhen(goRxIdle) {
    rxIdle := true.B
  }

  when(goRxRtr1 | io.goErrorFrame) {
    rxId1 := false.B
  }.elsewhen(goRxId1) {
    rxId1 := true.B
  }

  when(goRxIde | io.goErrorFrame) {
    rxRtr1 := false.B
  }.elsewhen(goRxRtr1) {
    rxRtr1 := true.B
  }

  when (goRxR0 | goRxId2 | io.goErrorFrame) {
    rxIde := false.B
  }.elsewhen(goRxIde) {
    rxIde := true.B
  }

  when (goRxRtr2 | io.goErrorFrame) {
    rxId2 := false.B
  }.elsewhen(goRxId2) {
    rxId2 := true.B
  }

  when(goRxR1 | io.goErrorFrame) {
    rxRtr2 := false.B
  }.elsewhen(goRxRtr2) {
    rxRtr2 := true.B
  }

  when(goRxR0 | io.goErrorFrame) {
    rxR1 := false.B
  }.elsewhen(goRxR1) {
    rxR1 := true.B
  }

  when(goRxDlc | io.goErrorFrame) {
    rxR0 := false.B
  }.elsewhen(goRxR0) {
    rxR0 := true.B
  }

  when(goRxData | goRxCrc | io.goErrorFrame) {
    rxDlc := false.B
  }.elsewhen(goRxDlc) {
    rxDlc := true.B
  }

  when(goRxCrc | io.goErrorFrame) {
    rxData := false.B
  }.elsewhen(goRxData) {
    rxData := true.B
  }

  when(goRxCrcLim | io.goErrorFrame) {
    rxCrc := false.B
  }.elsewhen(goRxCrc) {
    rxCrc := true.B
  }

  when(goRxAck | io.goErrorFrame) {
    rxCrcLim := false.B
  }.elsewhen(goRxCrcLim) {
    rxCrcLim := true.B
  }

  when(goRxAckLim | io.goErrorFrame) {
    rxAck := false.B
  }.elsewhen(goRxAck) {
    rxAck := true.B
  }

  when(goRxEof | io.goErrorFrame) {
    rxAckLim := false.B
  }.elsewhen(goRxAckLim) {
    rxAckLim := true.B
  }

  when(io.goRxInter | io.goErrorFrame | io.goOverloadFrame) {
    rxEof := false.B
  }.elsewhen(goRxEof) {
    rxEof := true.B
  }

  when(goRxIdle | goRxId1 | io.goOverloadFrame | io.goErrorFrame) {
    rxInter := false.B
  }.elsewhen(io.goRxInter) {
    rxInter := true.B
  }

  when(io.samplePoint & (rxId1 | rxId2) & !bitDeStuff) {
    id := Cat(id(27,0), io.sampledBit)
  }

  when(io.samplePoint & rxRtr1 & !bitDeStuff) {
    rtr1 := io.sampledBit
  }

  when(io.samplePoint & rxRtr2 & !bitDeStuff) {
    rtr2 := io.sampledBit
  }

  when(io.samplePoint & rxIde & !bitDeStuff) {
    ide := io.sampledBit
  }

  when(io.samplePoint & rxDlc & !bitDeStuff) {
    dataLen := Cat(dataLen(2,0),io.sampledBit)
  }

  when(io.samplePoint & rxData & !bitDeStuff) {
    tmpData := Cat(tmpData(6,0),io.sampledBit)
  }

  when(io.samplePoint & rxData & !bitDeStuff & bitCnt(2,0).andR()) {
    writeDataToTmpFifo := true.B
  }.otherwise {
    writeDataToTmpFifo := false.B
  }

  when(writeDataToTmpFifo) {
    byteCnt := byteCnt + 1.U
  }.elsewhen(io.samplePoint & goRxCrcLim) {
    byteCnt := 0.U
  }

  when(writeDataToTmpFifo) {
    tmpFifo.write(byteCnt,tmpData)
  }

  when(io.samplePoint & rxCrc & !bitDeStuff) {
    crcIn := Cat(crcIn(13,0), io.sampledBit)
  }

  when(goRxId1 | goRxId2 | goRxDlc | goRxData | goRxCrc | goRxAck | goRxEof | io.goRxInter | io.goErrorFrame | io.goOverloadFrame) {
    bitCnt := 0.U
  }.elsewhen(io.samplePoint & !bitDeStuff) {
    bitCnt := bitCnt + 1.U
  }
  when(io.samplePoint) {
    when(io.goRxInter | io.goErrorFrame | io.goOverloadFrame) {
      eofCnt := 0.U
    }.elsewhen(rxEof) {
      eofCnt := eofCnt + 1.U
    }
  }

  when(bitDeStuffSet) {
    bitStuffCntEn := true.B
  }.elsewhen(bitDeStuffReset) {
    bitStuffCntEn := false.B
  }

  when(bitDeStuffReset) {
    bitStuffCnt := 1.U
  }.elsewhen(io.samplePoint & bitStuffCntEn) {
    when(bitStuffCnt === 5.U) {
      bitStuffCnt := 1.U
    }.elsewhen(io.sampledBit === io.sampledBitQ) {
      bitStuffCnt := bitStuffCnt + 1.U
    }.otherwise {
      bitStuffCnt := 1.U
    }
  }

  when(io.resetMode | bitDeStuffReset) {
    bitStuffCntTx := 1.U
  }.elsewhen(txPointQ & bitStuffCntEn) {
    when(bitStuffCntTx === 5.U) {
      bitStuffCntTx := 1.U
    }.elsewhen(io.tx === txQ) {
      bitStuffCntTx := bitStuffCntTx + 1.U
    }.otherwise {
      bitStuffCntTx := 1.U
    }
  }

  when(rstCrcEnable) {
    crcEnable := false.B
  }.elsewhen(goCrcEnable) {
    crcEnable := true.B
  }

  when(io.resetMode | errorFrameEnded) {
    crcErr := false.B
  }.elsewhen(goRxAck) {
    crcErr := crcIn =/= calculatedCrc
  }

  when(io.resetMode | errorFrameEnded | io.goOverloadFrame) {
    ackErrLatched := false.B
  }.elsewhen(ackErr) {
    ackErrLatched := true.B
  }

  when(io.resetMode | errorFrameEnded | io.goOverloadFrame) {
    bitErrLatched := false.B
  }.elsewhen(bitErr) {
    bitErrLatched := true.B
  }

  when(io.goErrorFrame | rule3Exc1(1)) {
    rule3Exc1(1) := false.B
  }.elsewhen(rule3Exc1(0) & (errorCnt1 < 7.U) & io.samplePoint & (!io.sampledBit)) {
    rule3Exc1(1) := true.B
  }

  when(errorFlagOver | rule3Exc1(1)) {
    rule3Exc1(0) := false.B
  }.elsewhen(io.transmitter & io.nodeErrorPassive & ackErr) {
    rule3Exc1(0) := true.B
  }

  when(io.resetMode | errorFrameEnded | io.goOverloadFrame) {
    stuffErrLatched := false.B
  }.elsewhen(stuffErr) {
    stuffErrLatched := true.B
  }

  when(io.resetMode | errorFrameEnded | io.goOverloadFrame) {
    formErrLatched := false.B
  }.elsewhen(formErr) {
    formErrLatched := true.B
  }

  val canCrcRx : CanCrc = withClockAndReset(clock,goCrcEnable) { Module(new CanCrc()) }
  canCrcRx.io.data := io.sampledBit
  canCrcRx.io.enable := crcEnable & io.samplePoint & !bitDeStuff
  calculatedCrc := canCrcRx.io.crc

  val canAcf : CanAcf = Module(new CanAcf())
  canAcf.io.id := id
  canAcf.io.resetMode := io.resetMode
  canAcf.io.acceptanceFilterMode := io.acceptanceFilterMode
  canAcf.io.extendedMode := io.extendedMode
  canAcf.io.acceptanceCode := io.acceptanceCode
  canAcf.io.acceptanceMask := io.acceptanceMask
  canAcf.io.goRxCrcLim := goRxCrcLim
  canAcf.io.goRxInter := io.goRxInter
  canAcf.io.goErrorFrame := io.goErrorFrame
  canAcf.io.data0 := tmpFifo(0)
  canAcf.io.data1 := tmpFifo(1)
  canAcf.io.rtr1 := rtr1
  canAcf.io.rtr2 := rtr2
  canAcf.io.ide := ide
  canAcf.io.noByte0 := noByte0
  canAcf.io.noByte1 := noByte1
  idOk := canAcf.io.idOk

  when(resetWrFifo) {
    wrFifo := false.B
  }.elsewhen(io.goRxInter & idOk & !errorFrameEnded & (!io.txState | io.selfRxRequest)) {
    wrFifo := true.B
  }

  when(resetWrFifo) {
    headerCnt := 0.U
  }.elsewhen(wrFifo & storingHeader) {
    headerCnt := headerCnt + 1.U
  }

  when(resetWrFifo) {
    dataCnt := 0.U
  }.elsewhen(wrFifo) {
    dataCnt := dataCnt + 1.U
  }

  val fifoSelector : UInt = Cat(storingHeader,io.extendedMode,ide,headerCnt)
  dataForFifo := Lookup(fifoSelector,tmpFifo(dataCnt - Cat(0.U(1.W),headerLen)), Array(
    BitPat("b111000") -> Cat(1.U(1.W), rtr2, 0.U(2.W), dataLen), // extended mode, extended format header
    BitPat("b111001") -> id(28,21), // extended mode, extended format header
    BitPat("b111010") -> id(20,13), // extended mode, extended format header
    BitPat("b111011") -> id(12,5), // extended mode, extended format header
    BitPat("b111100") -> Cat(id(4,0),0.U(3.W)), // extended mode, extended format header
    BitPat("b110000") -> Cat(0.U(1.W),rtr1,0.U(2.W),dataLen), // extended mode, standard format header
    BitPat("b110001") -> id(10,3), // extended mode, standard format header
    BitPat("b110010") -> Cat(id(2,0), rtr1, 0.U(4.W)), // extended mode, standard format header
    BitPat("b10?000") -> id(10,3), // normal mode header
    BitPat("b10?001") -> Cat(id(2,0), rtr1, dataLen), // normal mode header
  ).reverse)

  val canFifo : CanFifo = Module(new CanFifo())
  canFifo.io.wr := wrFifo
  canFifo.io.dataIn := dataForFifo
  canFifo.io.addr := io.addr(5,0)
  io.dataOut := canFifo.io.dataOut
  canFifo.io.resetMode := io.resetMode
  canFifo.io.releaseBuffer := io.releaseBuffer
  canFifo.io.extendedMode := io.extendedMode
  io.overrun := canFifo.io.overrun
  io.rxMessageCounter := canFifo.io.infoCnt
  io.infoEmpty := canFifo.io.infoEmpty


  when(io.setResetMode | errorFrameEnded | io.goOverloadFrame) {
    errorFrame := false.B
  }.elsewhen(io.goErrorFrame) {
    errorFrame := true.B
  }

  when(errorFrameEnded | io.goErrorFrame | io.goOverloadFrame) {
    errorCnt1 := 0.U
  }.elsewhen(errorFrame & io.txPoint & (errorCnt1 < 7.U)) {
    errorCnt1 := errorCnt1 + 1.U
  }

  when(errorFrameEnded | io.goErrorFrame | io.goOverloadFrame) {
    errorFlagOverLatched := false.B
  }.elsewhen(errorFlagOver) {
    errorFlagOverLatched := true.B
  }

  when(errorFrameEnded | io.goErrorFrame | io.goOverloadFrame) {
    enableErrorCnt2 := false.B
  }.elsewhen(errorFrame & (errorFlagOver & io.sampledBit)) {
    enableErrorCnt2 := true.B
  }

  when(errorFrameEnded | io.goErrorFrame | io.goOverloadFrame) {
    errorCnt2 := 0.U
  }.elsewhen(enableErrorCnt2 & io.txPoint) {
    errorCnt2 := errorCnt2 + 1.U
  }

  when(enableErrorCnt2 | io.goErrorFrame | enableOverloadCnt2 | io.goOverloadFrame) {
    delayedDominantCnt := 0.U
  }.elsewhen(io.samplePoint & !io.sampledBit & ((errorCnt1 === 7.U) | (overloadCnt1 === 7.U))) {
    delayedDominantCnt := delayedDominantCnt + 1.U
  }

  when(errorFrameEnded | io.goErrorFrame | io.goOverloadFrame | firstCompareBit) {
    passiveCnt := 1.U
  }.elsewhen(io.samplePoint & (passiveCnt < 6.U)) {
    when(errorFrame & !enableErrorCnt2 & (io.sampledBit === io.sampledBitQ)) {
      passiveCnt := passiveCnt + 1.U
    }.otherwise {
      passiveCnt := 1.U
    }
  }

  when(io.goErrorFrame) {
    firstCompareBit := true.B
  }.elsewhen(io.samplePoint) {
    firstCompareBit := false.B
  }
  
  when(overloadFrameEnded | io.goErrorFrame) {
    overloadFrame := false.B
  }.elsewhen(io.goOverloadFrame) {
    overloadFrame := true.B
  }

  when(overloadFrameEnded | io.goErrorFrame | io.goOverloadFrame) {
    overloadCnt1 := 0.U(3.W)
  }.elsewhen(io.overloadFrame & io.txPoint & (overloadCnt1 < 7.U)){
    overloadCnt1 := overloadCnt1 + 1.U
  }

  when(overloadFrameEnded | io.goErrorFrame | io.goOverloadFrame) {
    enableOverloadCnt2 := false.B
  }.elsewhen(io.overloadFrame & (overloadFlagOver & io.sampledBit)){
    enableOverloadCnt2 := true.B
  }

  when(overloadFrameEnded | io.goErrorFrame | io.goOverloadFrame) {
    overloadCnt2 := 0.U
  }.elsewhen(enableOverloadCnt2 & io.txPoint){
    overloadCnt2 := overloadCnt2 + 1.U
  }

  when(io.goErrorFrame | goRxId1) {
    overloadRequestCnt := 0.U
  }.elsewhen(io.overloadRequest & io.overloadFrame) {
    overloadRequestCnt := overloadRequestCnt + 1.U
  }

  when(io.goErrorFrame | goRxId1) {
    overloadFrameBlocked := false.B
  }.elsewhen(io.overloadRequest & io.overloadFrame & (overloadRequestCnt === 2.U)) {
    overloadFrameBlocked := true.B
  }

  when (io.resetMode | io.nodeBusOff) { // Reset or nodeBusOff
    io.txNext := true.B
  }.otherwise {
    when (io.goErrorFrame | errorFrame) { // transmitting Error frame
      when(errorCnt1 < 6.U) {
        when(io.nodeErrorPassive) {
          io.txNext := true.B
        }
        .otherwise {
          io.txNext := false.B
        }
      }.otherwise {
        io.txNext := true.B
      }
    }.elsewhen(io.goOverloadFrame | io.overloadFrame) { // transmitting overload frame
      when (overloadCnt1 < 6.U) {
        io.txNext := false.B
      }.otherwise {
        io.txNext := true.B
      }
    }
      .elsewhen(io.goTx | io.txState) { // transmitting message
        io.txNext := ((!bitDeStuffTx) & txBit) | (bitDeStuffTx & (!txQ))
      }.elsewhen(io.sendAck) { // Acknowledge
      io.txNext := false.B
    }.otherwise {
      io.txNext := true.B
    }
  }

  when(io.resetMode) {
    tx := true.B
  }.elsewhen(io.txPoint) {
    tx := io.txNext
  }

  when(io.resetMode) {
    txQ := false.B
  }.elsewhen(io.txPoint) {
    txQ := io.tx & !goEarlyTxLatched
  }

  when(io.extendedMode) {
    when(rxData) {
      txBit := Mux(rTxData(0)(0),extendedChainDataExt(txPointer),extendedChainDataStd(txPointer))
    }.elsewhen(rxCrc) {
      txBit := rCalculatedCrc(txPointer)
    }.elsewhen(finishMsg) {
      txBit := true.B
    }.otherwise {
      txBit := Mux(rTxData(0)(0),extendedChainExt(txPointer),extendedChainStd(txPointer))
    }
  }.otherwise {
    when(rxData) {
      txBit := basicChainData(txPointer)
    }.elsewhen(rxCrc) {
      txBit := rCalculatedCrc(txPointer)
    }.elsewhen(finishMsg) {
      txBit := true.B
    }.otherwise {
      txBit := basicChain(txPointer)
    }
  }

  when(rstTxPointer) {
    txPointer := 0.U
  }.elsewhen(goEarlyTx | (io.txPoint & (io.txState | io.goTx) & !bitDeStuffTx)) {
    txPointer := txPointer + 1.U
  }

  when(io.txSuccessful | io.resetMode | (io.abortTx & !io.transmitting) | !io.txState & io.txStateQ & io.singleShotTransmission) {
    needToTx := false.B
  }.elsewhen(io.txRequest & io.samplePoint) {
    needToTx := true.B
  }

  io.goTx := (!io.listenOnlyMode) & io.needToTx & (!io.txState) & (!suspend | (io.samplePoint & (suspendCnt === 7.U))) & (goEarlyTx | io.rxIdle)

  when(io.resetMode | io.txPoint) {
    goEarlyTxLatched := false.B
  }.elsewhen(goEarlyTx) {
    goEarlyTxLatched := true.B
  }

  when(io.resetMode | io.goRxInter | errorFrame | arbitrationLost) {
    txState := false.B
  }.elsewhen(io.goTx) {
    txState := true.B
  }

  when(io.goTx) {
    transmitter := true.B
  }.elsewhen(io.resetMode | goRxIdle | suspend & goRxId1) {
    transmitter := false.B
  }

  when(io.goErrorFrame | io.goOverloadFrame | io.goTx | io.sendAck) {
    transmitting := true.B
  }.elsewhen(io.resetMode | goRxIdle | (goRxId1 & !io.txState) | (arbitrationLost & io.txState)) {
    transmitting := false.B
  }

  when(io.resetMode | (io.samplePoint &(suspendCnt === 7.U))) {
    suspend := false.B
  }.elsewhen(io.notFirstBitOfInter & io.transmitter & io.nodeErrorPassive) {
    suspend := true.B
  }

  when(io.resetMode | (io.samplePoint & (suspendCnt === 7.U))) {
    suspendCntEn := false.B
  }.elsewhen(suspend & io.samplePoint & lastBitOfInter) {
    suspendCntEn := true.B
  }

  when(io.resetMode | (io.samplePoint & (suspendCnt === 7.U))) {
    suspendCnt := 0.U
  }.elsewhen(suspendCntEn & io.samplePoint) {
    suspendCnt := suspendCnt + 1.U
  }

  when(goRxIdle | goRxId1 | errorFrame | io.resetMode) {
    finishMsg := false.B
  }.elsewhen(goRxCrcLim) {
    finishMsg := true.B
  }

  when(goRxIdle | errorFrameEnded) {
    arbitrationLost := false.B
  }.elsewhen(io.transmitter & io.samplePoint & io.tx & arbitrationField & !io.sampledBit) {
    arbitrationLost := true.B
  }

  when(io.samplePoint) {
    arbitrationFieldD := arbitrationField
  }

io.setArbitrationLostIrq := arbitrationLost & (!arbitrationLostQ) & !arbitrationBlocked

  when(io.samplePoint & !bitDeStuff) {
    when(arbitrationFieldD) {
      arbitrationCnt := arbitrationCnt + 1.U
    }.otherwise {
      arbitrationCnt := 0.U
    }
  }

  when(io.setArbitrationLostIrq) {
    arbitrationLostCapture := arbitrationCnt
  }

  when(io.readArbitrationLostCaptureReg) {
    arbitrationBlocked := false.B
  }.elsewhen(io.setArbitrationLostIrq) {
    arbitrationBlocked := true.B
  }

  when(io.writeEnReceiveErrorCounter & (!io.nodeBusOff)) {
    rxErrorCount := Cat(0.U(1.W),io.dataIn)
  }.elsewhen(io.setResetMode){
    rxErrorCount := 0.U
  }.otherwise {
    when(!io.listenOnlyMode & (!io.transmitter | arbitrationLost)) {
      when(goRxAckLim & (!io.goErrorFrame) & (!crcErr) & (io.rxErrorCount > 0.U)) {
        when(io.rxErrorCount > 127.U) {
          rxErrorCount := 127.U
        }.otherwise {
          rxErrorCount := io.rxErrorCount - 1.U
        }
      }.elsewhen(io.rxErrorCount < 128.U) {
        when(io.goErrorFrame & !rule5) {
          rxErrorCount := io.rxErrorCount + 1.U
        }.elsewhen((errorFlagOver & (!errorFlagOverLatched) & io.samplePoint & (!io.sampledBit) & (errorCnt1 === 7.U)) |  // 2
                   (io.goErrorFrame & rule5) | (io.samplePoint & (!io.sampledBit) & (delayedDominantCnt === 7.U))) {
          rxErrorCount := io.rxErrorCount + 8.U
        }
      }
    }
  }

  when(io.writeEnTransmitErrorCounter) {
    txErrorCount := Cat(0.U(1.W),io.dataIn)
  }.otherwise {
    when(io.setResetMode) {
      txErrorCount := 128.U
    }.elsewhen((txErrorCount > 0.U) & (io.txSuccessful | busFree)) {
      txErrorCount := txErrorCount - 1.U
    }.elsewhen(io.transmitter & (!arbitrationLost)) {
      when((io.samplePoint & (!io.sampledBit) & (delayedDominantCnt === 7.U)) | (io.goErrorFrame & rule5) |
        (io.goErrorFrame & (!(io.transmitter & io.nodeErrorPassive & ackErr)) &
          (!(io.transmitter & stuffErr & arbitrationField & io.samplePoint & io.tx & (!io.sampledBit)))) |
        (errorFrame & rule3Exc1(1))) {
        txErrorCount := txErrorCount + 8.U
      }
    }
  }

  when((rxErrorCount < 128.U) & (txErrorCount < 128.U)) {
    nodeErrorPassive := false.B
  }.elsewhen(((rxErrorCount >= 128.U) | (txErrorCount >= 128.U)) & (errorFrameEnded | io.goErrorFrame | (!io.resetMode) & resetModeQ) & (!io.nodeBusOff)) {
    nodeErrorPassive := true.B
  }

  io.nodeErrorActive := !(io.nodeErrorPassive | io.nodeBusOff)

  when((io.rxErrorCount === 0.U) & (io.txErrorCount === 0.U) & (!io.resetMode) | (io.writeEnTransmitErrorCounter & (io.dataIn < 255.U))) {
    nodeBusOff := false.B
  }.elsewhen((io.txErrorCount >= 256.U) | (io.writeEnTransmitErrorCounter & (io.dataIn === 255.U))) {
    nodeBusOff := true.B
  }

  when(io.samplePoint) {
    when(io.sampledBit & busFreeCntEn & busFreeCnt < 10.U) {
      busFreeCnt := busFreeCnt + 1.U
    }.otherwise {
      busFreeCnt := 0.U
    }
  }

  when((!io.resetMode) & resetModeQ | nodeBusOffQ & !io.resetMode) {
    busFreeCntEn := true.B
  }.elsewhen(io.samplePoint & io.sampledBit & (busFreeCnt === 10.U) & !io.nodeBusOff) {
    busFreeCntEn := false.B
  }

  when(io.samplePoint & io.sampledBit & (busFreeCnt === 10.U) & waitingForBusFree) {
    busFree := true.B
  }.otherwise {
    busFree := false.B
  }

  when(busFree & !io.nodeBusOff) {
    waitingForBusFree := false.B
  }.elsewhen(nodeBusOffQ & !io.resetMode) {
    waitingForBusFree := true.B
  }

  when(io.readErrorCodeCaptureReg) {
    errorCaptureCode := 0.U
  }.elsewhen(io.setBusErrorIrq) {
    errorCaptureCode := Cat(errorCaptureCodeType,errorCaptureCodeDirection,errorCaptureCodeSegment)
  }
  errorCaptureCodeSegment := Cat(rxCrcLim | rxAck | rxAckLim | rxEof | io.rxInter | errorFrame | io.overloadFrame,
    (rxId2 & (bitCnt > 4.U)) | rxRtr2 | rxR1 | rxR0 | rxDlc | rxData | rxCrc | rxCrcLim | rxAck | rxAckLim | rxEof | io.overloadFrame,
    (rxId1 & (bitCnt > 7.U)) | rxRtr1 | rxIde | rxId2 | rxRtr2 | rxR1 | errorFrame & io.nodeErrorPassive | io.overloadFrame,
  io.rxIdle | rxId1 | rxId2 | rxDlc | rxData | rxAckLim | rxEof | io.rxInter | errorFrame & io.nodeErrorPassive,
    io.rxIdle | rxIde | (rxId2 & (bitCnt < 13.U)) | rxR1 | rxR0 | rxDlc | rxAck | rxAckLim | errorFrame & io.nodeErrorActive)

  when(bitErr) {
    errorCaptureCodeType := 0.U
  }.elsewhen(formErr) {
    errorCaptureCodeType := 1.U
  }.elsewhen(stuffErr) {
    errorCaptureCodeType := 2.U
  }.otherwise {
    errorCaptureCodeType := 3.U
  }

  io.transmitStatus := io.transmitting  | (io.extendedMode & waitingForBusFree)
  io.receiveStatus  := Mux(io.extendedMode, waitingForBusFree || (!io.rxIdle) && (!io.transmitting),
                                            (!waitingForBusFree) && (!io.rxIdle) && (!io.transmitting))
  io.setBusErrorIrq := io.goErrorFrame & (!errorCaptureCodeBlocked)

  when(io.readErrorCodeCaptureReg) {
    errorCaptureCodeBlocked := false.B
  }.elsewhen(io.setBusErrorIrq) {
    errorCaptureCodeBlocked := true.B
  }
  io.busOffOn := !io.nodeBusOff
  io.errorStatus := Mux(io.extendedMode,(io.rxErrorCount >= io.errorWarningLimit) | (io.txErrorCount >= io.errorWarningLimit),
                                        (io.rxErrorCount >= 96.U) | (io.txErrorCount >= 96.U))
}
