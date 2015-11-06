libraryDependencies ++= Seq(
	compilerPlugin("org.scala-lang.plugins" % ("scala-continuations-plugin_" + scalaVersion.value) % "1.0.2")
)

scalaSource in Compile <<= baseDirectory(_ / "src")

javaSource in Compile <<= baseDirectory(_ / "src")

compileOrder := CompileOrder.JavaThenScala

unmanagedJars in Compile <<= baseDirectory map { base => ((base ** "lib") ** "*.jar").classpath }

autoCompilerPlugins := true

scalacOptions += "-P:continuations:enable"

ivyXML := <dependencies>
	  <dependency org="com.github.ansell.pellet" name="pellet-owlapiv3" rev="2.3.6-ansell">
	  	<!-- xsdlib brings very old xerces implementation, exclude it-->
        <exclude org="msv" module="xsdlib" />
	  </dependency>
    </dependencies>

classDirectory in Compile <<= target(_ / "scala/classes")

classDirectory in Test <<= target(_ / "scala/test-classes")

fork := true

unmanagedClasspath in Runtime <+= (baseDirectory) map { bd => Attributed.blank(bd / "configs") }

baseDirectory in run := baseDirectory.value

javaOptions in run += "-Xmx2G" 

javaOptions in run += "-Xms1G"

scalacOptions += "-P:continuations:enable"
