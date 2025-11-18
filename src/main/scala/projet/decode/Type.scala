package projet.decode

import chisel3._
import chisel3.util._
import projet.AluAMuxSel
import projet.AluBMuxSel
import projet.Control
import projet.FnAlu
import projet.WbMuxSel

// We define an object for typeU instruction.
object TypeU {
  private def getOp(instr: UInt): UInt  = instr(6, 0)
  private def getRd(instr: UInt): UInt  = instr(11, 7)
  private def getImm(instr: UInt): UInt = Cat(instr(31, 12), 0.U(12.W))

  // Decode function for typeU instructions.
  def decode(instr: UInt): (Control, DecodeData) = {
    val ctrl = Wire(new Control)
    val data = Wire(new DecodeData)

    ctrl := 0.U.asTypeOf(new Control)
    data := 0.U.asTypeOf(new DecodeData)

    // We force to 31 for test simulation.
    data.rs1 := 31.U(5.W)

    // Invariants
    ctrl.wb.weRf := true.B
    data.rw      := getRd(instr)
    data.imm     := getImm(instr)

    switch(getOp(instr)) {
      is(OpCode.Lui) {
        ctrl.wb.wbSel := WbMuxSel.IMM
      }
      is(OpCode.Auipc) {
        ctrl.alu.aSel := AluAMuxSel.PC
        ctrl.alu.bSel := AluBMuxSel.IMM
        ctrl.alu.fn   := FnAlu.ADD
        ctrl.wb.wbSel := WbMuxSel.ALU
      }
    }

    (ctrl, data)
  }

}

// We define an object for typeI and TypeIR instructions.
object TypeI {
  private def getOp(instr: UInt): UInt  = instr(6, 0)
  private def getRd(instr: UInt): UInt  = instr(11, 7)
  private def getRs1(instr: UInt): UInt = instr(19, 15)
  // Sign extension to construct imm.
  private def getImm(instr: UInt): UInt = Cat(Fill(20, instr(31)), instr(31, 20))
  private def func3(instr: UInt): UInt  = instr(14, 12)

  // Suport for shift Type (TypeIR) with IMM.
  private def shamt(instr: UInt): UInt = instr(24, 20)
  private def func7(instr: UInt): UInt = instr(30)

  // Decode function for typeI instructions.
  def decode(instr: UInt): (Control, DecodeData) = {
    val ctrl = Wire(new Control)
    val data = Wire(new DecodeData)

    // Default init
    ctrl := 0.U.asTypeOf(new Control)
    data := 0.U.asTypeOf(new DecodeData)

    // Invariants
    data.rw      := getRd(instr)
    data.rs1     := getRs1(instr)
    ctrl.wb.weRf := true.B

    switch(getOp(instr)) {
      is(OpCode.Imm) {
        // Detect shift instructions for IMM (SLLI, SRLI, SRAI).
        val isShift = func3(instr) === "b001".U || func3(instr) === "b101".U
        // Adapt Func code in consequence.
        val code = Mux(isShift, Cat(func7(instr), func3(instr)), func3(instr))
        ctrl.alu.aSel := AluAMuxSel.RFA
        ctrl.alu.bSel := AluBMuxSel.IMM
        ctrl.alu.fn   := FnAluDecoder.fromFuncCode(code)
        ctrl.wb.wbSel := WbMuxSel.ALU
        // We evaluate the immediate value differently if it's a shift.
        data.imm := Mux(isShift, shamt(instr), getImm(instr))
      }

      is(OpCode.Load) {
        // Not Yet implemented
      }

      is(OpCode.Jalr) {
        // Not Yet implemented
      }
    }
    (ctrl, data)
  }

}

// We define an object for typeR instruction.
object TypeR {
  private def getRd(instr: UInt): UInt  = instr(11, 7)
  private def getRs1(instr: UInt): UInt = instr(19, 15)
  private def getRs2(instr: UInt): UInt = instr(24, 20)
  private def func3(instr: UInt): UInt  = instr(14, 12)
  private def func7(instr: UInt): UInt  = instr(30)

  // Decode function for typeR instructions.
  def decode(instr: UInt): (Control, DecodeData) = {
    val ctrl = Wire(new Control)
    val data = Wire(new DecodeData)

    // Default init
    ctrl := 0.U.asTypeOf(new Control)
    data := 0.U.asTypeOf(new DecodeData)

    // Data
    data.rw  := getRd(instr)
    data.rs1 := getRs1(instr)
    data.rs2 := getRs2(instr)

    // Control
    ctrl.alu.aSel := AluAMuxSel.RFA
    ctrl.alu.bSel := AluBMuxSel.RFB
    ctrl.alu.fn   := FnAluDecoder.fromFuncCode(Cat(func7(instr), func3(instr)))
    ctrl.wb.wbSel := WbMuxSel.ALU
    ctrl.wb.weRf  := true.B

    (ctrl, data)
  }

}
