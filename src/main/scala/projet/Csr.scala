package projet

import chisel3._
import chisel3.util.Cat
import chisel3.util.Fill

// Enum to cast the Csr command from func3(1,0).
object CsrCmd extends ChiselEnum {
  val MRET = Value(0.U) // Special command to before interupt state
  val W = Value(1.U) // Read and write
  val S = Value(2.U) // Read and set
  val C = Value(3.U) // Read and clear
}

// Machine-level addr as specified in
// Table 2.5: Currently allocated RISC-V machine-level CSR addresses.
// of `riscv-priviledged`
object CsrAddr extends ChiselEnum {
  val default = Value(0.U) // Default value to 0.U to use
  // Machine Trap Set Up
  val mstatus = Value(0x300.U) // Machine status register (hardware and software)
  val mie     = Value(0x304.U) // Machine interrupt-enable register (software)
  val mtvec   = Value(0x305.U) // Machine trap-handler base address (software)

  // Machine trap handling
  val mepc   = Value(0x341.U) // Machine exception program counter (software)
  val mcause = Value(0x342.U) // Machine trap cause (hardware)
  val mip    = Value(0x344.U) // Machine interrupt pending (hardware)
}

class MachineInteruptPendingSign extends Bundle {
  val meip = Input(Bool()) // Machine External interrupt pending
  val msip = Input(Bool()) // Machine Software interrupt pending
  val mtip = Input(Bool()) // Machine Timer Interrupt pending
}

class CsrData extends Bundle {
  val csrAddr = Input(UInt(12.W))
  val imm     = Input(UInt(32.W))
  val rs1     = Input(UInt(32.W))
}

class CsrIO extends Bundle {
  val mipSign = new MachineInteruptPendingSign()
  val data    = new CsrData
  val ctrl    = Input(new ControlCsr())
}

class Csr extends Module {
  val io = IO(new CsrIO())

  // Registers

  // Core status
  val mstatus = RegInit(0.U(32.W))
  val mie     = RegInit(0.U(32.W))
  val mtvec   = RegInit(0.U(32.W))

  val mepc   = RegInit(0.U(32.W))
  val mcause = RegInit(0.U(32.W))
  val mip    = RegInit(0.U(32.W))

  // We keep only the bit relative
  // to an external maching interrupt cause.

  // Global machine interupt enable.
  val smie = mstatus(3)

  // External machine interupt enable.

  val meie = mie(11) // machine external interrupts
  val mtie = mie(7)  // machine time interrupts
  val msie = mie(3)  // machine software interrupts

  // External machine interupt state

  val meip = mip(11) // machine external interrupts
  val mtip = mip(7)  // machine time interrupts
  val msip = mip(3)  // machine software interrupts

  // The handler addr is a multiple of 4 so first two bytes should be 0.
  val handlerAddr = Cat(mtvec(31, 2), Fill(2, 0.U))

  val interuptTrap = (smie & meie & meip) || // external interrupt
    (smie & mtie & mtip) || // timer interrupt
    (smie & msie & msip) // software interrupt3

  // Write in the pending register (only hardware)
  mip := Cat(mip(31, 12), io.mipSign.meip, mip(10, 8), io.mipSign.mtip, mip(6, 4), io.mipSign.msip, mip(2, 0))

}
