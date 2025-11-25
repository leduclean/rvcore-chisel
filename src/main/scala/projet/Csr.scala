package projet

import chisel3._

// Enum to cast the Csr command from func3(1,0).
object CsrCmd extends ChiselEnum {
  val MRET = Value(0.U) // Special command to before interupt state
  val W = Value(1.U) // Read and write
  val S = Value(2.U) // Read and set
  val C = Value(3.U) // Read and clear
}

class CsrIO extends Bundle {
  val ctrl = Input(new ControlCsr())
  val csr  = Input(UInt(5.W))
  val imm  = Input(UInt(32.W))
  val rs1  = Input(UInt(32.W))
}

class Csr extends Module {
  val io = IO(new CsrIO())
}
