package projet

import chisel3._
import projet.BusInterface
import projet.ControlFetch

// Fetch pipeline stage
class Fetch extends Module {

  val io = IO(new Bundle {
    val pc      = Input(UInt(32.W))
    val ibus    = Flipped(new BusInterface)
    val control = Flipped(new ControlFetch)
    val instr   = Output(UInt(32.W))
  })

  io.ibus.wdata := 0.U(32.W)

  // Only after reset
  when(!reset.asBool) {
    // Read access on instruction bus
    io.ibus.addr := io.pc
    io.ibus.en   := true.B
    io.ibus.be   := VecInit.fill(4)(false.B)

  }.otherwise {
    // We do nothing during reset
    io.ibus.addr := 0.U
    io.ibus.en   := false.B
    io.ibus.be   := VecInit.fill(4)(false.B)
  }

  io.instr := io.ibus.rdata
}
