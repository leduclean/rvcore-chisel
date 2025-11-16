package projet

import chisel3._
import chisel3.experimental._
import chisel3.util._

/* Blackbox Verilog pour la simu et la synthèse sans placement */
class dpram( // nom identique à celui du verilog
  val NUM_COL: Int,
  val COL_WIDTH: Int,
  val ADDR_WIDTH: Int,
  val DATA_WIDTH: Int,
  val INIT_FILE: String = "program.mem"
) extends BlackBox(Map(
      "NUM_COL"    -> IntParam(NUM_COL),
      "COL_WIDTH"  -> IntParam(COL_WIDTH),
      "ADDR_WIDTH" -> IntParam(ADDR_WIDTH),
      "DATA_WIDTH" -> IntParam(DATA_WIDTH),
      "INIT_FILE"  -> StringParam(if (INIT_FILE != "") INIT_FILE else "NONE")
    )) with HasBlackBoxResource {

  require(DATA_WIDTH == NUM_COL * COL_WIDTH, "DATA_WIDTH must be equal to NUM_COL*COL_WIDTH.")

  addResource("/vsrc/dpram.v")

  val io = IO(new Bundle {
    // Port A
    val clkA  = Input(Clock())
    val enaA  = Input(Bool())
    val addrA = Input(UInt(ADDR_WIDTH.W))
    val doutA = Output(UInt(DATA_WIDTH.W))
    val dinA  = Input(UInt(DATA_WIDTH.W))
    val weA   = Input(UInt(NUM_COL.W))
    // Port B
    val clkB  = Input(Clock())
    val enaB  = Input(Bool())
    val addrB = Input(UInt(ADDR_WIDTH.W))
    val doutB = Output(UInt(DATA_WIDTH.W))
    val dinB  = Input(UInt(DATA_WIDTH.W))
    val weB   = Input(UInt(NUM_COL.W))
  })

}

class xpm_memory_tdpram( // nom identique à celui de Xilinx
  val ADDR_WIDTH: Int,
  val INIT_FILE: String
) extends BlackBox(Map(
      "ADDR_WIDTH_A"       -> IntParam(ADDR_WIDTH),
      "ADDR_WIDTH_B"       -> IntParam(ADDR_WIDTH),
      "BYTE_WRITE_WIDTH_A" -> 8,
      "MEMORY_INIT_FILE"   -> StringParam(INIT_FILE),
      "MEMORY_PRIMITIVE"   -> "block",
      "MEMORY_SIZE"        -> 524288,
      "READ_DATA_WIDTH_A"  -> 32,
      "READ_DATA_WIDTH_B"  -> 32,
      "READ_LATENCY_A"     -> 1,
      "READ_LATENCY_B"     -> 1,
      "WRITE_DATA_WIDTH_A" -> 32,
      "WRITE_DATA_WIDTH_B" -> 32
    )) with HasBlackBoxResource {

  val io = IO(new Bundle {
    // Port A
    val clka           = Input(Clock())
    val rsta           = Input(Bool())
    val ena            = Input(Bool())
    val regcea         = Input(Bool())
    val dina           = Input(UInt(32.W))
    val addra          = Input(UInt(ADDR_WIDTH.W))
    val wea            = Input(UInt(4.W))
    val injectdbiterra = Input(Bool())
    val injectsbiterra = Input(Bool())
    val douta          = Output(UInt(32.W))
//    val dbiterrA = Output(Bool())
//    val sbiterrA = Output(Bool())
    // Port B
    val clkb           = Input(Clock())
    val rstb           = Input(Bool())
    val enb            = Input(Bool())
    val regceb         = Input(Bool())
    val dinb           = Input(UInt(32.W))
    val addrb          = Input(UInt(ADDR_WIDTH.W))
    val web            = Input(UInt(4.W))
    val injectdbiterrb = Input(Bool())
    val injectsbiterrb = Input(Bool())
    val doutb          = Output(UInt(32.W))
//    val dbiterrB = Output(Bool())
//    val sbiterrB = Output(Bool())
    val sleep = Input(Bool())
  })

}

/* \ Attention, les BRAM sont adressables en mots, et les adresses émises sur notre interconnect sont des adresses
 * octets, qui en revanche doivent être alignées sur le type de la donnée transportée.
 * Nous optons ici pour une mémoire dual-port pour l'accès concurrent aux instructions et aux données, avec un espace
 * d'adressage unique pour faciliter le préchargement (on espère).
 *
 * \ */
class IDMem(sim: Boolean, addrWidth: Int, memFile: String = "") extends Module {

  val io = IO(new Bundle {
    val clki = Input(Clock())
    val ibus = new BusInterface
    val clkd = Input(Clock())
    val dbus = new BusInterface
  })

// Pour la simulation ou la synthèse ne nécessitant pas de repérer les BRAM, on blackboxe avec le verilog Xilinx
  if (sim || memFile == "") {
    // Mémoire 32 bits : 4 colonnes de 8 bit
    // l'accès BRAM est un accès mot, et non octet, on ignore les 2 bits de
    // poids faible de l'adresse

    val unifiedRam = Module(new dpram(
      NUM_COL = 4,
      COL_WIDTH = 8,
      ADDR_WIDTH = addrWidth - 2,
      DATA_WIDTH = 32,
      INIT_FILE = memFile
    ))

    unifiedRam.io.clkA := io.clki
    unifiedRam.io.clkB := io.clkd
    // instructions sur le port a
    unifiedRam.io.enaA  := io.ibus.en
    unifiedRam.io.addrA := io.ibus.addr(addrWidth - 1, 2)
    io.ibus.rdata       := unifiedRam.io.doutA
    unifiedRam.io.weA   := 0.U
    unifiedRam.io.dinA  := 0.U
    // données sur le port b
    unifiedRam.io.enaB  := io.dbus.en
    unifiedRam.io.addrB := io.dbus.addr(addrWidth - 1, 2)
    io.dbus.rdata       := unifiedRam.io.doutB
    unifiedRam.io.weB   := io.dbus.be.asUInt
    unifiedRam.io.dinB  := io.dbus.wdata
  } else {
    val dpram = Module(new xpm_memory_tdpram(ADDR_WIDTH = addrWidth - 2, INIT_FILE = memFile))
    dpram.io.clka           := clock
    dpram.io.rsta           := 0.U
    dpram.io.ena            := io.dbus.en
    dpram.io.regcea         := false.B
    dpram.io.dina           := io.dbus.wdata
    dpram.io.addra          := io.dbus.addr(addrWidth - 1, 2)
    dpram.io.wea            := io.dbus.be.asUInt
    dpram.io.injectdbiterra := false.B
    dpram.io.injectsbiterra := false.B
    io.dbus.rdata           := dpram.io.douta

    dpram.io.clkb           := clock
    dpram.io.rstb           := 0.U
    dpram.io.enb            := io.ibus.en
    dpram.io.regceb         := false.B
    dpram.io.dinb           := 0.U
    dpram.io.addrb          := io.ibus.addr(addrWidth - 1, 2)
    dpram.io.web            := 0.U
    dpram.io.injectdbiterrb := false.B
    dpram.io.injectsbiterrb := false.B
    io.ibus.rdata           := dpram.io.doutb

    dpram.io.sleep := false.B
  }

}
