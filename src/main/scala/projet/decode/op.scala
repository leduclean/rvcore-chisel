package projet.decode

import chisel3._

// trait op instruction codes CONST
trait Op {
    val LUI = "b0110111".U
}