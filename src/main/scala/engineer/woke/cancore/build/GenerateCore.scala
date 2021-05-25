package engineer.woke.cancore.build

import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import engineer.woke.cancore.{CanBsp, CanTop}

object GenerateCore extends App {
  (new ChiselStage).execute(Array("-X", "verilog","--target-dir", "genrtl"),Seq(ChiselGeneratorAnnotation(() => new CanTop())))
}

