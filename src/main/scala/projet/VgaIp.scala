package projet

import chisel3._
import chisel3.util._

class VgaIp extends Module {

  val io = IO(new Bundle {
    val clk = Input(Clock())
    val bus = Flipped(new BusInterface)
    val hs  = Output(Bool())
    val vs  = Output(Bool())
    val r   = Output(UInt(5.W))
    val g   = Output(UInt(6.W))
    val b   = Output(UInt(5.W))
  })

  withClock(io.clk) {
    val sync = Module(new GeneSync())
    io.hs := RegNext(sync.io.hsync)
    io.vs := RegNext(sync.io.vsync)
    val img_sync = RegNext(sync.io.img)
    val index    = RegNext(sync.io.x(2, 1))

    io.bus.en    := true.B
    io.bus.be    := VecInit(false.B, false.B, false.B, false.B)
    io.bus.wdata := 0.U
    val addr = 80.U * sync.io.y(8, 1) + sync.io.x(9, 3)
    // Adresse mot dans la ram vidéo
    io.bus.addr := addr(14, 0) ## 0.U(2.W)

    val pixel = MuxCase(
      0.U,
      (0 until 4).map { i =>
        (index === i.U) -> io.bus.rdata(8 * (3 - i) + 7, 8 * (3 - i))
      }
    )

    io.r := Mux(img_sync, pixel(7, 6) << 3, 0.U)
    io.g := Mux(img_sync, pixel(5, 3) << 3, 0.U)
    io.b := Mux(img_sync, pixel(2, 0) << 2, 0.U)
  }

}
