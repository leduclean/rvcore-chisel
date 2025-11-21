package projet

import chisel3._
import chisel3.util.Fill
import projet.AluAMuxSel
import projet.AluBMuxSel
import projet.Fetch
import projet.WriteBack
import projet.decode.Decode

// Bundle to pipe datas from the Decode/Exe
// stage to the write back stage.
class ExWbPipelineData extends Bundle {
  val rw     = UInt(5.W)  // Write destination register
  val imm    = UInt(32.W) // Imm from the decode
  val aluRes = UInt(32.W) // result from Alu
}

class Rv32i(sim: Boolean = true) extends Module {

  val io = IO(new Bundle {
    val ibus      = Flipped(new BusInterface)
    val dbus      = Flipped(new BusInterface)
    val x31       = Output(UInt(32.W))
    val valid_x31 = if (sim) Some(Output(Bool())) else None
  })

  // Module init
  val decode = Module(new Decode)
  val branch = Module(new BranchCond)
  val alu    = Module(new Alu)
  val rf     = Module(new RFLUTRAM(sim))
  val wb     = Module(new WriteBack)
  val fetch  = Module(new Fetch)

  // Connecting all wires
  io.dbus.en    := false.B
  io.dbus.be    := VecInit.fill(4)(false.B)
  io.dbus.addr  := 0.U(32.W)
  io.dbus.wdata := 0.U(32.W)

  // ------------------------- FETCH -------------------------
  fetch.io.ibus <> io.ibus
  val instr = fetch.io.instr
  // ------------------------- DECODE -------------------------
  // `isNop` depends of the previous instruction decode so we use a reg.
  val isNop    = RegInit(false.B)
  val nopInstr = Fill(32, 0.U)

  // If the previous instruction decoded was a branch
  // or a jump next instruction should be a `Nop`.
  val decodeInstr = Mux(isNop, nopInstr, instr)

  // log: only for DEBUG and Simulation
  if (sim) {
    val instr_log = Wire(UInt(32.W))
    instr_log := decodeInstr
    dontTouch(instr_log)
  }

  decode.io.instr := decodeInstr
  // We get the controls from the decode part.
  val controls = decode.io.control

  // Reg read adress assignation from decode signals.
  rf.io.ra := decode.io.data.rs1
  rf.io.rb := decode.io.data.rs2

// ---------------------------- EXE -------------------------

  // pcReg is the pc value from the current decoded instruction.
  val pcReg      = RegNext(fetch.io.pcOut)
  val decodeData = decode.io.data

  // Pipelined data between Execute and write back.
  val dataExReg = RegInit(0.U.asTypeOf(new ExWbPipelineData))
  // Pipelined controls between Execute and write back.
  val wbcontrolsReg = RegNext(controls.wb)

  // We execute the right operations, given the decode control signals.
  alu.io.input.a  := Mux(controls.alu.aSel === AluAMuxSel.PC, pcReg, rf.io.a)
  alu.io.input.b  := Mux(controls.alu.bSel === AluBMuxSel.IMM, decodeData.imm, rf.io.b)
  alu.io.input.fn := controls.alu.fn

  // We aliment the data from Ex
  dataExReg.rw     := decodeData.rw
  dataExReg.imm    := decodeData.imm
  dataExReg.aluRes := alu.io.result

  // We stub memData because we don't support Mem loading yet.
  val memData = 0.U(32.W)

  // Branch cond computation.
  branch.io.ty         := controls.branch.ty
  branch.io.aluSignals := alu.io.signals

  // Feed fetch PC ctrl with the jump and branch targets.
  fetch.io.branchTaken  := branch.io.branchTaken
  fetch.io.branchTarget := pcReg + decodeData.imm
  fetch.io.jumpTarget   := 0.U // TODO change to the right jump target.

  // Connect control signals.
  fetch.io.ctrl <> controls.pc

  // If we jump or we branch, the next instruction is going to be a Nop
  isNop := controls.pc.doJump | (controls.pc.isBranchInstr && branch.io.branchTaken)


  // ------------------------- WRITE BACK -------------------------

  wb.io.control <> wbcontrolsReg
  wb.io.data.pipe <> dataExReg
  wb.io.data.memData := memData

  // Write Back handle the write interface of
  // rf so we interconnect it.
  wb.io.rfWrite <> rf.io.rfWrite

  // We align valid to the signal for a x31 written value.
  if (sim) {
    io.valid_x31.get := rf.io.write_fire.get
    io.x31           := rf.io.x31.get
  } else {
    // Default
    io.x31 := 0.U
  }

}
