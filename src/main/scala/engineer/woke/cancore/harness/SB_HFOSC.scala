package engineer.woke.cancore.harness

import chisel3._

class SB_HFOSC extends BlackBox(Map("TRIM_EN"->"0b0","CLKHF_DIV"->"0b00")) {
  val io = IO(new Bundle {
    val TRIM0 : Bool = Input(Bool())
    val TRIM1 : Bool = Input(Bool())
    val TRIM2 : Bool = Input(Bool())
    val TRIM3 : Bool = Input(Bool())
    val TRIM4 : Bool = Input(Bool())
    val TRIM5 : Bool = Input(Bool())
    val TRIM6 : Bool = Input(Bool())
    val TRIM7 : Bool = Input(Bool())
    val TRIM8 : Bool = Input(Bool())
    val TRIM9 : Bool = Input(Bool())
    val CLKHFPU : Bool = Input(Bool())
    val CLKHFEN : Bool = Input(Bool())
    val CLKHF : Clock = Output(Clock())
  })

}

