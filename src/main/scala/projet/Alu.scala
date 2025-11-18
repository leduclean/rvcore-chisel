package projet

import chisel3._
import chisel3.util._

// UInt Wrapper of the operations of Alu.
// We use it to Cast UInt into clear types of enum variant.
object FnAlu extends ChiselEnum {
  // Non exaustive
  val ADD  = Value(0.U) // 0000 - ADDI/ADD LB/LH/LW/LBU/LHU, JALR
  val SLL  = Value(1.U) // 0001 - SLLI/SLL
  val SLT  = Value(2.U) // 0010 - SLTI/SLT
  val SLTU = Value(3.U) // 0011 - SLTIU/SLTU
  val XOR  = Value(4.U) // 0100 - XORI/XOR
  val SRL  = Value(5.U) // 0101 - SRLI/SRL
  val OR   = Value(6.U) // 0110 - ORI/OR
  val AND  = Value(7.U) // 0111 - ANDI/AND

  // Special values determined by the single f7 byte code (instr(30)).
  // We concatenate f7 single byte code and f3 byte code to obtain those.
  val SUB     = Value(8.U)  // 1000 - SUB
  val SRA     = Value(13.U) // 1101 - SRAI/SRA
  val Unvalid = Value(15.U) // 1111 - Not supported
}

class AluInput extends Bundle {
  val fn = Input(FnAlu())    // Operation control signal
  val a  = Input(UInt(32.W)) // Upper input
  val b  = Input(UInt(32.W)) // Lower input
}

class AddSub extends Module {

  val io = IO(new Bundle {
    val input  = new AluInput()
    val result = Output(UInt(32.W))
  })

  when(io.input.fn === FnAlu.ADD) {
    io.result := io.input.a + io.input.b
  }.otherwise {
    io.result := io.input.a - io.input.b
  }

}

class Logic extends Module {

  val io = IO(new Bundle {
    val input  = new AluInput()
    val result = Output(UInt(32.W))
  })

  io.result := MuxCase(
    0.U,
    Seq(
      (io.input.fn === FnAlu.OR)  -> (io.input.a | io.input.b),
      (io.input.fn === FnAlu.AND) -> (io.input.a & io.input.b),
      (io.input.fn === FnAlu.XOR) -> (io.input.a ^ io.input.b)
    )
  )

}

class Comp extends Module {

  val io = IO(new Bundle {
    val input  = new AluInput()
    val result = Output(UInt(32.W))
  })

  when(io.input.fn === FnAlu.SLT) {
    val ai = io.input.a.asSInt
    val bi = io.input.b.asSInt

    io.result := Mux(ai < bi, 1.U(32.W), 0.U(32.W))
  }.otherwise {
    io.result := Mux(
      io.input.a < io.input.b,
      1.U(32.W),
      0.U(32.W)
    )
  }

}

class Shift extends Module {

  val io = IO(new Bundle {
    val input  = new AluInput()
    val result = Output(UInt(32.W))
  })

  val tb = io.input.b(4, 0)

  io.result := MuxCase(
    0.U,
    Seq(
      (io.input.fn === FnAlu.SLL) -> (io.input.a << tb),
      (io.input.fn === FnAlu.SRL) -> (io.input.a >> tb),
      (io.input.fn === FnAlu.SRA) -> {
        val ai = io.input.a.asSInt
        val ri = ai >> tb

        ri.asUInt
      }
    )
  )

}

// Alu object.
class Alu extends Module {

  val io = IO(new Bundle {
    val input  = new AluInput()
    val result = Output(UInt(32.W))
  })

  val addsub = Module(new AddSub)
  val logic  = Module(new Logic)
  val comp   = Module(new Comp)
  val shift  = Module(new Shift)

  addsub.io.input := io.input
  logic.io.input  := io.input
  comp.io.input   := io.input
  shift.io.input  := io.input

  // default value
  io.result := 0.U(32.W)

  switch(io.input.fn) {
    is(FnAlu.ADD) {
      io.result := addsub.io.result
    }
    is(FnAlu.SUB) {
      io.result := addsub.io.result
    }
    is(FnAlu.SLT) {
      io.result := comp.io.result
    }
    is(FnAlu.SLTU) {
      io.result := comp.io.result
    }
    is(FnAlu.XOR) {
      io.result := logic.io.result
    }
    is(FnAlu.OR) {
      io.result := logic.io.result
    }
    is(FnAlu.AND) {
      io.result := logic.io.result
    }
    is(FnAlu.SLL) {
      io.result := shift.io.result
    }
    is(FnAlu.SRL) {
      io.result := shift.io.result
    }
    is(FnAlu.SRA) {
      io.result := shift.io.result
    }
  }

}
