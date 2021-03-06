name := "the_gardener"

val jdkVersion = "1.8"
scalaVersion := "2.12.4"

lazy val root = (project in file(".")).enablePlugins(PlayScala)


// specify the source and target jdk for Java compiler
javacOptions ++= Seq("-source", jdkVersion, "-target", jdkVersion)

// specify the target jdk for Scala compiler
//scalacOptions += s"-target:jvm-$jdkVersion"

// add directory for test configuration files
unmanagedClasspath in Test += baseDirectory.value / "local-conf"
unmanagedClasspath in Runtime += baseDirectory.value / "local-conf"

//*** dist packaging
// do not generage API documentation when using dist task
sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false


//Removing the top level directory
topLevelDirectory := None

libraryDependencies ++= Seq( 
  ws,
  filters,
  guice,
  //jdbc,
  //ehcache,
  "ch.qos.logback"%"logback-access"%"1.2.3",
  "net.logstash.logback"%"logstash-logback-encoder"%"4.11",
  "io.cucumber" %% "cucumber-scala" % "2.0.1" % Test,
  "io.cucumber" % "cucumber-junit" % "2.0.1" % Test,
  "io.cucumber" % "cucumber-picocontainer" % "2.0.1" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test
)

evictionWarningOptions in update := EvictionWarningOptions.default.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false).withWarnScalaVersionEviction(false)

routesGenerator := InjectedRoutesGenerator
