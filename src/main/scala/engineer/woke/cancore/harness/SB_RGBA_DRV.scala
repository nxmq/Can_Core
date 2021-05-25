package engineer.woke.cancore.harness

import chisel3._

class SB_RGBA_DRV extends BlackBox(Map("CURRENT_MODE" -> "0b1", "RGB0_CURRENT" -> "0b000011","RGB1_CURRENT" -> "0b000011","RGB2_CURRENT" -> "0b000011")) {
  val io = IO(new Bundle {
    val CURREN : Bool = Input(Bool())
    val RGB0PWM : Bool = Input(Bool())
    val RGB1PWM : Bool = Input(Bool())
    val RGB2PWM : Bool = Input(Bool())
    val RGB0 : Bool = Output(Bool())
    val RGB1 : Bool = Output(Bool())
    val RGB2 : Bool = Output(Bool())
  })

}

