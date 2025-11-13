package projet.decode

import chisel3._
import chisel3.util._
import projet.Control
import projet.WbMuxSel
import projet.AluAMuxSel
import projet.AluBMuxSel
import projet.FnAlu

// We define an object for typeU instruction.
object TypeU {
    private def getOp(instr: UInt): UInt = instr(6,0)
    private def getRd(instr: UInt): UInt = instr(11, 7)
    private def getImm(instr: UInt): UInt = Cat(instr(31, 12), 0.U(12.W))

    // Decode function for typeU instructions.
    def decode(instr: UInt): (Control, PipelineData) = {
        val ctrl = Wire(new Control)
        val data = Wire(new PipelineData)

        ctrl := 0.U.asTypeOf(new Control)
        data := 0.U.asTypeOf(new PipelineData)
        
        // We force to 31 for test simulation.
        data.rs1 := 31.U(5.W)

        // Invariants
        ctrl.wb.weRf := true.B
        data.rw := getRd(instr)
        data.imm := getImm(instr)

        switch (getOp(instr)) {
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

// We define an object for typeI instruction.
object TypeI {
    private def getOp(instr: UInt): UInt = instr(6,0)
    private def getRd(instr: UInt): UInt = instr(11, 7)
    private def getRs1(instr: UInt): UInt = instr(19, 15)
    // Sign extension to construct imm.
    private def getImm(instr: UInt): UInt = Cat(Fill(20, instr(31)), instr(31,20))
    private def func3(instr: UInt): UInt = instr(14, 12)


    // Decode function for typeI instructions.
    def decode(instr: UInt): (Control, PipelineData) = {
        val ctrl = Wire(new Control)
        val data = Wire(new PipelineData)

        // Default init
        ctrl := 0.U.asTypeOf(new Control)
        data := 0.U.asTypeOf(new PipelineData)

        // Invariants
        data.rw := getRd(instr)
        data.rs1 := getRs1(instr)
        ctrl.wb.weRf  := true.B

        switch (getOp(instr)) {
            is(OpCode.Imm) {
                ctrl.alu.aSel := AluAMuxSel.RFA
                ctrl.alu.bSel := AluBMuxSel.IMM
                ctrl.alu.fn := FnAluDecoder.fromFuncCode(func3(instr))
                ctrl.wb.wbSel := WbMuxSel.ALU
                data.imm := getImm(instr)
            }

            is (OpCode.Load) {
                // Not Yet implemented
            }

            is (OpCode.Jalr) {
                // Not Yet implemented
            }
        }
        (ctrl, data)
    }
}
