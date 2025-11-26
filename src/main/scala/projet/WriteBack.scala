package projet

import chisel3._
import chisel3.util.MuxLookup
import projet.ControlWriteback
import projet.ExWbPipelineData

class WbData extends Bundle {
  val pipe    = Flipped(new ExWbPipelineData) // Pipelined data from the Ex stage
  val memData = Input(UInt(32.W))             // Direct data from the Mem stage
}

class WriteBack extends Module {

  val io = IO(new Bundle {
    val ctrl    = Flipped(new ControlWriteback) // Control signals.
    val data    = new WbData()                  // Data to write back.
    val rfWrite = new RFWriteInterface          // Controler of the write input of the RF.
  })

  io.rfWrite.we := io.ctrl.weRf
  io.rfWrite.rw := io.data.pipe.rd

  io.rfWrite.w := MuxLookup(io.ctrl.wbSel, io.data.pipe.value)(Seq(
    WbMuxSel.MEM -> io.data.memData,
    WbMuxSel.CSR -> io.data.pipe.csrData
  ))

}
