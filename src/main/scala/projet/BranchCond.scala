package projet

import chisel3._
import chisel3.util.MuxCase
import projet.CompSignals

// Branch Type matchin enum.
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

  io.branchTaken := MuxCase(
    false.B,
    Seq(
      (io.ty === BranchType.BEQ)  -> io.aluSignals.eq,
      (io.ty === BranchType.BNE)  -> !io.aluSignals.eq,
      (io.ty === BranchType.BLT)  -> io.aluSignals.less,
      (io.ty === BranchType.BGE)  -> !io.aluSignals.less,
      (io.ty === BranchType.BLTU) -> io.aluSignals.uless,
      (io.ty === BranchType.BGEU) -> !io.aluSignals.uless
    )
  )

}
