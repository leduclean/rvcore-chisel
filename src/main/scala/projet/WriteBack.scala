package projet

import projet.WbMuxSel
import projet.ControlWriteback
import projet.decode.PipelineData
import chisel3._
import chisel3.util._


class WriteBack extends Module {
    val io = IO(new Bundle{
        val control = Flipped(new ControlWriteback) // Control signals.
        val data = Flipped(new PipelineData) // Data to write back.
        val rfWrite = new RFWriteInterface // Controler of the write input of the RF.
    })

    // We stub the connexions in all other case
    // because we only support LUI for now
    io.rfWrite.rw := 0.U(5.W)
    io.rfWrite.w  := 0.U(5.W)
    io.rfWrite.we := false.B

    switch (io.control.wbSel) {
        is(WbMuxSel.IMM) {
            io.rfWrite.we := io.control.weRf
            io.rfWrite.rw := io.data.rw
            io.rfWrite.w := io.data.imm
        }
        is(WbMuxSel.INV) {
            io.rfWrite.we := false.B
        }
        is(WbMuxSel.ALU) {
        // Not implemented
        }
        is(WbMuxSel.MEM) {
        // Not implemented
        }
    }
}