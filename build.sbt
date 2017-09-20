name := """composers-api"""
organization := "edu.nyu.dlts"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
	guice,
	ws,
	"org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "edu.nyu.dlts.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "edu.nyu.dlts.binders._"
