name := """automation-webservice"""

version := "0.9-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  //Reactive mongo
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.14",
  //Play with bootstrap
  //"com.adrianhurt" %% "play-bootstrap" % "1.0-P25-B3",
  "com.adrianhurt" %% "play-bootstrap" % "1.1-P25-B3-SNAPSHOT",
  //Silhouette for authentication
  "com.mohiva" %% "play-silhouette" % "4.0.0-RC1",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "4.0.0-RC1",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "4.0.0-RC1",
  "com.mohiva" %% "play-silhouette-persistence" % "4.0.0-RC1",
  "com.mohiva" %% "play-silhouette-testkit" % "4.0.0-RC1" % "test",
  "com.mohiva" %% "play-silhouette-persistence-reactivemongo" % "4.0.0-RC1",
  
  //Webjars that allows dependecy management of things like jquery and other web tools
  "org.webjars" %% "webjars-play" % "2.5.0-2",
  //Extra stuff from silhuette example, don´t know exactly
  "net.codingwell" %% "scala-guice" % "4.0.1",
  "com.iheart" %% "ficus" % "1.2.6",
  
  //Scrimage that allows for rescaling of images etc.
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.0"  
)

//Scala play test
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

//Snapshots resolver for play-bootstrap
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

//Silhouette
resolvers += Resolver.jcenterRepo

EclipseKeys.preTasks := Seq(compile in Compile)

fork in run := true
