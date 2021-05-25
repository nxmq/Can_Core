package engineer.woke.cancore.harness

import chisel3._
import chisel3.util._

class WishboneDebugBusMaster extends Module {
  val io = IO(new Bundle {
    val cmdReset = Input(Bool())
    val cmdValid = Input(Bool())
    val cmdReady = Output(Bool())
    val cmdData = Input(UInt(36.W))

    val respValid = Output(Bool())
    val respData = Output(UInt(36.W))

    val wbCyc = Output(Bool())
    val wbStb = Output(Bool())
    val wbWe = Output(Bool())
    val wbAddr = Output(UInt(32.W))
    val wbDataIn = Input(UInt(32.W))
    val wbDataOut = Output(UInt(32.W))
    val wbAck = Input(Bool())
    val wbErr = Input(Bool())
    val wbStall = Input(Bool())
  })

  val CMD_READ_REQ = 1
  val CMD_WRITE_REQ = 2
  val CMD_SET_ADDR = 3
  val CMD_SET_ADDR_INC = 7

  val RESP_READ_RESP = 1
  val RESP_WRITE_ACK = 2
  val RESP_ADDR_ACK = 3
  val RESP_BUS_ERROR = 4
  val RESP_BUS_RESET = 5

  val addrInc = RegInit(false.B)
  val respValid = RegInit(true.B)
  io.respValid := respValid
  val respData = Reg(UInt(36.W))
  io.respData := respData
  val wbCyc = RegInit(false.B)
  io.wbCyc := wbCyc
  val wbStb = RegInit(false.B)
  io.wbStb := wbStb
  val wbWe = RegInit(false.B)
  io.wbWe := wbWe
  val wbAddr = RegInit(0.U(32.W))
  io.wbAddr := wbAddr
  val wbDataOut = RegInit(0.U(32.W))
  io.wbDataOut := wbDataOut
  io.cmdReady := !wbCyc

  val cmdRecv = WireDefault(io.cmdValid & io.cmdReady)
  val cmdInst = WireDefault(io.cmdData(35,32))
  val cmdData = WireDefault(io.cmdData(31,0))

  when(io.wbErr | io.cmdReset) {
    wbCyc := false.B
    wbStb := false.B
  }.elsewhen(wbStb) {
    when(!io.wbStall) {
      wbStb := false.B
    }
  }.elsewhen(wbCyc) {
    when(io.wbAck) {
      wbCyc := false.B
    }
  }.otherwise {
    when(cmdRecv & ((cmdInst === CMD_READ_REQ.U) | (cmdInst === CMD_WRITE_REQ.U))) {
      wbCyc := true.B
      wbStb := true.B
    }
  }
  when(cmdRecv) {
    when((cmdInst === CMD_SET_ADDR.U | (cmdInst === CMD_SET_ADDR_INC.U))) {
      wbAddr := cmdData
      addrInc := (cmdInst === CMD_SET_ADDR_INC.U)
    }
  }.elsewhen(wbStb & !io.wbStall) {
    wbAddr := wbAddr + addrInc.asUInt()
  }
  when(!(wbStb & io.wbStall)) {
    wbDataOut := cmdData
  }

  when(!wbCyc) {
    wbWe := cmdRecv & (cmdInst === CMD_WRITE_REQ.U)
  }

  when(!(wbStb & io.wbStall)) {
    wbDataOut := cmdData
  }

  when(io.cmdReset) {
    io.respValid := true.B
    io.respData := Cat(RESP_BUS_RESET.U,0.U(32.W))
  }.elsewhen(io.wbErr) {
    io.respValid := true.B
    io.respData := Cat(RESP_BUS_ERROR.U,0.U(32.W))
  }.elsewhen(wbCyc & io.wbAck) {
    io.respValid := true.B
    when(wbWe) {
      io.respData := Cat(RESP_WRITE_ACK.U,0.U(32.W))
    }.otherwise {
      io.respData := Cat(RESP_READ_RESP.U,io.wbDataIn)
    }
  }.elsewhen(cmdRecv & ((cmdInst === CMD_SET_ADDR.U) || (cmdInst === CMD_SET_ADDR_INC.U))) {
    io.respValid := true.B
    io.respData := Cat(RESP_ADDR_ACK.U,0.U(32.W))
  }.otherwise {
    io.respValid := false.B
  }

}

