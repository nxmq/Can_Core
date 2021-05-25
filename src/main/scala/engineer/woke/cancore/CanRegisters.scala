package engineer.woke.cancore

import chisel3._
import chisel3.util._

//VERIFIED CORRECT TO can_register_syn.v VIA YOSYS EQUIV ON 4/30/2021, DONT FUCKING TOUCH
class CanRegisters extends Module with RequireAsyncReset {
  val io = IO(new Bundle {
    val cs: Bool = Input(Bool())
    val writeEn: Bool = Input(Bool())
    val addr: UInt = Input(UInt(8.W))
    val dataIn: UInt = Input(UInt(8.W))
    val dataOut: UInt = Output(UInt(8.W))
    val irqN: Bool = Output(Bool())
    val samplePoint: Bool = Input(Bool())
    val transmitting: Bool = Input(Bool())
    val setResetMode: Bool = Input(Bool())
    val nodeBusOff: Bool = Input(Bool())
    val errorStatus: Bool = Input(Bool())
    val rxErrorCount: UInt = Input(UInt(8.W))
    val txErrorCount: UInt = Input(UInt(8.W))
    val transmitStatus: Bool = Input(Bool())
    val receiveStatus: Bool = Input(Bool())
    val txSuccessful: Bool = Input(Bool())
    val needToTx: Bool = Input(Bool())
    val overrun: Bool = Input(Bool())
    val infoEmpty: Bool = Input(Bool())
    val setBusErrorIrq: Bool = Input(Bool())
    val setArbitrationLostIrq: Bool = Input(Bool())
    val arbitrationLostCapture: UInt = Input(UInt(5.W))
    val nodeErrorPassive: Bool = Input(Bool())
    val nodeErrorActive: Bool = Input(Bool())
    val rxMessageCounter: UInt = Input(UInt(7.W))

    /* Mode Register */
    val resetMode: Bool = Output(Bool())
    val listenOnlyMode: Bool = Output(Bool())
    val acceptanceFilterMode: Bool = Output(Bool())
    val selfTestMode: Bool = Output(Bool())

    /* Command Register */
    val clearDataOverrun: Bool = Output(Bool())
    val releaseBuffer: Bool = Output(Bool())
    val abortTx: Bool = Output(Bool())
    val txRequest: Bool = Output(Bool())
    val selfRxRequest: Bool = Output(Bool())
    val singleShotTransmission: Bool = Output(Bool())
    val txState: Bool = Input(Bool())
    val txStateQ: Bool = Input(Bool())
    val overloadRequest: Bool = Output(Bool())
    val overloadFrame: Bool = Input(Bool())

    /* Arbitration Lost Capture Register */
    val readArbitrationLostCaptureReg: Bool = Output(Bool())

    /* Error Code Capture Register */
    val readErrorCodeCaptureReg: Bool = Output(Bool())
    val errorCaptureCode: UInt = Input(UInt(8.W))

    /* Bus Timing 0 register */
    val baudRatePrescaler: UInt = Output(UInt(6.W))
    val syncJumpWidth: UInt = Output(UInt(2.W))

    /* Bus Timing 1 register */
    val timeSegment1: UInt = Output(UInt(4.W))
    val timeSegment2: UInt = Output(UInt(3.W))
    val tripleSampling: Bool = Output(Bool())

    /* Error Warning Limit register */
    val errorWarningLimit: UInt = Output(UInt(8.W))
    val writeEnReceiveErrorCounter: Bool = Output(Bool())
    val writeEnTransmitErrorCounter: Bool = Output(Bool())

    /* Clock Divider register */
    val extendedMode: Bool = Output(Bool())
    val clkout: Bool = Output(Bool())

    /* Acceptance code register */
    val acceptanceCode: Vec[UInt] = Output(Vec(4,UInt(8.W)))

    /* Acceptance mask register */
    val acceptanceMask: Vec[UInt] = Output(Vec(4,UInt(8.W)))

    /* Tx Data Registers */
    val txData: Vec[UInt] = Output(Vec(13,UInt(8.W)))
  })

  def writeEnAddrExt(extModeAddr : Int) : Bool = {
    io.cs & io.writeEn & (io.addr === extModeAddr.U(8.W)) & io.extendedMode
  }

  def writeEnAddrBasicExt(basicModeAddr : Int, extModeAddr : Int) : Bool = {
    io.cs & io.writeEn & ((!io.extendedMode) & (io.addr === basicModeAddr.U) | io.extendedMode & (io.addr === extModeAddr.U))
  }

  def writeEnAddr(allModeAddr : Int) : Bool = {
    io.cs & io.writeEn & (io.addr === allModeAddr.U)
  }

  val irqN: Bool = RegInit(true.B)
  io.irqN := irqN
  val selfRxRequest: Bool = RegInit(false.B)
  io.selfRxRequest := selfRxRequest
  val singleShotTransmission: Bool = RegInit(false.B)
  io.singleShotTransmission := singleShotTransmission
  val txSuccessfulQ: Bool = RegNext(io.txSuccessful)
  val overrunQ: Bool = RegNext(io.overrun)
  val overrunStatus: Bool = RegInit(false.B)
  val transmissionComplete: Bool = RegInit(true.B)
  val transmitBufferStatus: Bool = RegInit(true.B)
  val transmitBufferStatusQ: Bool = RegNext(transmitBufferStatus)
  val receiveBufferStatus: Bool = RegInit(false.B)
  val errorStatusQ: Bool = RegNext(io.errorStatus)
  val nodeBusOffQ: Bool = RegNext(io.nodeBusOff)
  val nodeErrorPassiveQ: Bool = RegNext(io.nodeErrorPassive)

  val dataOverrunIrqEn: Bool = Wire(Bool())
  val errorWarningIrqEn: Bool = Wire(Bool())
  val transmitIrqEn: Bool = Wire(Bool())
  val receiveIrqEn: Bool = Wire(Bool())

  val irqReg: UInt = Wire(UInt(8.W))
  val irq: Bool = Wire(Bool())

  val writeEnMode: Bool = writeEnAddr(0)
  val writeEnCommand: Bool = writeEnAddr(1)
  val writeEnBusTiming0: Bool = writeEnAddr(6) & io.resetMode
  val writeEnBusTiming1: Bool = writeEnAddr(7) & io.resetMode
  val writeEnClockDivLow: Bool = writeEnAddr(31)
  val writeEnClockDivHigh: Bool = writeEnClockDivLow & io.resetMode

  val read: Bool = io.cs & (!io.writeEn)
  val readIrqReg: Bool = read & (io.addr === 3.U)
  io.readArbitrationLostCaptureReg := read & io.extendedMode & (io.addr === 11.U)
  io.readErrorCodeCaptureReg := read & io.extendedMode & (io.addr === 12.U)

  val writeEnAcceptanceCode : Vec[Bool] = Wire(Vec(4,Bool()))
  val writeEnAcceptanceMask : Vec[Bool] = Wire(Vec(4,Bool()))
  writeEnAcceptanceCode(0) := writeEnAddrBasicExt(4,16) & io.resetMode
  writeEnAcceptanceMask(0) := writeEnAddrBasicExt(5,20) & io.resetMode

  (1 to 3).foreach { v=>
      writeEnAcceptanceCode(v) := writeEnAddrExt(16+v) & io.resetMode
      writeEnAcceptanceMask(v) := writeEnAddrExt(20+v) & io.resetMode
  }

  val writeEnTxData : Vec[Bool] = Wire(Vec(13,Bool()))
  (0 to 12).foreach { v=>
    if(v < 10)
      writeEnTxData(v) := writeEnAddrBasicExt(10+v,16+v) & (!io.resetMode) & transmitBufferStatus
    else
      writeEnTxData(v) := writeEnAddrExt(16+v) & (!io.resetMode) & transmitBufferStatus
  }

  val writeEnInterruptEnable: Bool = writeEnAddrExt(4)
  val writeEnErrorWarningLimit: Bool = writeEnAddrExt(13) & io.resetMode
  io.writeEnReceiveErrorCounter := writeEnAddrExt(14) & io.resetMode
  io.writeEnTransmitErrorCounter := writeEnAddrExt(15) & io.resetMode

  val mode: Bool = CanRegisterAsyncAndSyncReset(1,io.setResetMode,io.dataIn(0),writeEnMode).asBool()
  val modeBasic: UInt = CanRegisterAsyncReset(0,io.dataIn(4,1),writeEnMode)
  val modeExt: UInt = WireDefault(CanRegisterAsyncReset(0,io.dataIn(3,1),writeEnMode & io.resetMode))
  val receiveIrqEnBasic: Bool = modeBasic(0)
  val transmitIrqEnBasic: Bool = modeBasic(1)
  val errorIrqEnBasic: Bool = modeBasic(2)
  val overrunIrqEnBasic: Bool = modeBasic(3)

  io.resetMode := mode
  io.listenOnlyMode := io.extendedMode & modeExt(0)
  io.selfTestMode := io.extendedMode & modeExt(1)
  io.acceptanceFilterMode := io.extendedMode & modeExt(2)

  val commandBld: Vec[Bool] = Wire(Vec(5,Bool()))
  val command : UInt = WireDefault(commandBld.asUInt())
  commandBld(0) := CanRegisterAsyncAndSyncReset(0,command(0) & io.samplePoint | io.resetMode,io.dataIn(0),writeEnCommand)
  commandBld(1) := CanRegisterAsyncAndSyncReset(0,io.samplePoint & (io.txRequest | (io.abortTx & !io.transmitting)) | io.resetMode,io.dataIn(1),writeEnCommand)
  commandBld(2) := CanRegisterAsyncAndSyncReset(0 ,command(3) | command(2) | io.resetMode,io.dataIn(2),writeEnCommand)
  commandBld(3) := CanRegisterAsyncAndSyncReset(0,command(3) | command(2) | io.resetMode,io.dataIn(3),writeEnCommand)
  commandBld(4) := CanRegisterAsyncAndSyncReset(0,command(4) & io.samplePoint | io.resetMode,io.dataIn(4),writeEnCommand)

  when(command(4) & !command(0)) {
    selfRxRequest := true.B
  }.elsewhen((!io.txState) & io.txStateQ) {
    selfRxRequest := false.B
  }

  io.clearDataOverrun := command(3)
  io.releaseBuffer := command(2)
  io.txRequest := command(0) | command(4)
  io.abortTx := command(1) & !io.txRequest

  when(io.txRequest & command(1) & io.samplePoint) {
    singleShotTransmission := true.B
  }
  .elsewhen((!io.txState) & io.txStateQ) {
    singleShotTransmission := false.B
  }

  val overloadFrameQ : Bool = RegNext(io.overloadFrame,false.B)

  io.overloadRequest := CanRegisterAsyncAndSyncReset(0,io.overloadFrame & !overloadFrameQ,io.dataIn(5),writeEnCommand)
  io.overloadRequest := false.B

  val status : UInt = Wire(UInt(8.W))
  status := Cat(io.nodeBusOff,
                io.errorStatus,
                io.transmitStatus,
                io.receiveStatus,
                transmissionComplete,
                transmitBufferStatus,
                overrunStatus,
                receiveBufferStatus)

  when(io.txRequest) {
    transmitBufferStatus := false.B
  }.elsewhen(io.resetMode | !io.needToTx) {
    transmitBufferStatus := true.B
  }

  when(io.txSuccessful & (!txSuccessfulQ) | io.abortTx) {
    transmissionComplete := true.B
  }
  .elsewhen(io.txRequest) {
    transmissionComplete := false.B
  }

  when(io.overrun & !overrunQ) {
    overrunStatus := true.B
  }
  .elsewhen(io.resetMode | io.clearDataOverrun) {
    overrunStatus := false.B
  }

  when(io.resetMode | io.releaseBuffer) {
    receiveBufferStatus := false.B
  }
  .elsewhen(!io.infoEmpty) {
    receiveBufferStatus := true.B
  }

  val irqEnExt : UInt = Wire(UInt(8.W))
  val busErrorIrqEn : Bool = irqEnExt(7)
  val arbitrationLostIrqEn : Bool = irqEnExt(6)
  val errorPassiveIrqEn : Bool = irqEnExt(5)
  val dataOverrunIrqEnExt : Bool = irqEnExt(3)
  val errorWarningIrqEnExt : Bool = irqEnExt(2)
  val transmitIrqEnExt : Bool = irqEnExt(1)
  val receiveIrqEnExt : Bool = irqEnExt(0)

  irqEnExt := CanRegister(io.dataIn,writeEnInterruptEnable)

  val busTiming0 : UInt = CanRegister(io.dataIn,writeEnBusTiming0)

  io.baudRatePrescaler := busTiming0(5,0)
  io.syncJumpWidth := busTiming0(7,6)

  val busTiming1 : UInt = CanRegister(io.dataIn,writeEnBusTiming1)

  io.timeSegment1 := busTiming1(3,0)
  io.timeSegment2 := busTiming1(6,4)
  io.tripleSampling := busTiming1(7)

  io.errorWarningLimit := CanRegisterAsyncReset(96,io.dataIn,writeEnErrorWarningLimit)

  io.extendedMode := CanRegisterAsyncReset(0,io.dataIn(7),writeEnClockDivHigh).asBool()
  val clockOff : Bool = CanRegisterAsyncReset(0,io.dataIn(3),writeEnClockDivHigh).asBool()
  val cd : UInt = CanRegisterAsyncReset(0,io.dataIn(2,0),writeEnClockDivLow)
  val clkoutDiv : UInt = WireDefault(Mux(cd === 7.U, 0.U, cd))
  val clockDivider : UInt = WireDefault(Cat(io.extendedMode,0.U(3.W),clockOff,cd))

  val clkoutCnt : UInt = RegInit(0.U(3.W))
  val clkoutTmp : Bool = RegInit(false.B)

  when(clkoutCnt === clkoutDiv) {
    clkoutCnt := 0.U
    clkoutTmp := !clkoutTmp
  }
  .otherwise {
    clkoutCnt := clkoutCnt + 1.U
  }

  io.clkout := Mux(clockOff, true.B , Mux(cd.andR(), clock.asBool() , clkoutTmp))

  when (cd.andR() === true.B) {
     io.clkout := clockOff | clock.asBool()
  }
  .otherwise {
    io.clkout := clockOff | clkoutTmp
  }

  (0 to 3).foreach { v =>
    io.acceptanceCode(v) := CanRegister(io.dataIn,writeEnAcceptanceCode(v))
    io.acceptanceMask(v) := CanRegister(io.dataIn,writeEnAcceptanceMask(v))
  }

  (0 to 12).foreach { v =>
    io.txData(v) := CanRegister(io.dataIn,writeEnTxData(v))
  }

  io.dataOut := Mux(io.extendedMode,
    MuxLookup(io.addr(4,0), 0.U, Array(
      0.U(5.W) -> Cat(0.U(4.W), modeExt, mode),
      2.U(5.W) -> status,
      3.U(5.W) -> irqReg,
      4.U(5.W) -> irqEnExt,
      6.U(5.W) -> busTiming0,
      7.U(5.W) -> busTiming1,
      11.U(5.W) -> Cat(0.U(3.W), io.arbitrationLostCapture),
      12.U(5.W) -> io.errorCaptureCode,
      13.U(5.W) -> io.errorWarningLimit,
      14.U(5.W) -> io.rxErrorCount,
      15.U(5.W) -> io.txErrorCount,
      16.U(5.W) -> io.acceptanceCode(0),
      17.U(5.W) -> io.acceptanceCode(1),
      18.U(5.W) -> io.acceptanceCode(2),
      19.U(5.W) -> io.acceptanceCode(3),
      20.U(5.W) -> io.acceptanceMask(0),
      21.U(5.W) -> io.acceptanceMask(1),
      22.U(5.W) -> io.acceptanceMask(2),
      23.U(5.W) -> io.acceptanceMask(3),
      29.U(5.W) -> Cat(0.U(1.W),io.rxMessageCounter),
      31.U(5.W) -> clockDivider
    ).reverse),
    MuxLookup(io.addr(4,0),0.U, Array(
      0.U(5.W) -> Cat(1.U(3.W), modeBasic, mode),
      1.U(5.W) -> 0xFF.U,
      2.U(5.W) -> status,
      3.U(5.W) -> Cat(0xE.U(4.W),irqReg(3,0)),
      4.U(5.W) -> Mux(io.resetMode, io.acceptanceCode(0) , 255.U),
      5.U(5.W) -> Mux(io.resetMode, io.acceptanceMask(0) , 255.U),
      6.U(5.W) -> Mux(io.resetMode, busTiming0 , 255.U),
      7.U(5.W) -> Mux(io.resetMode, busTiming1 , 255.U),
      10.U(5.W) -> Mux(io.resetMode, 255.U, io.txData(0)),
      11.U(5.W) -> Mux(io.resetMode, 255.U, io.txData(1)),
      12.U(5.W) -> Mux(io.resetMode, 255.U, io.txData(2)),
      13.U(5.W) -> Mux(io.resetMode, 255.U, io.txData(3)),
      14.U(5.W) -> Mux(io.resetMode, 255.U, io.txData(4)),
      15.U(5.W) -> Mux(io.resetMode, 255.U, io.txData(5)),
      16.U(5.W) -> Mux(io.resetMode, 255.U, io.txData(6)),
      17.U(5.W) -> Mux(io.resetMode, 255.U, io.txData(7)),
      18.U(5.W) -> Mux(io.resetMode, 255.U, io.txData(8)),
      19.U(5.W) -> Mux(io.resetMode, 255.U, io.txData(9)),
      31.U(5.W) -> clockDivider
    ).reverse))

  dataOverrunIrqEn := Mux(io.extendedMode,dataOverrunIrqEnExt,overrunIrqEnBasic)
  errorWarningIrqEn := Mux(io.extendedMode,errorWarningIrqEnExt,errorIrqEnBasic)
  transmitIrqEn := Mux(io.extendedMode,transmitIrqEnExt,transmitIrqEnBasic)
  receiveIrqEn := Mux(io.extendedMode,receiveIrqEnExt,receiveIrqEnBasic)

  val dataOverrunIrq : Bool = RegInit(false.B)
  val transmitIrq : Bool = RegInit(false.B)
  val receiveIrq : Bool = RegInit(false.B)
  val errorIrq : Bool = RegInit(false.B)
  val busErrorIrq : Bool = RegInit(false.B)
  val arbitrationLostIrq : Bool = RegInit(false.B)
  val errorPassiveIrq : Bool = RegInit(false.B)

  when(io.overrun & !overrunQ & dataOverrunIrqEn) {
    dataOverrunIrq := true.B
  }.elsewhen(io.resetMode | readIrqReg) {
    dataOverrunIrq := false.B
  }

  when(io.resetMode | readIrqReg) {
    transmitIrq := false.B
  }.elsewhen(transmitBufferStatus & !transmitBufferStatusQ & transmitIrqEn) {
    transmitIrq := true.B
  }

  when((!io.infoEmpty) & (!receiveIrq) & receiveIrqEn) {
    receiveIrq := true.B
  }.elsewhen(io.resetMode || io.releaseBuffer) {
    receiveIrq := false.B
  }

  when(((io.errorStatus ^ errorStatusQ) | (io.nodeBusOff ^ nodeBusOffQ)) & errorWarningIrqEn) {
    errorIrq := true.B
  }.elsewhen(readIrqReg) {
    errorIrq := false.B
  }

  when(io.setBusErrorIrq & busErrorIrqEn) {
    busErrorIrq := true.B
  }.elsewhen(io.resetMode | readIrqReg) {
    busErrorIrq := false.B
  }

  when(io.setArbitrationLostIrq & arbitrationLostIrqEn) {
    arbitrationLostIrq := true.B
  }.elsewhen(io.resetMode | readIrqReg) {
    arbitrationLostIrq := false.B
  }

  when((io.nodeErrorPassive & (!nodeErrorPassiveQ) | (!io.nodeErrorPassive) & nodeErrorPassiveQ & io.nodeErrorActive) & errorPassiveIrqEn) {
    errorPassiveIrq := true.B
  }.elsewhen(io.resetMode | readIrqReg) {
    errorPassiveIrq := false.B
  }

  irqReg := Cat(busErrorIrq,arbitrationLostIrq,errorPassiveIrq,false.B,dataOverrunIrq,errorIrq,transmitIrq,receiveIrq)
  irq := irqReg.orR()

  when(readIrqReg | io.releaseBuffer) {
    irqN := true.B
  }.elsewhen(irq) {
    irqN := false.B
  }
}
