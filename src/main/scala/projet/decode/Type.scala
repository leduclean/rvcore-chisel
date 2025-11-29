package projet.decode

import chisel3._
import chisel3.util._
import projet.AluAMuxSel
import projet.AluBMuxSel
import projet.BranchType
import projet.Control
import projet.CsrAddr
import projet.CsrCmd
import projet.FnAlu
import projet.MemType
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
    data.rw  := getRd(instr)
    data.rs1 := getRs1(instr)

    switch(getOp(instr)) {
      is(OpCode.Imm) {
        // Detect shift instructions for IMM (SLLI, SRLI, SRAI).
        val isShift = func3(instr) === "b001".U || func3(instr) === "b101".U
        // Adapt Func code in consequence.
        val code = Mux(isShift, Cat(func7(instr), func3(instr)), func3(instr))
        ctrl.alu.aSel := AluAMuxSel.RFA
        ctrl.alu.bSel := AluBMuxSel.IMM

        // We cast the func code into Fn Alu type.
        val (fn, valid) = FnAlu.safe(code)
        ctrl.alu.fn     := fn
        ctrl.pc.invalid := !valid
        ctrl.wb.weRf    := valid
        ctrl.wb.wbSel   := WbMuxSel.ALU
        // We evaluate the immediate value differently if it's a shift.
        data.imm := Mux(isShift, shamt(instr), getImm(instr))
      }

      is(OpCode.Load) {
        // We use the alu to calculate the Addr
        ctrl.alu.aSel := AluAMuxSel.RFA
        ctrl.alu.bSel := AluBMuxSel.IMM

        // We cast the func code into MemSize type.
        // We use `safe` to return a `valid` boolean value
        // that indicate weither the variant size exists or not.
        val (ty, valid) = MemType.safe(func3(instr))
        ctrl.mem.ty := ty
        // We block memory access and RF write if invalid instr.
        ctrl.mem.en   := valid
        ctrl.wb.weRf  := valid
        ctrl.wb.wbSel := WbMuxSel.MEM

        ctrl.pc.invalid := !valid

        data.imm := getImm(instr)

      }

      is(OpCode.Jalr) {
        ctrl.alu.aSel  := AluAMuxSel.RFA
        ctrl.alu.bSel  := AluBMuxSel.IMM
        ctrl.alu.fn    := FnAlu.ADD
        ctrl.wb.wbSel  := WbMuxSel.PC4
        ctrl.wb.weRf   := true.B
        ctrl.pc.doJump := true.B
        data.imm       := getImm(instr)
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

  // Decode function for typeR instru  wb.io.data.memData := mem.io.out
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

    // We cast the func code into Fn Alu type.
    val (fn, valid) = FnAlu.safe(Cat(func7(instr), func3(instr)))
    ctrl.alu.fn     := fn
    ctrl.pc.invalid := !valid
    ctrl.wb.wbSel   := WbMuxSel.ALU
    ctrl.wb.weRf    := valid

    (ctrl, data)
  }

}

// We define an object for typeB instruction.
object TypeB {
  private def getRs1(instr: UInt): UInt = instr(19, 15)
  private def getRs2(instr: UInt): UInt = instr(24, 20)
  private def func3(instr: UInt): UInt  = instr(14, 12)

  // Sign extension to construct Imm.
  private def getImm(instr: UInt): UInt =
    Cat(Fill(19, instr(31)), instr(31), instr(7), instr(30, 25), instr(11, 8), 0.U(1.W))

  // Decode function for typeB instructions.
  def decode(instr: UInt): (Control, DecodeData) = {
    val ctrl = Wire(new Control)
    val data = Wire(new DecodeData)

    // Default init
    ctrl := 0.U.asTypeOf(new Control)
    data := 0.U.asTypeOf(new DecodeData)

    // Data
    data.rs1 := getRs1(instr)
    data.rs2 := getRs2(instr)
    data.imm := getImm(instr)

    // Control
    ctrl.pc.isBranchInstr := true.B
    ctrl.alu.aSel         := AluAMuxSel.RFA
    ctrl.alu.bSel         := AluBMuxSel.RFB

    // We cast the func code into Fn Alu type.
    val (ty, valid) = BranchType.safe(func3(instr))
    ctrl.branch.ty  := ty
    ctrl.pc.invalid := !valid
    (ctrl, data)
  }

}

// Object for type J instructions
object TypeJ {
  private def getRd(instr: UInt): UInt = instr(11, 7)

  private def getImm(instr: UInt): UInt = {
    Cat(Fill(12, instr(31)), instr(19, 12), instr(20), instr(30, 21), 0.U)
  }

  def decode(instr: UInt): (Control, DecodeData) = {
    val ctrl = Wire(new Control)
    val data = Wire(new DecodeData)

    // Default
    ctrl := 0.U.asTypeOf(new Control)
    data := 0.U.asTypeOf(new DecodeData)

    // rd = PC + 4
    data.rw  := getRd(instr)
    data.imm := getImm(instr)

    ctrl.alu.aSel  := AluAMuxSel.PC
    ctrl.alu.bSel  := AluBMuxSel.IMM
    ctrl.alu.fn    := FnAlu.ADD
    ctrl.wb.wbSel  := WbMuxSel.PC4
    ctrl.wb.weRf   := true.B
    ctrl.pc.doJump := true.B

    (ctrl, data)
  }

}

object TypeS {
  private def getRs1(instr: UInt): UInt = instr(19, 15)
  private def getRs2(instr: UInt): UInt = instr(24, 20)
  private def func3(instr: UInt): UInt  = instr(14, 12)
  private def getImm(instr: UInt): UInt = Cat(Fill(20, instr(31)), instr(31, 25), instr(11, 7))

  def decode(instr: UInt): (Control, DecodeData) = {
    val ctrl = Wire(new Control)
    val data = Wire(new DecodeData)

    ctrl := 0.U.asTypeOf(new Control)
    data := 0.U.asTypeOf(new DecodeData)

    data.rs1 := getRs1(instr)
    data.rs2 := getRs2(instr)
    data.imm := getImm(instr)

    ctrl.alu.aSel := AluAMuxSel.RFA
    ctrl.alu.bSel := AluBMuxSel.IMM
    ctrl.alu.fn   := FnAlu.ADD
    ctrl.wb.wbSel := WbMuxSel.ALU

    val (ty, valid) = MemType.safe(func3(instr))
    ctrl.mem.ty     := ty
    ctrl.wb.weRf    := false.B
    ctrl.pc.invalid := !valid

    ctrl.mem.we := valid
    ctrl.mem.en := valid

    (ctrl, data)
  }

}

object TypeCsr {
  private def getRd(instr: UInt): UInt  = instr(11, 7)
  private def getRs1(instr: UInt): UInt = instr(19, 15)
  private def func3(instr: UInt): UInt  = instr(14, 12)
  // Zero extension of the imm[4:0]
  private def getImm(instr: UInt): UInt = instr(19, 15).zext(32)
  private def getCsr(instr: UInt): UInt = instr(31, 20)
  private def isMRET(instr: UInt): UInt = instr(31, 20) === 0x302.U

  def decode(instr: UInt): (Control, DecodeData) = {
    val ctrl = Wire(new Control)
    val data = Wire(new DecodeData)

    // Default init
    ctrl := 0.U.asTypeOf(new Control)
    data := 0.U.asTypeOf(new DecodeData)

    // Data
    data.rw  := getRd(instr)
    data.rs1 := getRs1(instr)
    data.imm := getImm(instr)
    val (csrAddr, validAddr) = CsrAddr.safe(getCsr(instr))
    data.csrAddr := csrAddr

    // Control
    val f3 = func3(instr)
    // We can deduce the csrCmd only from the two lower bytes.
    val cmdCode = f3(1, 0)
    // MSB give the flag for IMM usage.
    val useImm = f3(2)

    // We cast the cmd code into Csr Cmd type.
    val (cmd, validCmd) = CsrCmd.safe(cmdCode)

    val ismret = isMRET(instr)
    when(cmd === CsrCmd.Default) {
      // The insruction is either an mret
      // or an invalid instruction
      ctrl.csr.mret   := ismret
      ctrl.pc.invalid := !ismret
    }.otherwise {
      ctrl.csr.csrCmd := cmd
      ctrl.csr.useImm := useImm
      ctrl.pc.invalid := !validCmd || !validAddr
      ctrl.wb.weRf    := true.B
      ctrl.wb.wbSel   := WbMuxSel.CSR
    }

    (ctrl, data)
  }

}
