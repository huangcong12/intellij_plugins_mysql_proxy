plugins {
  id("java")
//  id("org.jetbrains.kotlin.jvm") version "1.8.21"
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
    // https://mvnrepository.com/artifact/io.vertx/vertx-core
    implementation("io.vertx:vertx-core:4.4.5")
    // https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
    implementation("com.mysql:mysql-connector-j:8.1.0")
    // https://mvnrepository.com/artifact/com.github.jsqlparser/jsqlparser
    implementation("com.github.mnadeem:sql-table-name-parser:0.0.5")
  // https://mvnrepository.com/artifact/org.json/json
  implementation("org.json:json:20231013")
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    options.encoding = "UTF-8"
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
    token.set("aHVhbmdjb25nMTI=.OTItODY5OQ==.IDVLJHKarZkMFhdfOe3L6e39nULvg2")
  }
}
