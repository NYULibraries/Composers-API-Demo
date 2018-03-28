name := """composers-api"""
organization := "edu.nyu.dlts"

version := "b0.2.2"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
	guice,
	ws,
	"commons-io" % "commons-io" % "2.5",
	"org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "edu.nyu.dlts.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "edu.nyu.dlts.binders._"
