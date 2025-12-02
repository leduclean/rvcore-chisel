package projet

import chisel3._
import chisel3.util.MuxLookup
import projet.CompSignals

// Branch Type matching enum.
object BranchType extends ChiselEnum {
  val BEQ  = Value(0.U) // 000
  val BNE  = Value(1.U) // 001
  val BLT  = Value(4.U) // 100
  val BGE  = Value(5.U) // 101
  val BLTU = Value(6.U) // 110
  val BGEU = Value(7.U) // 111
}

class BranchIO extends Bundle {
  val ty          = Input(BranchType())      // Type of the branch.
  val aluSignals  = Input(new CompSignals()) // Comparison signals from the alu
  val branchTaken = Output(Bool())           // Branch taken output signal.
}

class BranchCond extends Module {
  val io = IO(new BranchIO)

  io.branchTaken := MuxLookup(io.ty, false.B)(Seq(
    BranchType.BEQ  -> io.aluSignals.eq,
    BranchType.BNE  -> !io.aluSignals.eq,
    BranchType.BLT  -> io.aluSignals.less,
    BranchType.BGE  -> !io.aluSignals.less,
    BranchType.BLTU -> io.aluSignals.uless,
    BranchType.BGEU -> !io.aluSignals.uless
  ))

}
