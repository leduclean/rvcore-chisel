package projet

import chisel3._
import chisel3.util._

/* \ Interface chisel du fichier généré par le clock_wizard de Vivado pour générer une clock à 10 MHz Le nom *doit* être
 * celui qui est dans le systemverilog (qui est dans TP/common/clk_wiz_0_clk_wiz.sv) \ */
class clk_wiz extends BlackBox with HasBlackBoxResource {

  addResource("/vsrc/clk_wiz.v")

  val io = IO(new Bundle {
    val clk_in1  = Input(Clock())
    val reset    = Input(Reset())
    val clk_out1 = Output(Clock()) // 10 MHz
    val clk_out2 = Output(Clock()) // 125 MHz
    val locked   = Output(Bool())
  })

}

class ClockGen(val divBy: Int = 10, useSim: Boolean = false) extends Module {

  val io = IO(new Bundle {
    val clk_in1  = Input(Clock())
    val reset    = Input(Reset())
    val clk_out1 = Output(Clock())
    val clk_out2 = Output(Clock())
    val locked   = Output(Bool())
  })

  val maxCnt = (divBy - 1).U
  val midCnt = ((divBy / 2) - 1).U

  if (useSim) {
    val cntReg = RegInit(0.U(log2Ceil(divBy).W))
    cntReg := cntReg + 1.U
    val outReg = RegInit(false.B)
    val rstReg = RegInit(true.B)

    rstReg := Mux(io.reset.asBool, true.B, rstReg)

    io.locked := !rstReg

    when(cntReg === midCnt) {
      outReg := true.B
    }.elsewhen(cntReg === maxCnt) {
      outReg := false.B
      cntReg := 0.U
      rstReg := false.B
    }
    io.clk_out1 := outReg.asClock
    io.clk_out2 := io.clk_in1
  } else {
    val ckgen = Module(new clk_wiz)
    ckgen.io.clk_in1 := io.clk_in1
    ckgen.io.reset   := io.reset
    io.clk_out1      := ckgen.io.clk_out1
    io.clk_out2      := ckgen.io.clk_out2
    io.locked        := ckgen.io.locked
  }

}
