package projet
import chisel3._
// UInt Wrapper of the operations of Alu.
// We use it to Cast UInt into clear types of enum variant.
object FnAlu extends ChiselEnum {
    // Non exaustive
    val ADD  = Value(0.U)  // ADDI/ADD LB/LH/LW/LBU/LHU, JALR
    val SLT  = Value(2.U)  // SLTI/SLT
    val SLTU = Value(3.U)  // SLTIU/SLTU
    val XOR  = Value(4.U)  // XORI/XOR
    val OR   = Value(6.U)  // ORI/OR
    val AND  = Value(7.U)  // ANDI/AND

    val Unvalid = Value(15.U) // Not supported
}
