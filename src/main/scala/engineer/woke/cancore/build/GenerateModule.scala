package engineer.woke.cancore.build

import chisel3.stage._
import engineer.woke.cancore._

object GenerateModule extends App {
  (new ChiselStage).execute(Array("-X", "verilog","--target-dir", "genrtl"),Seq(ChiselGeneratorAnnotation(() => new CanRegisters())))
}
