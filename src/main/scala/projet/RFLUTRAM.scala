package projet
import chisel3._

class RFLUTRAM(sim: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val ra = Input(UInt(5.W))
    val rb = Input(UInt(5.W))
    val rw = Input(UInt(5.W))
    val w = Input(UInt(32.W))
    val we = Input(Bool())
    val a = Output(UInt(32.W))
    val b = Output(UInt(32.W))
  })

  val regs = Mem(32, UInt(32.W))
  if (sim) {
    when(reset.asBool) {
      regs(0.U) := 0.U
    }
    io.b := regs(io.rb)
    io.a := regs(io.ra)
  } else {
    io.b := Mux(io.rb=== 0.U, 0.U, regs(io.rb))
    io.a := Mux(io.ra=== 0.U, 0.U, regs(io.ra))
  }

  when(io.we && (io.rw =/= 0.U)) {
    regs(io.rw) := io.w
  }
}