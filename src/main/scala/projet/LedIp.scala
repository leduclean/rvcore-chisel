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

  val dinReg      = RegInit(0.U(32.W))
  val x31Reg      = RegNext(io.x31) // On lit x31 en continue
  val outReg      = RegInit(0.U(32.W))
  val capturedBtn = RegInit(0.U(32.W))

  val regDelay     = RegNext(io.button.orR)
  val regNextDelay = RegNext(regDelay)
  val watCnt       = RegInit(0.U(4.W))
  val bntReg = RegInit(0.U(4.W))

  val btnRaw0 = RegNext(io.button)
  val btnRaw1 = RegNext(btnRaw0)

  when(!io.button.orR && !regDelay && regNextDelay) {
    watCnt      := 0xf.U
    capturedBtn := Fill(29, 0.U) ## btnRaw1

  }.elsewhen(watCnt =/= 0.U) {
    watCnt := watCnt - 1.U
  }

  io.irq := watCnt =/= 0.U
  val currBtn = Fill(29, 0.U) ## io.button

  // Le cpu fait une requête
  // On se moque de l'adresse ...
  // L'écriture écrit dans le registre, la lecture retourne l'état des boutons
  when(io.bus.en === true.B) {
    when(io.bus.be.asUInt.orR) { // Écriture dans le registre des leds internes
      dinReg := io.bus.wdata
    }.otherwise { // Lecture des registre de boutons poussoirs, si différent de 0 alors on veut lire 
    // le bouton du state de l'interruption capturée
      bntReg := Mux(io.bus.addr === 0.U, currBtn, capturedBtn)
    }
  }
  io.bus.rdata := bntReg

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

}
