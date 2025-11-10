package projet.decode

import chisel3._
import chisel3.util._

// OpCode consts.
object OpCode {
    val Lui = "b0110111".U
    val Auipc = "b0010111".U
}

// Enum variants for instruction type.
object InstrType extends ChiselEnum {
    val TypeU, Default = Value


  def getType(instr: UInt): InstrType.Type = {
    val op = instr(6,0)
    // Default
    val t = WireDefault(Default)

    switch(op) {
      is(OpCode.Lui, OpCode.Auipc) {
        t := TypeU
      }
    }
    t
  }
}
