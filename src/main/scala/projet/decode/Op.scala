package projet.decode

import chisel3._
import chisel3.util._

// OpCode consts.
object OpCode {
  // No instr loaded
  val Nop = "b0000000".U
  // Type U
  val Lui   = "b0110111".U
  val Auipc = "b0010111".U

  // Type I
  val Load = "b0000011".U // LB, LH, LW, LBU, LHU
  val Jalr = "b1100111".U // JALR
  // Type I and IR
  val Imm = "b0010011".U // ADDI, SLTI, SLTIU, XORI, ORI, ANDI, SLLI, SRLI, SRAI

  // Type R
  val RType = "b0110011".U // ADD, SUB, SLL, SLT, SLTU, XOR, SRL, SRA, OR, AND

  // Type B
  val BType = "b1100011".U // BEQ, BNE, BLT, BGE, BLTU, BGEU
}

// Enum variants for instruction type.
object InstrType extends ChiselEnum {
  val TypeU, TypeI, TypeR, TypeB, Invalid, Nop = Value

  def getType(instr: UInt): InstrType.Type = {
    val op = instr(6, 0)
    val t  = WireDefault(Invalid)
    switch(op) {
      is(OpCode.Nop) {
        t := Nop
      }
      // Type U
      is(OpCode.Lui, OpCode.Auipc) {
        t := TypeU
      }
      // Type I
      is(OpCode.Load, OpCode.Jalr, OpCode.Imm) {
        t := TypeI
      }
      is(OpCode.RType) {
        t := TypeR
      }
      is(OpCode.BType) {
        t := TypeB
      }
    }
    t
  }

}
