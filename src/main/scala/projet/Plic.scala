package projet

import chisel3._
import chisel3.util._

/* \ Version très ad-hoc du plic \ */
class Plic extends Module {

  val io = IO(new Bundle {
    val bus  = new BusInterface
    val irq  = Input(Bool())
    val meip = Output(Bool())
  })

  // Champs de bits
  // Le bit 0 vaut toujours 0 "(interrupt source 0 does not exist)"
  val PENDING_ADDR = 0x1000
  val ENABLE_ADDR  = 0x2000
  // Id de l'interruption la plus prioritaire, 0 si aucune
  val CLAIM_ADDR = 0x200004

  val regPending = RegInit(0.U(32.W))
  val regEnable  = RegInit(0.U(32.W))
  val regClaim   = RegInit(0.U(32.W))
  val regIrq     = RegInit(false.B)
  // Nécessaire pour répondre dans le cycle et faire baisser mip
  // dans le processeur si le claim est suivi immédiatement du mret
  val regIrq_d = WireInit(false.B)

  // Une seule interruption, sur le fil 1
  regIrq_d   := (io.irq.asUInt & ~regIrq) | regIrq
  regPending := Fill(30, 0.U) ## regIrq_d ## 0.U(1.W)
  regIrq     := regIrq_d

  // Claim contient en permanence l'Id de l'interruption la
  // plus prioritaire ou zéro : pas trop compliqué lorsqu'il
  // n'y en a qu'une sur le fil 1, ...
  when(regIrq) {
    regClaim := 1.U
  }.otherwise {
    regClaim := 0.U
  }

  // On généralise des fois qu'on doive supporter plusieurs its
  // dans le futur
  io.meip := (regPending & regEnable).orR

  // Latence d'un cycle pour la production de la valeur lue
  val regValue = RegInit(0.U(32.W))

  when(io.bus.en === true.B) {
    when(io.bus.be.asUInt.orR) { // Écriture
      when(io.bus.addr === ENABLE_ADDR.U) {
        regEnable := io.bus.wdata
      }.elsewhen(io.bus.addr === CLAIM_ADDR.U) {
        regClaim := io.bus.wdata
      }
    }.otherwise { // Lecture
      when(io.bus.addr === PENDING_ADDR.U) {
        regValue := regPending
      }.elsewhen(io.bus.addr === ENABLE_ADDR.U) {
        regValue := regEnable
      }.elsewhen(io.bus.addr === CLAIM_ADDR.U) {
        regValue := regClaim
        regIrq_d := 0.U
      }
    }
  }

  io.bus.rdata := regValue
}
