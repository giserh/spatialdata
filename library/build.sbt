//scalaVersion := "2.12.6"
scalaVersion := "2.11.8"

name := "spatialdata"

organization := "org.openmole"

version := "0.1-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.sonatypeRepo("staging")
resolvers += Resolver.mavenLocal

libraryDependencies += "org.apache.commons" % "commons-math3" % "3.6.1"
libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.5.0"
libraryDependencies += "org.diana-hep" %% "histogrammar" % "1.0.4"

val osmCommonVersion = "0.0.3-SNAPSHOT"
libraryDependencies += "se.kodapan.osm.common" % "jts" % osmCommonVersion
