package projet.decode

import chisel3._
import chisel3.util._

// OpCode consts.
object OpCode {
  // Type U
  val Lui   = "b0110111".U
  val Auipc = "b0010111".U

  // Type I
  val Load   = "b0000011".U   // LB, LH, LW, LBU, LHU
  val Jalr   = "b1100111".U   // JALR
  val Imm    = "b0010011".U   // ADDI, SLTI, SLTIU, XORI, ORI, ANDI, SLLI, SRLI, SRAI
}

// Enum variants for instruction type.
object InstrType extends ChiselEnum {
  val TypeU, TypeI, Default = Value

  def getType(instr: UInt): InstrType.Type = {
    val op = instr(6,0)
    val t = WireDefault(Default)

    switch(op) {
      // Type U
      is(OpCode.Lui, OpCode.Auipc) {
        t := TypeU
      }
      // Type I
      is(OpCode.Load, OpCode.Jalr, OpCode.Imm) {
        t := TypeI
      }
    }
    t
  }
}