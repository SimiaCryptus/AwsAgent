import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.net.URI

fun properties(key: String) = project.findProperty(key).toString()
group = properties("libraryGroup")
version = properties("libraryVersion")

plugins {
    java
    `java-library`
    `maven-publish`
    id("signing")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.jetbrains.kotlin.jvm") version "1.7.21"
//    kotlin("jvm") version "1.8.20"
}

repositories {
    mavenCentral {
        metadataSources {
            mavenPom()
            artifact()
        }
    }
}

kotlin {
    jvmToolchain(11)
//    jvmToolchain(17)
}

val kotlin_version = "1.7.21"
val jetty_version = "11.0.15"
val scala_version = "2.13.8"
dependencies {

//    implementation("com.simiacryptus:JoePenai:1.0.8")
    implementation("com.simiacryptus:joe-penai:1.0.7")

    implementation("org.apache.groovy:groovy-all:4.0.11")

//    implementation("com.simiacryptus:skyenet:1.0.4")
//    implementation("com.simiacryptus:SkyeNet:1.0.5")
    implementation("com.simiacryptus.skyenet:util:1.0.5")
    implementation("com.simiacryptus.skyenet:core:1.0.5")
    implementation("com.simiacryptus.skyenet:groovy:1.0.5")
    implementation("com.simiacryptus.skyenet:webui:1.0.5")

    implementation("org.eclipse.jetty:jetty-server:$jetty_version")
    implementation("org.eclipse.jetty:jetty-servlet:$jetty_version")
    implementation("org.eclipse.jetty:jetty-annotations:$jetty_version")
    implementation("org.eclipse.jetty.websocket:websocket-jetty-server:$jetty_version")
    implementation("org.eclipse.jetty.websocket:websocket-jetty-client:$jetty_version")
    implementation("org.eclipse.jetty.websocket:websocket-servlet:$jetty_version")

    implementation("com.google.api-client:google-api-client:1.35.2")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-oauth2:v2-rev157-1.25.0")

    implementation("com.amazonaws:aws-java-sdk:1.12.454")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.google.cloud:google-cloud-texttospeech:2.0.0")

    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("ch.qos.logback:logback-classic:1.2.9")
    implementation("ch.qos.logback:logback-core:1.2.9")
    //implementation("org.slf4j:slf4j-simple:2.0.5")
    implementation("commons-io:commons-io:2.11.0")

    testImplementation(kotlin("script-runtime"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")

}

tasks.withType(ShadowJar::class.java).configureEach {
    archiveClassifier.set("")
    mergeServiceFiles()
    append("META-INF/kotlin_module")
}

tasks {
    compileKotlin {
        kotlinOptions {
            javaParameters = true
        }
    }
    compileTestKotlin {
        kotlinOptions {
            javaParameters = true
        }
    }
    test {
        useJUnitPlatform()
        systemProperty("surefire.useManifestOnlyJar", "false")
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        jvmArgs(
            "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED"
        )
    }
    wrapper {
        gradleVersion = properties("gradleVersion")
    }
}

tasks.withType(ShadowJar::class.java).configureEach {
    isZip64 = true

    archiveClassifier.set("")
    mergeServiceFiles()
    append("META-INF/kotlin_module")

    exclude("**/META-INF/*.SF")
    exclude("**/META-INF/*.DSA")
    exclude("**/META-INF/*.RSA")
    exclude("**/META-INF/*.MF")
    exclude("META-INF/versions/9/module-info.class")

    manifest {
        attributes(
            "Main-Class" to "com.simiacryptus.skyenet.AwsAgent"
        )
    }
}

