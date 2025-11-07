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
    // Output signal to signalize a write on register x31
    val write_fire = if (sim)  Some(Output(Bool())) else None
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

  val write_cond = io.we && (io.rw =/= 0.U)

  when(write_cond) {
    regs(io.rw) := io.w
  }

  if (sim){
    // The value is readable 1 cycle after it has been written
    val delayed_fire = RegNext(write_cond && io.rw === 31.U(5.W), false.B)
    io.write_fire.foreach(_ := delayed_fire)
  }
}