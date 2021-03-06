import sbt._

object Dependencies {

  object Versions {
    val scala = "2.12.8"
    val akka = "2.5.21"
    val akkaHttp = "10.1.7"
    val alpakka = "0.20"
  }

  val smqd: Seq[ModuleID] = Seq(
    "com.thing2x" %% "smqd-core" % "0.4.12" % Provided,
    "org.slf4j" % "log4j-over-slf4j" % "1.7.7" % Test,
  )

  val fonts: Seq[ModuleID] = Seq(
    "com.thing2x" % "jasperreports-font-nanum" % "0.2.0" % Test
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5",
    "com.typesafe.akka" %% "akka-http-testkit" % Versions.akkaHttp,
    "com.typesafe.akka" %% "akka-testkit" % "2.5.17",
  ).map(_ % Test)

  val slick: Seq[ModuleID] = Seq(
    "com.typesafe.slick" %% "slick",
    "com.typesafe.slick" %% "slick-hikaricp",
    "com.typesafe.slick" %% "slick-codegen",
  ).map ( _ % "3.3.0")

  val quartz: Seq[ModuleID] = Seq(
    "com.enragedginger" %% "akka-quartz-scheduler" % "1.8.0-akka-2.5.x" exclude("com.zaxxer", "HikariCP-java6")
  )

  val h2db: Seq[ModuleID] = Seq(
    "com.h2database" % "h2" % "1.4.198"
  )

  private val jasperReportVersion = "6.9.0"

  val jasperreports: Seq[ModuleID] = Seq(
    // "net.sf.jasperreports" % "jasperreports-chart-themes", // this extension requires spring framework
    // "net.sf.jasperreports" % "jasperreports-chart-customizers",
    // "net.sf.jasperreports" % "jasperreports-custom-visualization",
    "net.sf.jasperreports" % "jasperreports-functions",
    "net.sf.jasperreports" % "jasperreports-annotation-processors",
    "net.sf.jasperreports" % "jasperreports-metadata",
    "net.sf.jasperreports" % "jasperreports-fonts",
    "net.sf.jasperreports" % "jasperreports",
  ).map( _ % jasperReportVersion )

  // JasperReport requires dependencies
  // https://community.jaspersoft.com/wiki/jasperreports-library-requirements
  
  val rhino: Seq[ModuleID] = Seq(
    "org.mozilla" % "rhino" % "1.7.10"
  )

  val jfreechart: Seq[ModuleID] = Seq(
    "org.jfree" % "jfreechart" % "1.5.0"
  )

  //
  // apache poi is required for exporting report in excel format
  //
  val poi: Seq[ModuleID] = Seq(
    "org.apache.poi" % "poi-ooxml" %  "3.17"
  )

  //
  // !!! add resolver to solve the issue related "com.lowagie" % "itext" % "2.1.7.js6"
  // https://community.jaspersoft.com/questions/1071031/itext-js6-dependency-issue
  //
  def jasperreportsResolver: Resolver = {
    "jaspersoft-third-party" at "http://jaspersoft.jfrog.io/jaspersoft/third-party-ce-artifacts/"
  }
}
