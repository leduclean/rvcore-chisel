package projet

import chisel3._
import chisel3.util._
import projet.ControlWriteback
import projet.PipelineData
import projet.WbMuxSel

class WriteBack extends Module {

  val io = IO(new Bundle {
    val control = Flipped(new ControlWriteback) // Control signals.
    val data    = Flipped(new PipelineData)     // Data to write back.
    val rfWrite = new RFWriteInterface          // Controler of the write input of the RF.
  })

  // Defaults
  io.rfWrite := 0.U.asTypeOf(new RFWriteInterface)

  switch(io.control.wbSel) {
    is(WbMuxSel.INV) {
      // We don't write back if it's an invalid instruction.
    }
    is(WbMuxSel.IMM) {
      io.rfWrite.we := io.control.weRf
      io.rfWrite.rw := io.data.rw
      io.rfWrite.w  := io.data.imm
    }
    is(WbMuxSel.ALU) {
      io.rfWrite.we := io.control.weRf
      io.rfWrite.rw := io.data.rw
      io.rfWrite.w  := io.data.aluRes
    }
    is(WbMuxSel.MEM) {
      // Not implemented
    }
  }

}
