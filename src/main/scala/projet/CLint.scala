package projet

import chisel3._

/* \ Le CLINT est un timer 64 bits avec comparateur et contient le registre d'interruption logicielle.
 * \ */
class CLint extends Module {

  val io = IO(new Bundle {
    val bus  = new BusInterface
    val mtip = Output(Bool())
    val msip = Output(Bool())
  })

  // Adresse de l'interruption logicielle
  val MSIP_ADDR = 0x0000
  // Adresses des deux registres 64 bits du composant
  val MTIMERCMP_ADDR = 0x4000
  val MTIMER_ADDR    = 0xbff8

  // Registre interne pour sw it
  val msip = RegInit(false.B)
  // La sortie recopie sa valeur en permanence
  io.msip := msip
  // Sur le rv32i le timer est constitué de 2 registres pour faire 64 bits
  val mtime_lo = RegInit(0.U(32.W))
  val mtime_hi = RegInit(0.U(32.W))

  // Free running, comme on dit chez nous !
  mtime_lo := mtime_lo + 1.U
  mtime_hi := Mux(mtime_lo + 1.U === 0.U, mtime_hi + 1.U, mtime_hi)

  // La comparaison nécessite également 2 registres, pour une
  // comparaison sur 64 bits si nécessaire
  val mtimecmp_lo = RegInit(0.U(32.W))
  val mtimecmp_hi = RegInit(0.U(32.W))
  // Sortie mtimer interrupt pending dont on se moque pour l'instant
  io.mtip := (mtime_hi > mtimecmp_hi) || ((mtime_hi === mtimecmp_hi) && (mtime_lo >= mtimecmp_lo))
  // Valeur par défaut sur le bus de lecture
  // avec toujours un cycle de latence
  val clintRegValue = RegInit(0.U(32.W))

  io.bus.rdata := clintRegValue

  when(io.bus.en === true.B) {
    when(io.bus.be.asUInt.orR) { // Écriture
      when(io.bus.addr === MSIP_ADDR.U) {
        msip := io.bus.wdata(0)
      }.elsewhen(io.bus.addr === MTIMERCMP_ADDR.U) {
        mtimecmp_lo := io.bus.wdata
      }.elsewhen(io.bus.addr === (MTIMERCMP_ADDR + 4).U) {
        mtimecmp_hi := io.bus.wdata
      }
    }.otherwise { // Lecture
      when(io.bus.addr === MSIP_ADDR.U) {
        clintRegValue := msip.asUInt
      }.elsewhen(io.bus.addr === MTIMER_ADDR.U) {
        clintRegValue := mtime_lo
      }.elsewhen(io.bus.addr === (MTIMER_ADDR + 4).U) {
        clintRegValue := mtime_hi
      }.elsewhen(io.bus.addr === MTIMERCMP_ADDR.U) {
        clintRegValue := mtimecmp_lo
      }.elsewhen(io.bus.addr === (MTIMERCMP_ADDR + 4).U) {
        clintRegValue := mtimecmp_hi
      }
    }
  }

}
