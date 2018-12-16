import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm") version "1.2.41"
    id("org.jetbrains.dokka") version "0.9.17"

}

group = "com.github.oowekyala"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
    jcenter()
}

dependencies {

    compile(kotlin("stdlib-jdk7"))
    compile(kotlin("test"))

    // test use case
    testCompile("net.sourceforge.pmd:pmd-java:6.6.0")

    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.1.11")
}



tasks {

    getByName<DokkaTask>("dokka") {
        outputFormat = "html"
        outputDirectory = "$buildDir/javadoc"

        includes += "dokka.md"
    }

    getByName<Test>("test") {
        useJUnitPlatform { }
    }
}