package projet.decode

import chisel3._
import chisel3.util._
import projet.Control

class DecodeData extends Bundle {
  val rw  = UInt(5.W)  // Write back destination
  val rs1 = UInt(32.W) // Register adress 1
  val rs2 = UInt(32.W) // Register adress 2
  val imm = UInt(32.W) // Extracted imm
  val csr = UInt(12.W) // Csr source/dest
}

// This module will be used for the whole DecodeExecMem pipeline stage.
class Decode extends Module {

  val io = IO(new Bundle {
    val instr   = Input(UInt(32.W)) // Instruction to decode
    val control = new Control       // Control of the Decode stage
    val data    = new DecodeData    // Data of the Decode stage
  })

  // Defaults
  io.control := 0.U.asTypeOf(new Control)
  io.data    := 0.U.asTypeOf(new DecodeData)

  val instrTy = InstrType.getType(io.instr)

  // Instruction matching.
  switch(instrTy) {
    is(InstrType.Nop) {
      // We do nothing because no instruction loaded yet.
    }
    is(InstrType.TypeU) {
      val (ctrl, data) = TypeU.decode(io.instr)
      io.control := ctrl
      io.data    := data
    }
    is(InstrType.TypeI) {
      val (ctrl, data) = TypeI.decode(io.instr)
      io.control := ctrl
      io.data    := data
    }
    is(InstrType.TypeR) {
      val (ctrl, data) = TypeR.decode(io.instr)
      io.control := ctrl
      io.data    := data
    }
    is(InstrType.TypeB) {
      val (ctrl, data) = TypeB.decode(io.instr)
      io.control := ctrl
      io.data    := data
    }
    is(InstrType.TypeJ) {
      val (ctrl, data) = TypeJ.decode(io.instr)
      io.control := ctrl
      io.data    := data
    }
    is(InstrType.TypeS) {
      val (ctrl, data) = TypeS.decode(io.instr)
      io.control := ctrl
      io.data    := data
    }
    is(InstrType.Invalid) {
      io.control.pc.invalid := true.B
    }
  }

}
