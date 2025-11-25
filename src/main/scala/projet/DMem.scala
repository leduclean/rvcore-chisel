package projet

import chisel3._
import chisel3.util.Cat
import chisel3.util.MuxLookup

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

  // Select the right byte when reading the 32 bits rdata output from IMEM.
  def selectByte(rdata: UInt, offset: UInt, signed: Boolean): UInt = {
    val b = MuxLookup(offset, rdata(7, 0))(Seq(
      0.U -> rdata(7, 0),
      1.U -> rdata(15, 8),
      2.U -> rdata(23, 16),
      3.U -> rdata(31, 24)
    ))
    if (signed) b.asSInt.pad(32).asUInt else b.pad(32)
  }

  // Select the right half word when reading the 32 bits rdata output from IMEM.
  def selectHalf(rdata: UInt, hi: Bool, signed: Boolean): UInt = {
    val h = Mux(hi, rdata(31, 16), rdata(15, 0))
    // sign/zero-extension
    if (signed) h.asSInt.pad(32).asUInt else h.pad(32)
  }

  // Replicate the data for half and bytes input.
  def makeWData(value: UInt, ty: MemType.Type): UInt = MuxLookup(ty, value)(Seq(
    MemType.B -> Cat(Seq.fill(4)(value(7, 0))),
    MemType.H -> Cat(Seq.fill(2)(value(15, 0)))
  ))

  def makeBE(addr: UInt, ty: MemType.Type): Vec[Bool] = MuxLookup(ty, VecInit.fill(4)(false.B))(Seq(
    MemType.B -> VecInit(Seq(
      addr(1, 0) === 0.U,
      addr(1, 0) === 1.U,
      addr(1, 0) === 2.U,
      addr(1, 0) === 3.U
    )),
    MemType.H -> VecInit(Seq(
      addr(1) === 0.U,
      addr(1) === 0.U,
      addr(1) === 1.U,
      addr(1) === 1.U
    )),
    MemType.W -> VecInit.fill(4)(true.B)
  ))

  // Default
  io.dbus.en    := false.B
  io.dbus.be    := VecInit.fill(4)(false.B)
  io.dbus.addr  := 0.U(32.W)
  io.dbus.wdata := 0.U(32.W)

  io.dbus.addr := io.addr
  io.dbus.en   := io.ctrl.en

  when(io.ctrl.we) {
    io.dbus.wdata := makeWData(io.din, io.ctrl.ty)
    io.dbus.be    := makeBE(io.addr, io.ctrl.ty)
  }

  // register the mem-type and addr to match pipeline timing
  // when selecting the data.
  val tyReg   = RegNext(io.ctrl.ty)
  val addrReg = RegNext(io.addr)
  val rdata   = io.dbus.rdata

  io.out := MuxLookup(tyReg, 0.U)(Seq(
    MemType.B  -> selectByte(rdata, addrReg(1, 0), signed = true),
    MemType.BU -> selectByte(rdata, addrReg(1, 0), signed = false),
    MemType.H  -> selectHalf(rdata, addrReg(1), signed = true),
    MemType.HU -> selectHalf(rdata, addrReg(1), signed = false),
    MemType.W  -> io.dbus.rdata(31, 0)
  ))

}
