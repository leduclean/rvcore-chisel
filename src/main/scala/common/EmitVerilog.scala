package common

import _root_.circt.stage.ChiselStage
import chisel3._

object EmitModule {

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage: mill ... common.EmitModule <ModuleName> [constructor args...]")
      sys.exit(1)
    }

    val moduleName = args(0)
    val ctorArgs   = args.drop(1) // reste des arguments (strings)

    // Reflection scala : charger dynamiquement un module
    val cls  = Class.forName(moduleName)
    val ctor = cls.getConstructors.head

    // Fonction de conversion string -> objet typé
    def parseArg(s: String): AnyRef = {
      // essai en Int
      if (s.matches("^-?\\d+$")) {
        Integer.valueOf(s.toInt)
      }
      // essai en Bool
      else if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
        java.lang.Boolean.valueOf(s.toBoolean)
      }
      // sinon, on laisse en String
      else {
        s
      }
    }

    val generator = () => {
      if (ctorArgs.isEmpty) {
        ctor.newInstance().asInstanceOf[RawModule]
      } else {
        val argsObjects: Array[Object] = ctorArgs.map(parseArg).toArray
        ctor.newInstance(argsObjects: _*).asInstanceOf[RawModule]
      }
    }

    ChiselStage.emitSystemVerilogFile(
      gen = generator(),
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
      args = Array("--target-dir", s"RTL/$moduleName")
    )
  }

}
