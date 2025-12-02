package projet

import chisel3._

// Alu mux for A input.
// (Chisel Enum as a bit wrapper to clarify the meaning of the signal)
object AluAMuxSel extends ChiselEnum {
  val PC, RFA = Value
}

// Alu mux for B input.
// (Chisel Enum as a bit wrapper to clarify the meaning of the signal)
object AluBMuxSel extends ChiselEnum {
  val IMM, RFB = Value
}

// Control signals from the Decode Unit for the ALU operation.
class ControlAlu extends Bundle {
  val aSel = Output(AluAMuxSel())
  val bSel = Output(AluBMuxSel())
  val fn   = Output(FnAlu())
}

// Control signals from the Decode Unit for the branch cond computation.
class ControlBranch extends Bundle {
  val ty = Output(BranchType())
}

// Control signals from Decode Unit for Csr computation
class ControlCsr extends Bundle {
  val csrCmd = Output(CsrCmd())
  val useImm = Output(Bool())
  val mret   = Output(Bool())
}

// Control signals from Decode Unit for Memory operations.
class ControlMem extends Bundle {
  val ty = Output(MemType())
  val en = Output(Bool())
  val we = Output(Bool())
}

object WbMuxSel extends ChiselEnum {
  val IMM, ALU, MEM, PC4, CSR = Value
}

// Controls signals from Decode Unit for PC update decision.
class ControlPc extends Bundle {
  val isBranchInstr = Bool()
  val doJump        = Bool()
  val invalid       = Bool()
}

// Control signals from Decode Unit for Write Back stage.
class ControlWriteback extends Bundle {
  val weRf  = Output(Bool())     // write enable
  val wbSel = Output(WbMuxSel()) // write back selector
}

// Global pipelined output signals from the Decode stage.
class Control extends Bundle {
  // Direct signals for the ALU
  val alu    = new ControlAlu
  val branch = new ControlBranch
  val csr    = new ControlCsr
  val mem    = new ControlMem
  val pc     = new ControlPc
  // Pipelined output signals
  val wb = new ControlWriteback
}
