package projet

import chisel3._

class GeneSync extends Module {

  val io = IO(new Bundle {
    val hsync = Output(Bool())
    val vsync = Output(Bool())
    val img   = Output(Bool())
    val x     = Output(UInt(10.W))
    val y     = Output(UInt(9.W))
  })

  // Paramètres de synchronisation horizontale et verticale
  val synchroX = VecInit(Seq(
    95.U,  // HS pulse width
    47.U,  // horizontal back porch
    639.U, // horizontal display time
    15.U   // horizontal front porch
  ))

  val synchroY = VecInit(Seq(
    1.U,   // VS pulse width
    28.U,  // vertical back porch
    479.U, // vertical display time
    9.U    // vertical front porch
  ))

  // Instanciation des compteurs
  val compteurCLK = Module(new Compteur(3, hasMax = true))                    // Compteur horloge (3 bits)
  val comptX      = Module(new Compteur(10, hasEnable = true, hasMax = true)) // Compteur horizontal (10 bits)
  val comptY      = Module(new Compteur(9, hasEnable = true, hasMax = true))  // Compteur vertical (9 bits)
  val phaseH      = Module(new Compteur(2, hasEnable = true))                 // Phases horizontales (2 bits)
  val phaseV      = Module(new Compteur(2, hasEnable = true))                 // Phases verticales (2 bits)

  // Signaux d'activation
  val en25     = compteurCLK.io.atMax.get          // Activation à 25MHz quand compteurCLK atteint 4
  val enPhaseH = en25 && comptX.io.atMax.get
  val enY      = enPhaseH && phaseH.io.out === 3.U // Nouvelle ligne

  // Configuration compteur CLK (divise 125MHz par 5 pour avoir 25MHz)
  compteurCLK.io.max.get := 4.U

  // Configuration compteur X (horizontal)
  comptX.io.en.get  := en25
  comptX.io.max.get := synchroX(phaseH.io.out)

  // Configuration compteur Y (vertical)
  comptY.io.en.get  := enY
  comptY.io.max.get := synchroY(phaseV.io.out)

  // Configuration compteurs de phases
  phaseH.io.en.get := enPhaseH
  phaseV.io.en.get := enY && comptY.io.atMax.get

  // Génération des signaux de sortie
  io.hsync := phaseH.io.out.orR                                  // true si phaseH != 0 (pas en phase pulse)
  io.vsync := phaseV.io.out.orR                                  // true si phaseV != 0 (pas en phase pulse)
  io.img   := (phaseH.io.out === 2.U) && (phaseV.io.out === 2.U) // Zone d'affichage

  // Coordonnées pixel
  io.x := comptX.io.out
  io.y := comptY.io.out
}
