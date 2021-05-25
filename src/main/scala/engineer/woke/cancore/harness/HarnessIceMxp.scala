package engineer.woke.cancore.harness

import chisel3._
import engineer.woke.cancore.CanTop

class HarnessIceMxp extends RawModule {
  val io = IO(new Bundle {
    val reset : AsyncReset = Input(AsyncReset())
    val ledR : Bool = Output(Bool())
    val ledG : Bool = Output(Bool())
    val ledB : Bool = Output(Bool())
    val canTx : Bool = Output(Bool())
    val canRx : Bool = Input(Bool())
    val irq : Bool = Output(Bool())
  })
  val HFOSC : SB_HFOSC = Module(new SB_HFOSC())
  val wbClock : Clock = HFOSC.io.CLKHF

  val clkReg : Bool = withClockAndReset(wbClock,io.reset) { RegInit(false.B) }
  clkReg := !clkReg
  val canClock : Clock = clkReg.asClock()
  withClockAndReset(canClock,io.reset) {
    val canTop : CanTop = Module(new CanTop())
    canTop.io.canRx := io.canRx
    io.canTx := canTop.io.canTx
    io.irq := canTop.io.irqOn
    canTop.io.wbClkI := wbClock
    canTop.io.wbDatI
  }
}

