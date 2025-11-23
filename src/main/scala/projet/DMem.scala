package projet

import chisel3._

// UInt wrapper (Chisel Enum) to decode memSize from func3.
// It's ordered in order to correspond to the func3 code.
object MemType extends ChiselEnum {
  // LOAD/STORE
  val B = Value(0.U) // 000 - byte (8 bits)
  val H = Value(1.U) // 001 - halfword (16 bits)
  val W = Value(2.U) // 010 - word (32 bits)

  // Only for LOAD operations.
  val BU = Value(4.U) //  100 - byte(not /signed)
  val HU = Value(5.U) //  101 - halfword (16 bites) not signed
}

// IO for the DMEM module handler.
class DMemIO extends Bundle {
  val din  = Input(UInt(32.W))
  val dbus = Flipped(new BusInterface)
  val ctrl = Flipped(new ControlMem())
  val addr = Input(UInt(32.W))
  val out  = Output(UInt(32.W))
}

// Module to handle DMEM load/store.
class DMem extends Module {
  val io = IO(new DMemIO())

  // Default
  io.dbus.en    := false.B
  io.dbus.be    := VecInit.fill(4)(false.B)
  io.dbus.addr  := 0.U(32.W)
  io.dbus.wdata := 0.U(32.W)
  io.out        := 0.U(32.W)

  io.dbus.addr := io.addr
  io.dbus.en   := io.ctrl.en
}
