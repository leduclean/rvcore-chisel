package projet

import chisel3._
import chisel3.util._

/* \ Fusion de IP_LED et IP_PIN du projet VHDL \ */
class LedIp extends Module {

  val io = IO(new Bundle {
    val x31    = Input(UInt(32.W))
    val switch = Input(UInt(4.W))
    val led    = Output(UInt(4.W))
    val button = Input(UInt(3.W))
    val irq    = Output(Bool())
    val bus    = new BusInterface
  })

  io.bus.rdata := 0x01ed01ed.U // On lira ça si on lit !

  val dinReg = RegInit(0.U(32.W))
  val x31Reg = RegNext(io.x31) // On lit x31 en continue
  val btnReg = RegInit(0.U(32.W))
  val outReg = RegInit(0.U(32.W))

  btnReg       := Fill(29, 0.U) ## io.button
  io.bus.rdata := outReg

  // Le cpu fait une requête
  // On se moque de l'adresse ...
  // L'écriture écrit dans le registre, la lecture retourne l'état des boutons
  when(io.bus.en === true.B) {
    when(io.bus.be.asUInt.orR) { // Écriture dans le registre des leds internes
      dinReg := io.bus.wdata
    }.otherwise { // Lecture des boutons poussoirs, pas de débounce car lus par le soft
      when(io.bus.addr === 0.U) {
        outReg := dinReg
      }.otherwise {
        outReg := btnReg
      }
    }
  }

  // On échantillonne la donnée d'entrée en continue
  // C'est soit une donnée venant du registre x31 du
  // proc, si le dernier switch est à 1, soit le registre écrit par le soft
  val data = Mux(io.switch(3).asBool, x31Reg, dinReg)

  // On l'affiche
  io.led := MuxLookup(io.switch(2, 0), 0.U(4.W))(
    (0 until 8).map { i =>
      (i.U -> data((i * 4) + 3, i * 4))
    }
  )

//   On dirait que ça merde en simu (cela dit je n'ai pas compris
//   la logique, ...

// // Copie du VHDL de IP_ITPin : on s'assure juste que le PLIC
// // va bien prendre en compte cette interruption
  val regDelay     = RegNext(io.button.orR)
  val regNextDelay = RegNext(regDelay)
  val regIrq       = RegInit(false.B)
  val watCnt       = RegInit(0.U(4.W))

  when(!io.button.orR && !regDelay && regNextDelay) {
    watCnt := 0xf.U
  }.elsewhen(watCnt =/= 0.U) {
    watCnt := watCnt - 1.U
  }

  io.irq := Mux(watCnt =/= 0.U, 1.U, 0.U)

  // Pour la simu pour l'instant
  // io.irq := io.button.orR
}
