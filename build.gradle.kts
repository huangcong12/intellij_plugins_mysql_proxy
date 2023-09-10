plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.8.21"
  id("org.jetbrains.intellij") version "1.13.3"
}

group = "com.ls.akong"
version = project.property("version")
val javaVersion = project.property("javaVersion") as String
val ideaVersion = project.property("ideaVersion") as String
val ideaTarget = project.property("ideaTarget") as String
val ideaSinceBuild = project.property("ideaSinceBuild") as String
val ideaUntilBuild = project.property("ideaUntilBuild") as String

repositories {
  mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set(ideaVersion)
  type.set(ideaTarget) // Target IDE Platform

  plugins.set(listOf(/* Plugin Dependencies */))

  pluginName.set("MySQL Proxy")
}

dependencies {
    // https://mvnrepository.com/artifact/com.github.shyiko/mysql-binlog-connector-java
    implementation("com.h2database:h2:2.1.214")
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }

  patchPluginXml {
    sinceBuild.set(ideaSinceBuild)
    untilBuild.set(ideaUntilBuild)
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
