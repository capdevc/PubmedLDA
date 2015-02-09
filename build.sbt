name := "PubmedLDA"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "cc.mallet" % "mallet" % "2.0.7",
  "com.github.scopt" %% "scopt" % "3.3.0"
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("public")
)
