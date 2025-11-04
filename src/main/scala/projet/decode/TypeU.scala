package projet.decode

import chisel3._
import chisel3.util._

// We define an object for typeU instruction
object TypeU {
    def rd(instr: UInt): UInt = instr(11, 7)
    def imm(instr: UInt): UInt = Cat(instr(31, 12), 0.U(12.W))
}