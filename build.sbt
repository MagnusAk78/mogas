name := """mogas"""

version := "0.9-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

//Scala play test
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

//Snapshots resolver for play-bootstrap
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

//Silhouette
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  //Reactive mongo
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.14",
  //"org.webjars" % "jquery" % "2.2.3", (Bootstrap include jquery 1.11.1)
  "org.webjars" % "bootstrap" % "4.0.0-alpha.3",
  //Silhouette for authentication
  "com.mohiva" %% "play-silhouette" % "4.0.0-RC1",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "4.0.0-RC1",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "4.0.0-RC1",
  "com.mohiva" %% "play-silhouette-persistence" % "4.0.0-RC1",
  "com.mohiva" %% "play-silhouette-testkit" % "4.0.0-RC1" % "test",
  "com.mohiva" %% "play-silhouette-persistence-reactivemongo" % "4.0.0-RC1",
  
  //Webjars that allows dependecy management of things like jquery and other web tools
  "org.webjars" %% "webjars-play" % "2.5.0-2",
  "org.webjars.bower" % "mediaelement" % "2.19.0",
  "org.webjars.bower" % "tether" % "1.3.3",
  //Extra stuff from silhuette example, don´t know exactly
  "net.codingwell" %% "scala-guice" % "4.0.1",
  "com.iheart" %% "ficus" % "1.2.6",
  
  //Scrimage that allows for rescaling of images etc.
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.0"  
)

libraryDependencies += filters

EclipseKeys.preTasks := Seq(compile in Compile)

EclipseKeys.withSource := true

EclipseKeys.withJavadoc := true

fork in run := true


fork in run := true