name := """mogas"""

version := "0.9-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, LauncherJarPlugin, SbtWeb)

scalaVersion := "2.12.11"

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)


//Scala play test
//resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

//Snapshots resolver for play-bootstrap
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

//Silhouette
resolvers += Resolver.jcenterRepo

//bugfix: https://github.com/jkutner/activator-sbt-bug
resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)

libraryDependencies ++= Seq(
  //Maybe needed (https://www.silhouette.rocks/v5.0/docs/migration-guide)
  ehcache,
  guice,
  // Play json is now in it's own module
  "com.typesafe.play" %% "play-json" % "2.6.7",
  // play iteratees is now in it's own module
  "com.typesafe.play" %% "play-iteratees" % "2.6.1",
  // play openId is now in it's own module
  openId,
  jdbc,
  ws,
  // https://mvnrepository.com/artifact/org.scalatestplus.play/scalatestplus-play
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0-M2" % Test,
  //Reactive mongo
  //"org.reactivemongo" %% "play2-reactivemongo" % "0.12.4-play26",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.20.13-play26",
  //"org.reactivemongo" %% "play2-reactivemongo" % "1.0.1-play26",
  // Same as play-silhouette-persistence-reactivemongo (Problematic, uses depricated executioncontext)
  //"org.reactivemongo" %% "play2-reactivemongo" % "0.16.0-play26",

  //Silhouette for authentication
  "com.mohiva" %% "play-silhouette" % "5.0.6",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "5.0.6",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.6",
  "com.mohiva" %% "play-silhouette-persistence" % "5.0.6",
  "com.mohiva" %% "play-silhouette-testkit" % "5.0.6" % "test",
  "com.mohiva" %% "play-silhouette-persistence-reactivemongo" % "5.0.6",

  //Injection for silhouette module
  //"net.codingwell" %% "scala-guice" % "4.1.0", (Tried removing, already included by Play)
  //Ficus is a lightweight companion to Typesafe config that makes it more Scala-friendly.
  "com.iheart" %% "ficus" % "1.4.1",
  
  // https://mvnrepository.com/artifact/org.webjars/webjars-play
  "org.webjars" %% "webjars-play" % "2.6.3",

  "org.webjars" % "bootstrap" % "4.0.0-alpha.3",
  "org.webjars.bower" % "mediaelement" % "2.19.0",
  "org.webjars.bower" % "tether" % "1.3.3",
    
  // https://sksamuel.github.io/scrimage/
  // Library that allows for rescaling of images etc.
  "com.sksamuel.scrimage" % "scrimage-core" % "4.0.10"
)

libraryDependencies += filters

EclipseKeys.preTasks := Seq(compile in Compile)

EclipseKeys.withSource := true

EclipseKeys.withJavadoc := true

fork := true // required for "sbt run" to pick up javaOptions

javaOptions += "-Dplay.editor=http://localhost:63342/api/file/?file=%s&line=%s"
