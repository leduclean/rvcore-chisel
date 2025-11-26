package projet

import chisel3._
import chisel3.util.Cat
import chisel3.util.Fill
import chisel3.util.MuxCase
import chisel3.util._

// Enum to cast the Csr command from func3(1,0).
object CsrCmd extends ChiselEnum {
  val MRET = Value(0.U) // Special command to comeback to before interupt state
  val W    = Value(1.U) // Read and write
  val S    = Value(2.U) // Read and set
  val C    = Value(3.U) // Read and clear
}

// Machine-level addr as specified in
// Table 2.5: Currently allocated RISC-V machine-level CSR addresses.
// of `riscv-priviledged`
object CsrAddr extends ChiselEnum {
  val default = Value(0.U(12.W)) // Default value to 0.U on 12 bits to hard set the enum width to 12
  // Machine Trap Set Up
  val mstatus = Value(0x300.U) // Machine status register (hardware and software)
  val mie     = Value(0x304.U) // Machine interrupt-enable register (software)
  val mtvec   = Value(0x305.U) // Machine trap-handler base address (software)

  // Machine trap handling
  val mepc   = Value(0x341.U) // Machine exception program counter (software)
  val mcause = Value(0x342.U) // Machine trap cause (hardware)
  val mip    = Value(0x344.U) // Machine interrupt pending (hardware)
}

// Machine Cause addr
// 3.1.15 Machine Cause Register
object McauseAddr {
  val External = 0x8000000bL.U
  val Software = 0x80000003L.U
  val Timer    = 0x80000007L.U
}

class MachineInteruptPendingSign extends Bundle {
  val meip = Input(Bool()) // Machine External interrupt pending
  val msip = Input(Bool()) // Machine Software interrupt pending
  val mtip = Input(Bool()) // Machine Timer Interrupt pending
}

class CsrData extends Bundle {
  val csrAddr = Input(CsrAddr())
  val imm     = Input(UInt(32.W))
  val rs1     = Input(UInt(32.W))
  val mepc      = Input(UInt(32.W))
}

class CsrIO extends Bundle {
  val mipSign = new MachineInteruptPendingSign()
  val data    = new CsrData
  val ctrl    = Input(new ControlCsr())
  val irq = Output(Bool())
  val pcNext  = Output(UInt(32.W))
  val readData  = Output(UInt(32.W))

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


  val regs = Seq(
    CsrAddr.mstatus -> mstatus,
    CsrAddr.mie     -> mie,
    CsrAddr.mtvec   -> mtvec,
    CsrAddr.mepc    -> mepc,
    CsrAddr.mcause  -> mcause,
    CsrAddr.mip     -> mip
  )

  // Hardware comparison betwen
  val csrWriteAllowed = MuxLookup(io.data.csrAddr, true.B)(Seq(
    CsrAddr.mip    -> false.B,
    CsrAddr.mcause -> false.B,
    CsrAddr.mepc   -> false.B
  ))

  val writeVal = Mux(io.ctrl.useImm, io.data.imm, io.data.rs1)

  // Always read
  io.readData := MuxLookup(io.data.csrAddr, 0.U(32.W))(regs)

  switch(io.ctrl.csrCmd) {
    is(CsrCmd.W) {
      // Write if permitted
      when(csrWriteAllowed) {
        for ((addr, reg) <- regs) {
          when(io.data.csrAddr === addr) {
            reg := writeVal
          }
        }
      }
    }
    is(CsrCmd.S) {
      // Set if permitted
      // Conditions are decrypted in
      // 6.1 riscv unprivileged Table 7
      when(csrWriteAllowed && (io.ctrl.useImm || io.data.rs1 =/= 0.U)) {
        for ((addr, reg) <- regs) {
          when(io.data.csrAddr === addr) {
            reg := reg | writeVal
          }
        }
      }
    }
    is(CsrCmd.C) {
      // Clear if permitted
      // Conditions are decrypted in
      // 6.1 riscv unprivileged Table 7
      when(csrWriteAllowed && (io.ctrl.useImm || io.data.rs1 =/= 0.U)) {
        for ((addr, reg) <- regs) {
          when(io.data.csrAddr === addr) {
            reg := reg & ~writeVal
          }
        }
      }
    }
  }

  // Interupt Controler
	// Write in the pending register (only hardware)
	mip := Cat(mip(31, 12), io.mipSign.meip, mip(10, 8), io.mipSign.mtip, mip(6, 4), io.mipSign.msip, mip(2, 0))

  // Bit position for mstatus machine enable signals.
  val MIE  = 3
  val MPIE = 7

  // Global machine interupt enable.
  val smie = mstatus(MIE)

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

  val interruptTrap = (smie & meie & meip) || // external interrupt
    (smie & mtie & mtip) || // timer interrupt
    (smie & msie & msip) // software interrupt3

  // previous state
	val interruptTrapPrev = RegNext(interruptTrap, init = false.B)
	// 1 cycle pulse when interruptTrapPrev goes up
	val irqPulse = interruptTrap && !interruptTrapPrev


  when(irqPulse) {
    // Handle interupt.
    mepc      := io.data.mepc
    io.pcNext := handlerAddr
    mcause    := MuxCase(
      McauseAddr.External,
      Seq(
        meip -> McauseAddr.External,
        msip -> McauseAddr.Software,
        mtip -> McauseAddr.Timer
      )
    )

    // We set the interrupt enable to false and we keep his value
    // in the pending interrupt enable.
    val swap = Csr.write(mstatus, MIE, false.B)
    mstatus := Csr.write(swap, MPIE, smie)
  }.elsewhen(io.ctrl.csrCmd === CsrCmd.MRET) {
    // We restore to the inital interrupt enable using the pending enable.
    val swap = Csr.write(mstatus, MIE, mstatus(MPIE))
    // Riscv convention to set back MPIE to 1.
    mstatus   := Csr.write(swap, MPIE, true.B)
    io.pcNext := mepc
  }.otherwise {
    // Defaults
    io.pcNext := io.data.mepc
  }

  io.irq := irqPulse

}

object Csr {

  // Writer to write to a specific byte
  def write(old: UInt, bit: Int, value: Bool): UInt =
    (old & ~(1.U << bit)) | (value.asUInt << bit)

}
