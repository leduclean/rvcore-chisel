
package projet.decode

import chisel3._
import chisel3.util._
import projet.Control

class DecodeData extends Bundle {
  val rw  = UInt(5.W)  // Write back destination
  val rs1 = UInt(32.W) // Register adress 1
  val rs2 = UInt(32.W) // Register adress 2
  val imm = UInt(32.W) // Extracted imm
}


// This module will be used for the whole DecodeExecMem pipeline stage.
class Decode extends Module {
  val io = IO(new Bundle {
    val instr = Input(UInt(32.W)) // Instruction to decode
    val control = new Control    // Control of the Decode stage
    val data = new DecodeData   // Data of the Decode stage
  })

  // Defaults
  io.control := 0.U.asTypeOf(new Control)
  io.data := 0.U.asTypeOf(new DecodeData)
  io.data.rs1 := 31.U

  val instrTy = InstrType.getType(io.instr)
  // Instruction matching.
  switch(instrTy) {
    is(InstrType.Nop) {
      // We do nothing because no instruction loaded yet.
    }
    is(InstrType.TypeU) {
      val (ctrl, data) = TypeU.decode(io.instr)
      io.control := ctrl
      io.data := data
    }
    is(InstrType.TypeI) {
      val (ctrl, data) = TypeI.decode(io.instr)
      io.control := ctrl
      io.data := data
    }
    is(InstrType.Invalid) {
      io.control.decode.invalid := true.B
    }
  }
}

// Decoder to get the Alu operation.
object FnAluDecoder {
  import projet.FnAlu

  // Get the FnAlu.Type from a func code.
  // Typical func codes:
  // - raw f3
  // - Cat(f7, f3)
  def fromFuncCode(code: UInt): FnAlu.Type = {
    val fn = WireDefault(FnAlu.Unvalid)
    // We try to down cast the code to our FnAlu Chisel enum.
    // If the cast is unvalid, we return the Unvalid behaviour.
    val (value, valid) = FnAlu.safe(code)
    when(valid) {
      fn := value
    }
    fn
  }
}
