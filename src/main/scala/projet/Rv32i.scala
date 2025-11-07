package projet
import projet.decode.TypeU
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
  io.ibus.wdata := 0.U(32.W)

  val pc = RegInit(0.U(32.W))

  // ------------------------- FETCH -------------------------
  // Only after reset
  when (!reset.asBool) {
    // Read access on instruction bus
    io.ibus.addr := pc
    dontTouch(io.ibus.addr)
    io.ibus.en := true.B
    io.ibus.be := VecInit.fill(4)(false.B)

  } .otherwise {
    // We do nothing during reset
      io.ibus.addr := 0.U
      io.ibus.en   := false.B
      io.ibus.be   := VecInit.fill(4)(false.B)
  }

  val instr = io.ibus.rdata

  // log: only for DEBUG and Simulation
  if (sim) {
    val instr_log = Wire(UInt(32.W))
    instr_log := instr
    dontTouch(instr_log)
  }

  // ------------------------- DECODE -------------------------
  val is_lui = TypeU.is_lui(instr)
  val rd     = TypeU.rd(instr)
  val imm    = TypeU.imm(instr)

  // Pipeline of decoded writing signals
  val rd_w  = RegNext(rd)
  val imm_w = RegNext(imm)
  val was_lui  = RegNext(is_lui)



  // ------------------------- WRITE BACK -------------------------
  // Wire connexions for an LU instruction
  when (was_lui) {
    rf.io.rw := rd_w
    rf.io.w  := imm_w
    rf.io.we := was_lui

  } .otherwise {
    // We stub the connexions in all other case
    // because we only support LUI for now
    rf.io.rw := 0.U(5.W)
    rf.io.w  := 0.U(5.W)
    rf.io.we := false.B
  }

  // We connect to x_31 for testing support
  rf.io.ra := 31.U(5.W)
  // We stub rb
  rf.io.rb := 0.U(32.W)

  // We allignate valid to the signal for a x31 written value
  if (sim) io.valid_x31.get := rf.io.write_fire.get

  // We connect x31 to first reading port
  io.x31 := rf.io.a

  pc := pc + 4.U(32.W)

}