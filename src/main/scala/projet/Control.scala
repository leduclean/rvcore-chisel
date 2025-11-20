package projet

import chisel3._

// We use ChiselEnum as a mux to simpify entry sel.
// Alu mux for A input.
object AluAMuxSel extends ChiselEnum {
  val PC, RFA = Value

}

// We use ChiselEnum as a mux to simpify entry sel
// Alu mux for B input.
object AluBMuxSel extends ChiselEnum {
  val IMM, RFB = Value
}

// Control signals from the Decode Unit.
class ControlAlu extends Bundle {
  val aSel = Output(AluAMuxSel())
  val bSel = Output(AluBMuxSel())
  val fn   = Output(FnAlu())
}

// Control signals from the Decode Unit for the branch cond computation.
class ControlBranch extends Bundle {
  val ty = Output(BranchType())
}

object WbMuxSel extends ChiselEnum {
  val IMM, ALU, MEM, INV = Value
}

// Controls signals for PC in fetch stage.
class ControlPc extends Bundle {
  val isBranchInstr = Bool()
  val doJump        = Bool()
  val invalid       = Bool()
}

// Control signals for the WriteBack stage.
class ControlWriteback extends Bundle {
  val weRf  = Output(Bool())     // write enable
  val wbSel = Output(WbMuxSel()) // write back selector
}

// Global pipelined output signals from the Decode stage.
class Control extends Bundle {
  // Direct signals for the ALU
  val alu    = new ControlAlu
  val branch = new ControlBranch
  val pc     = new ControlPc
  // Pipelined output signals
  val wb = new ControlWriteback
}
