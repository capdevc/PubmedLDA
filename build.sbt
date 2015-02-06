name := "PubmedLDA"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "com.github.rholder" % "snowball-stemmer" % "1.3.0.581.1",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.3.1",
  "cc.mallet" % "mallet" % "2.0.7"
)
