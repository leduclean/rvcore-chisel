package projet

import chisel3._
import chisel3.util._

// Bus très simple : au temps t le maître fait une requête
// au temps t+1, il a la réponse, sinon c'est mort !

// Signaux utilisés par le bus, coté esclave, on fait un
// Flip pour l'avoir coté maître ensuite
class BusInterface extends Bundle {
  val addr  = Input(UInt(32.W))     // adresse cible de la transaction
  val wdata = Input(UInt(32.W))     // donnée en écriture (max 32 bits)
  val rdata = Output(UInt(32.W))    // donnée lue de l'esclave, avec 1 cycle de retard
  val be    = Input(Vec(4, Bool())) // byte-enable pour les accès écriture mémoire
  val en    = Input(Bool())         // enable de la mémoire, read si be.orR est égal à 0, écriture sinon
}

class BusInterconnect extends Module {

  val io = IO(new Bundle {
    val master = new BusInterface          // Bus avec un unique maître
    val dmem   = Flipped(new BusInterface) // et plusieurs esclaves
    val ldip   = Flipped(new BusInterface)
    val clnt   = Flipped(new BusInterface)
    val vmem   = Flipped(new BusInterface)
  })

  // On récupère l'adresse émise par le maître pour trouver à quel
  // esclave elle fait référence
  val addr = io.master.addr

  // On reprend nos bonnes vielles adresses, pas très efficaces, mais ça fait le job
  // -- Slave    0             1             2             3              4             5
  // -- Name     RAM prog  |   IP led    |   IP pin    |   IP PLIC   |   IP_CLINT   |   DDR
  // BASE => ( X"0000_1000", X"3000_0000", X"3000_0008", X"0C00_0000", X"0200_0000", X"8000_0000"),
  // HIGH => ( X"0000_8FFF", X"3000_0004", X"3000_0008", X"1000_0000", X"0200_C000", X"8FFF_FFFF")

  val DMEM_BASE = 0x00000000L.U // Une mémoire unifiée code + données
  val DMEM_SIZE = 0x00010000L.U

  val LDIP_BASE = 0x30000000L.U
  val LDIP_SIZE = 0x00000004L.U

  val CLNT_BASE = 0x02000000L.U
  val CLNT_SIZE = 0x0000c000L.U

  val VMEM_BASE = 0x80000000L.U // Mémoire vidéo dual port
  val VMEM_SIZE = 0x00020000L.U

  val is_dmem = addr >= DMEM_BASE && addr < (DMEM_BASE + DMEM_SIZE)
  val is_ldip = addr >= LDIP_BASE && addr < (LDIP_BASE + LDIP_SIZE)
  val is_clnt = addr >= CLNT_BASE && addr < (CLNT_BASE + CLNT_SIZE)
  val is_vmem = addr >= VMEM_BASE && addr < (VMEM_BASE + VMEM_SIZE)

  // On connecte directement ce qui doit l'être en faisant l'hypothèse que
  io.dmem.en := io.master.en && is_dmem
  io.ldip.en := io.master.en && is_ldip
  io.clnt.en := io.master.en && is_clnt
  io.vmem.en := io.master.en && is_vmem
  // la chose connectée à ses adresses qui commencent à zéro
  io.dmem.addr  := io.master.addr - DMEM_BASE
  io.ldip.addr  := io.master.addr - LDIP_BASE
  io.clnt.addr  := io.master.addr - CLNT_BASE
  io.vmem.addr  := io.master.addr - VMEM_BASE
  io.dmem.wdata := io.master.wdata
  io.ldip.wdata := io.master.wdata
  io.clnt.wdata := io.master.wdata
  io.vmem.wdata := io.master.wdata
  io.dmem.be    := io.master.be
  io.ldip.be    := io.master.be
  io.clnt.be    := io.master.be
  io.vmem.be    := io.master.be

  // On doit décaler d'un cycle (latence de l'accès mémoire)
  // les signaux qui multiplexent la donnée en sortie
  val is_pdmem = RegNext(is_dmem)
  val is_pldip = RegNext(is_ldip)
  val is_pclnt = RegNext(is_clnt)
  val is_pvmem = RegNext(is_vmem)

  io.master.rdata := MuxCase(
    0x0badaddL.U(32.W),
    Seq(
      is_pdmem -> io.dmem.rdata,
      is_pldip -> io.ldip.rdata,
      is_pclnt -> io.clnt.rdata,
      is_pvmem -> io.vmem.rdata
    )
  )

}
