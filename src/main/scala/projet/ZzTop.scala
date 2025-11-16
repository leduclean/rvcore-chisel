package projet

import chisel3._

class ZzTop(file: String = "", sim: Boolean = true) extends Module {

  val io = IO(new Bundle {
    // Interface debug minimale
    val switch = Input(UInt(4.W))
    val led    = Output(UInt(4.W))
    val push   = Input(UInt(3.W))
    // Debug simu pour ne pas s'embeter avec le switch dans le testbench
    val x31       = if (sim) Some(Output(UInt(32.W))) else None
    val valid_x31 = if (sim) Some(Output(Bool())) else None

    // Port vga
    val hs = Output(Bool())
    val vs = Output(Bool())
    val r  = Output(UInt(5.W))
    val g  = Output(UInt(6.W))
    val b  = Output(UInt(5.W))
  })

  val clkg = Module(new ClockGen(12, sim)) // ~ 10 MHz
  // clock et reset sont les signaux implicites
  clkg.io.clk_in1 := clock
  clkg.io.reset   := reset
  val rst = (reset.asBool || !clkg.io.locked).asAsyncReset

  // Ces deux IPs doivent tourner à 125 MHz
  val vmem = Module(new IDMem(sim, 17))
  vmem.io.clki := clkg.io.clk_out2 // Clock du pixel
  vmem.io.clkd := clkg.io.clk_out1 // Clock du reste du système
  val vgad = Module(new VgaIp)
  vgad.io.clk := clkg.io.clk_out2 // Clock pixel

  withClockAndReset(clkg.io.clk_out1, rst) {
    val interconnect = Module(new BusInterconnect)
    val core         = Module(new Rv32i(sim))
    val dmem         = Module(new IDMem(sim, 16, file))
    val ldip         = Module(new LedIp)
    val clnt         = Module(new CLint)

    dmem.io.clki := clkg.io.clk_out1 // Ou clock, non ?
    dmem.io.clkd := clkg.io.clk_out1 //

    ldip.io.switch := io.switch
    ldip.io.button := io.push
    ldip.io.x31    := core.io.x31
    io.led         := ldip.io.led

    if (sim) {
      io.x31.get       := core.io.x31
      io.valid_x31.get := core.io.valid_x31.get
    }

    interconnect.io.master <> core.io.dbus

    interconnect.io.dmem <> dmem.io.dbus
    interconnect.io.ldip <> ldip.io.bus
    interconnect.io.clnt <> clnt.io.bus
    interconnect.io.vmem <> vmem.io.dbus

    // connexion ad-hoc pour le bus d'instructions
    dmem.io.ibus <> core.io.ibus
    // connexion ad-hoc pour l'IP Vga
    vmem.io.ibus <> vgad.io.bus
    io.hs := vgad.io.hs
    io.vs := vgad.io.vs
    io.r  := vgad.io.r
    io.g  := vgad.io.g
    io.b  := vgad.io.b
  }

}
