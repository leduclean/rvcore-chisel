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

// Contol signals from the Decode Unit.
class ControlAlu extends Bundle {
    val aSel = Output(AluAMuxSel())
    val bSel = Output(AluBMuxSel())
    val fn = Output(FnAlu())
}

object WbMuxSel extends ChiselEnum {
  val IMM, ALU, MEM, INV = Value
}

// The pipelined output signals from the Decode/Exec/Mem stage.
class Control extends Bundle {
    val alu = new ControlAlu
    val decode = new ControlDecode
    // Pipelined output signals
    val wb = new ControlWriteback
    val fetch = new ControlFetch
}

// Control signals for the Decode stage.
class ControlDecode extends Bundle {
  val invalid = Output(Bool())
}

// Control signals for the WriteBack stage.
class ControlWriteback extends Bundle {
    val weRf      = Output(Bool())   // write enable
    val wbSel   = Output(WbMuxSel()) // write back selector
}

// Control signals for the Fetch stage.
class ControlFetch extends Bundle {
    // not yet implemented
}