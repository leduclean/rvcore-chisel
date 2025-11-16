package projet

import chisel3._
import chisel3.util._

// UInt Wrapper of the operations of Alu.
// We use it to Cast UInt into clear types of enum variant.
object FnAlu extends ChiselEnum {
  // Non exaustive
  val ADD  = Value(0.U) // ADDI/ADD LB/LH/LW/LBU/LHU, JALR
  val SLL  = Value(1.U) // SLLI/SLL
  val SLT  = Value(2.U) // SLTI/SLT
  val SLTU = Value(3.U) // SLTIU/SLTU
  val XOR  = Value(4.U) // XORI/XOR
  val SRL  = Value(5.U) // SRLI/SRL
  val OR   = Value(6.U) // ORI/OR
  val AND  = Value(7.U) // ANDI/AND

  // Special values determined by the single f7 byte code (instr(30)).
  // We concatenate f7 single byte code and f3 byte code to obtain those.
  val SUB     = Value(8.U)  // SUB
  val SRA     = Value(13.U) // SRAI/SRA
  val Unvalid = Value(15.U) // Not supported
}

// Alu object.
class Alu extends Module {

  val io = IO(new Bundle {
    val fn     = Input(FnAlu())     // Control signal to match the operation.
    val a      = Input(UInt(32.W))  // Upper input of the Alu.
    val b      = Input(UInt(32.W))  // Bottom input of the Alu.
    val result = Output(UInt(32.W)) // Result from the Alu.
  })

  // Always give a default value
  io.result := 0.U

  switch(io.fn) {

    is(FnAlu.ADD) {
      io.result := io.a + io.b
    }

    is(FnAlu.SLT) {
      io.result := (io.a.asSInt < io.b.asSInt).asUInt
    }

    is(FnAlu.SLTU) {
      io.result := (io.a < io.b).asUInt
    }

    is(FnAlu.XOR) {
      io.result := io.a ^ io.b
    }

    is(FnAlu.OR) {
      io.result := io.a | io.b
    }

    is(FnAlu.AND) {
      io.result := io.a & io.b
    }
  }

}
