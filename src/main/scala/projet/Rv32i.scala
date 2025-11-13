package projet
import projet.decode.Decode
import projet.WriteBack
import projet.Fetch
// import projet.WbMuxSel
import chisel3._

class Rv32i(sim: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val ibus = Flipped(new BusInterface)
    val dbus = Flipped(new BusInterface)
    val x31 = Output(UInt(32.W))
    val valid_x31  = if (sim)  Some(Output(Bool())) else None
  })

  // Registers module init
  val rf = Module(new RFLUTRAM(sim))

  // Connecting all wires
  io.dbus.en := false.B
  io.dbus.be := VecInit.fill(4)(false.B)
  io.dbus.addr := 0.U(32.W)
  io.dbus.wdata := 0.U(32.W)

  val pc = RegInit(0.U(32.W))

  // ------------------------- FETCH -------------------------
  val fetch = Module(new Fetch)
  fetch.io.pc := pc
  // Fetch handle the ibus so we interconnect the interface.
  fetch.io.ibus <> io.ibus
  val instr = fetch.io.instr

  // log: only for DEBUG and Simulation
  if (sim) {
    val instr_log = Wire(UInt(32.W))
    instr_log := instr
    dontTouch(instr_log)
  }

  // ------------------------- DECODE -------------------------
  val decode = Module(new Decode)
  decode.io.instr := instr

  // We pipeline the decoded writing signals.
  val dataReg = RegNext(decode.io.data)
  val controls = RegNext(decode.io.control)

    // ------------------------- WRITE BACK -------------------------
  val wb = Module(new WriteBack)

  wb.io.control <> controls.wb
  wb.io.data <> dataReg

  // Write Back handle the write interface of
  // rf so we interconnect it.
  wb.io.rfWrite <> rf.io.rfWrite

  // We connect to x_31 for testing support
  rf.io.ra := 31.U(5.W)
  // We stub rb
  rf.io.rb := 0.U(32.W)

  // We align valid to the signal for a x31 written value.
  if (sim) io.valid_x31.get := rf.io.write_fire.get

  // We connect x31 to first reading port
  io.x31 := rf.io.a

  // Handle invalid instructions
  when (controls.decode.invalid) {
    pc := 0.U(32.W)
  } .otherwise {
    pc := pc + 4.U(32.W)
  }
}