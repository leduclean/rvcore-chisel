package projet

import chisel3._
import chisel3.util._
import projet.AluAMuxSel
import projet.AluBMuxSel
import projet.DMem
import projet.Fetch
import projet.WriteBack
import projet.decode.Decode

// Bundle to pipe datas from the Decode/Exe
// stage to the write back stage.
class ExWbPipelineData extends Bundle {
  val rd      = UInt(5.W)  // Write destination register
  val value   = UInt(32.W) // result from Alu or IMM or PC4 extracted in EX
  val csrData = UInt(32.W) // readed data from the csr register
}

class Rv32i(sim: Boolean = true) extends Module {

  val io = IO(new Bundle {
    val meip      = Input(Bool())
    val mtip      = Input(Bool())
    val msip      = Input(Bool())
    val ibus      = Flipped(new BusInterface)
    val dbus      = Flipped(new BusInterface)
    val x31       = Output(UInt(32.W))
    val valid_x31 = if (sim) Some(Output(Bool())) else None
  })

  // Module init
  val decode = Module(new Decode)
  val branch = Module(new BranchCond)
  val alu    = Module(new Alu)
  val mem    = Module(new DMem)
  val csr    = Module(new Csr)
  val rf     = Module(new RFLUTRAM(sim))
  val wb     = Module(new WriteBack)
  val fetch  = Module(new Fetch)

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

  // ByPass value determination: depends on the origin of the previous write back computation.
  val bypassVal = MuxLookup(wbcontrolsReg.wbSel, dataExReg.value)(Seq(
    WbMuxSel.MEM -> mem.io.out,
    WbMuxSel.CSR -> dataExReg.csrData
  ))

  // ByPass for rs1 and rs2 (condition: previous instruction was a register write).
  val rs1ByPass = Mux(wbcontrolsReg.weRf && rf.io.ra === dataExReg.rd, bypassVal, rf.io.a)
  val rs2ByPass = Mux(wbcontrolsReg.weRf && rf.io.rb === dataExReg.rd, bypassVal, rf.io.b)

  // We execute the right operations, given the decode control signals.
  alu.io.input.a  := Mux(controls.alu.aSel === AluAMuxSel.PC, pcReg, rs1ByPass)
  alu.io.input.b  := Mux(controls.alu.bSel === AluBMuxSel.IMM, decodeData.imm, rs2ByPass)
  alu.io.input.fn := controls.alu.fn

  val exOutput = MuxLookup(controls.wb.wbSel, alu.io.result)(Seq(
    WbMuxSel.ALU -> alu.io.result,
    WbMuxSel.IMM -> decodeData.imm,
    // Jump case, we use Alu result for jump target
    // and we hardcode the pc + 4 to write back in rd.
    WbMuxSel.PC4 -> (pcReg + 4.U)
  ))

  // We pipeline rd from
  dataExReg.rd    := decodeData.rw
  dataExReg.value := exOutput

  // Branch cond computation.
  branch.io.ty         := controls.branch.ty
  branch.io.aluSignals := alu.io.signals

  val branchTaken  = (controls.pc.isBranchInstr && branch.io.branchTaken)
  val branchTarget = pcReg + decodeData.imm
  val jumpTarget   = alu.io.result

  // Csr feeding
  csr.io.ctrl         := controls.csr
  csr.io.data.csrAddr := decodeData.csrAddr
  csr.io.data.imm     := decodeData.imm
  csr.io.data.mepc    := pcReg
  csr.io.data.rs1     := rs1ByPass
  dataExReg.csrData   := csr.io.readData

  csr.io.mipSign.meip := io.meip
  csr.io.mipSign.msip := io.msip
  csr.io.mipSign.mtip := io.mtip

  val csrPc = csr.io.pcNext

  // Ex -> Fetch feeding

  // Default case: PC + 4
  fetch.io.pcSel    := PcSel.PC4
  fetch.io.pcTarget := pcReg + 4.U

  // Target priority is:
  // irq > (jump, branch, invalid) > PC + 4
  when(csr.io.irq | controls.csr.mret) {
    fetch.io.pcSel    := PcSel.IRQ
    fetch.io.pcTarget := csr.io.pcNext
  }.elsewhen(controls.pc.doJump) {
    fetch.io.pcSel    := PcSel.JUMP
    fetch.io.pcTarget := jumpTarget
  }.elsewhen(branchTaken) {
    fetch.io.pcSel    := PcSel.BRANCH
    fetch.io.pcTarget := branchTarget
  }.elsewhen(controls.pc.invalid) {
    fetch.io.pcSel    := PcSel.INVALID
    fetch.io.pcTarget := 0.U
  }

  // If we jump or we branch, the next instruction is going to be a Nop
  isNop := controls.pc.doJump | (controls.pc.isBranchInstr && branch.io.branchTaken) | csr.io.irq

  // Mem computation
  mem.io.dbus <> io.dbus
  mem.io.ctrl <> controls.mem
  mem.io.addr := exOutput
  mem.io.din  := rs2ByPass

  // ------------------------- WRITE BACK -------------------------

  wb.io.ctrl <> wbcontrolsReg
  wb.io.data.pipe <> dataExReg
  wb.io.data.memData := mem.io.out

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
