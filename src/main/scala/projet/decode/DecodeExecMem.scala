
package projet.decode

import chisel3._
import chisel3.util._
import projet.Control
import projet.WbMuxSel


class PipelineData extends Bundle {
  val rw = Output(UInt(5.W)) // Write destination register
  val w = Output(UInt(32.W)) // Write back data
}


// This module will be used for the whole decode pipeline stage.
class DecodeUnit extends Module {
  val io = IO(new Bundle {
    val instr = Input(UInt(32.W)) // Instruction to decode
    val control = new Control     // Control of the Decode stage
    val data = new PipelineData   // Data of the Decode stage
  })

  // Defaults
  io.control := 0.U.asTypeOf(new Control)
  io.data := 0.U.asTypeOf(new PipelineData)

  val instrTy = InstrType.getType(io.instr)

  // Instruction matching
  switch (instrTy) {
    is(InstrType.TypeU) {
      when (TypeU.is_lui(io.instr)) {
        io.data.rw := TypeU.rd(io.instr)
        io.data.w := TypeU.imm(io.instr)
        io.control.wb.wbSel := WbMuxSel.IMM
        io.control.wb.weRf := true.B
      }
    }
    is(InstrType.Default) {
      io.data.rw := 0.U(5.W)
      io.data.w := 0.U(32.W)
      io.control.wb.wbSel := WbMuxSel.INV
      io.control.wb.weRf := true.B
    }
  }
}
