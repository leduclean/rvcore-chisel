package projet
import projet.decode.Decode
import projet.WriteBack
import projet.Fetch
import projet.AluAMuxSel
import projet.AluBMuxSel
// import projet.WbMuxSel
import chisel3._

// Bundle to pipe datas from the decode/Exec/Mem
// stage to the write back stage.
class PipelineData extends Bundle {
  val rw = UInt(5.W)        // Write destination register
  val imm = UInt(32.W)      // Imm from the decode
  val aluRes  = UInt(32.W)  // result from Alu
  val memData = UInt(32.W)  // result from Mem
}

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
  // Reg read adress assignation from decode signals.
  rf.io.ra := decode.io.data.rs1
  rf.io.rb := decode.io.data.rs2

  val pcReg = RegNext(pc)
  val decodeData = decode.io.data
  val controls = decode.io.control

  // ---------------------------- EXE -------------------------
  val alu = Module(new Alu)

  // We execute the right operations, given the decode control signals.
  alu.io.a := Mux(controls.alu.aSel === AluAMuxSel.PC, pcReg, rf.io.a)
  alu.io.b := Mux(controls.alu.bSel === AluBMuxSel.IMM, decodeData.imm, rf.io.b)
  alu.io.fn := controls.alu.fn

  // We create a new data Bundle with alu result and MemData.
  val data = Wire(new PipelineData)
  data.rw := decodeData.rw
  data.imm := decodeData.imm
  data.aluRes := alu.io.result
  // We stub memData because we don't support Mem loading yet.
  data.memData := 0.U(32.W)

  // We pipeline the data Bundle for the write back stage.
  val dataReg = RegNext(data)
  val controlsReg = RegNext(controls)
  // ------------------------- WRITE BACK -------------------------
  val wb = Module(new WriteBack)

  wb.io.control <> controlsReg.wb
  wb.io.data <> dataReg

  // Write Back handle the write interface of
  // rf so we interconnect it.
  wb.io.rfWrite <> rf.io.rfWrite

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
