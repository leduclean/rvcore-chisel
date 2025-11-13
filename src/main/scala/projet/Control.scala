package projet

import chisel3._

object WbMuxSel extends ChiselEnum {
  val IMM, ALU, MEM, INV = Value
}

// The pipelined output signals from the Decode/Exec/Mem stage.
class Control extends Bundle {
    val wb = new ControlWriteback
    val fetch = new ControlFetch
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