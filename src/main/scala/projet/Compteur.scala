package projet

import chisel3._

// Déclaration d'un classe Compteur très générique
class Compteur(
  n: Int, // Le paramètre n permet de fixer le nombre de bits du compteur.
  hasEnable: Boolean =
    false, // Le paramètre hasEnable permet d'ajouter une entrée optionnelle pour autoriser l'incrémentation.
  hasMax: Boolean =
    false // Le paramètre hasMax permet d'ajouter : - une entrée optionnelle max, qui fait reboucler le compteur après qu'il est atteint la valeur fournie.
  //                                        - une sortie optionnelle atMax, qui vaut 1 lorsque le compteur atteint la valeur max définie.
) extends Module {

  val io = IO(new Bundle {
    // Grâce au parametre hasEnable, on peut activer optionnellement le port d'entrée en.
    // En scala, la classe option a 2 classes dérivées : Some et None.
    // Some permet donc de wrapper notre type optionnel.
    val en    = if (hasEnable) Some(Input(Bool())) else None
    val max   = if (hasMax) Some(Input(UInt(n.W))) else None
    val atMax = if (hasMax) Some(Output(Bool())) else None
    val out   = Output(UInt(n.W)) // On utilise le paramètre n pour remplacer la constante de l'exercice prédédent.
  })

  val compteur = RegInit(0.U(n.W))

  // condition d'incrément
  val doInc = if (hasEnable) io.en.get
  else true.B // Attention, notez qu'il faut utiliser l'opérateur get pour récupérer l'option wrappée par le Some.

  // condition d'égalité au maximum
  val atMax = if (hasMax) compteur === io.max.get else false.B

  compteur := Mux(doInc, Mux(atMax, 0.U(n.W), compteur + 1.U), compteur)

  if (hasMax) io.atMax.get := atMax

  io.out := compteur
}
