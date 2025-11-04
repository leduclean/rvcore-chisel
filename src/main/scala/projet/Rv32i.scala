package projet

import chisel3._

class Rv32i(sim: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val ibus = Flipped(new BusInterface)
    val dbus = Flipped(new BusInterface)
    val x31 = Output(UInt(32.W))
    val valid_x31  = if (sim)  Some(Output(Bool())) else None
  })

  val pc = RegInit(0.U(32.W))
  locally(sim)

  // Read access on instruction bus
  io.ibus.en := true.B
  io.ibus.be := VecInit.fill(4)(false.B)
  io.ibus.addr := pc

  val instr = io.ibus.rdata

  pc := pc + 4.U(32.W)
}
