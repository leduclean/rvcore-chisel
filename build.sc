// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import mill.scalalib.TestModule.ScalaTest
import scalalib._
// support BSP
import mill.bsp._

// Note: This project requires .mill-jvm-opts file containing:
//   -Dchisel.project.root=${PWD}
// This is needed because Chisel needs to know the project root directory
// to properly generate and handle test directories and output files.
// See: https://github.com/com-lihaoyi/mill/issues/3840

object `TPchisel` extends SbtModule { m =>
  override def millSourcePath = super.millSourcePath / os.up
  override def scalaVersion   = "2.13.16"
  override def scalacOptions  = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit",
    "-Ymacro-annotations",
    "-unchecked",
    "-Xfatal-warnings",
    "-Ywarn-dead-code",
    "-Ywarn-unused"
  )
  override def ivyDeps = Agg(
    ivy"org.chipsalliance::chisel:7.2.0"
  )
  override def scalacPluginIvyDeps = Agg(
    ivy"org.chipsalliance:::chisel-plugin:7.2.0"
  )
  object test extends SbtTests with TestModule.ScalaTest {
    override def ivyDeps = m.ivyDeps() ++ Agg(
      ivy"org.scalatest::scalatest::3.2.19"
    )
  }
}

object scalafmt extends ScalafmtModule {
  override def moduleDeps = Seq(`TPchisel`)
  override def sources    = T.sources { TPchisel.sources() }
}
