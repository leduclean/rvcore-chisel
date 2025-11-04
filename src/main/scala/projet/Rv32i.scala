package projet

import chisel3._

class Rv32i(sim: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val ibus = Flipped(new BusInterface)
    val dbus = Flipped(new BusInterface)
    val x31 = Output(UInt(32.W))
    val valid_x31  = if (sim)  Some(Output(Bool())) else None
  })

  // Connecting all wires
  io.dbus.en := false.B
  io.dbus.be := VecInit.fill(4)(false.B)
  io.dbus.addr := 0.U(32.W)
  io.dbus.wdata := 0.U(32.W)

  io.x31 := 0.U(32.W)
  io.valid_x31.foreach { x => x := true.B }

  io.ibus.wdata := 0.U(32.W)

  val pc = RegInit(0.U(32.W))
  locally(sim)

  // Read access on instruction bus
  io.ibus.en := true.B
  io.ibus.be := VecInit.fill(4)(false.B)
  io.ibus.addr := pc

  val instr = io.ibus.rdata

  io.x31 := instr

  pc := pc + 4.U(32.W)
}
