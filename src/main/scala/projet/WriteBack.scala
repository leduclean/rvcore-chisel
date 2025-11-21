package projet

import chisel3._
import projet.ControlWriteback
import projet.ExWbPipelineData

class WbData extends Bundle {
  val pipe    = Flipped(new ExWbPipelineData) // Pipelined data from the Ex stage
  val memData = Input(UInt(32.W))             // Direct data from the Mem stage
}

class WriteBack extends Module {

  val io = IO(new Bundle {
    val control = Flipped(new ControlWriteback) // Control signals.
    val data    = new WbData()                  // Data to write back.
    val rfWrite = new RFWriteInterface          // Controler of the write input of the RF.
  })

  io.rfWrite.we := io.control.weRf
  io.rfWrite.rw := io.data.pipe.rd
  io.rfWrite.w  := Mux(io.control.wbSel === WbMuxSel.MEM, io.data.memData, io.data.pipe.value)
}
