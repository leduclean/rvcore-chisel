package projet

import chisel3._
import projet.BusInterface

// Fetch pipeline stage
class Fetch extends Module {

  val io = IO(new Bundle {
    val ibus     = Flipped(new BusInterface)
    val pcSel    = Input(PcSel())
    val pcTarget = Input(UInt(32.W))
    val instr    = Output(UInt(32.W)) // Current instruction
    val pcOut    = Output(UInt(32.W)) // Pc of the current decoded instruction
  })

  val pc = RegInit(0.U(32.W))
  io.ibus.wdata := 0.U(32.W)

  // Only after reset
  when(!reset.asBool) {
    // Read access on instruction bus
    io.ibus.addr := pc
    io.ibus.en   := true.B
    io.ibus.be   := VecInit.fill(4)(false.B)
  }.otherwise {
    // We do nothing during reset
    io.ibus.addr := 0.U
    io.ibus.en   := false.B
    io.ibus.be   := VecInit.fill(4)(false.B)
  }

  // Read the instruction from the bus.
  io.instr := io.ibus.rdata
  // Give the pc from the current instruction.
  io.pcOut := pc

  // Compute next pc after loading an instruction depending
  // on selectors signals from EX.
  val nextPC = Mux(io.pcSel === PcSel.PC4, pc + 4.U, io.pcTarget)

  // Update pc
  pc := nextPC
}
