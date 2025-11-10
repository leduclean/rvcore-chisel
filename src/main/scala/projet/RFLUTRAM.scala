package projet
import chisel3._

class RFWriteInterface extends Bundle {
  val rw = Output(UInt(5.W))   // written register
  val we = Output(Bool())      // write enable
  val w  = Output(UInt(32.W))  // data to write
}

class RFLUTRAM(sim: Boolean = false) extends Module {
  val io = IO(new Bundle {
    // Write interface controled by WriteBack stage.
    val rfWrite = Flipped(new RFWriteInterface)
    val ra = Input(UInt(5.W))
    val rb = Input(UInt(5.W))
    val a = Output(UInt(32.W))
    val b = Output(UInt(32.W))
    // Output signal to signalize a write on register x31
    val write_fire = if (sim)  Some(Output(Bool())) else None
  })

  val regs = Mem(32, UInt(32.W))
  val destination = io.rfWrite.rw

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

  val write_cond = io.rfWrite.we && (destination =/= 0.U)

  when(write_cond) {
    regs(destination) := io.rfWrite.w
  }

  if (sim){
    // The value is readable 1 cycle after it has been written
    val delayed_fire = RegNext(write_cond && destination === 31.U(5.W), false.B)
    io.write_fire.foreach(_ := delayed_fire)
  }
}