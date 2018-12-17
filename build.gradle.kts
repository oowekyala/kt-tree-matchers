import org.jetbrains.dokka.gradle.DokkaTask
import java.util.Date

plugins {
    kotlin("jvm") version "1.2.41"
    id("org.jetbrains.dokka") version "0.9.17"
    id("io.codearte.nexus-staging") version "0.11.0"
    `maven-publish`
    signing
}

group = "com.github.oowekyala"
version = "1.0"


repositories {
    jcenter()
}

dependencies {

    implementation(kotlin("stdlib-jdk7"))
    compile(kotlin("test"))

    // test use case
    testCompile("net.sourceforge.pmd:pmd-java:6.6.0")

    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.1.11")
}



tasks {

    test {
        useJUnitPlatform { }
    }

    dokka {
        outputFormat = "html"
        outputDirectory = "$buildDir/javadoc"

        includes += "dokka.md"
    }

    val dokkaJar by creating(Jar::class) {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = "Assembles Kotlin docs with Dokka"
        classifier = "javadoc"
        from(dokka)
    }

    val sourcesJar by creating(Jar::class) {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = "Assembles sources JAR"
        classifier = "sources"
        from(sourceSets["main"].allSource)
    }


    publishing {
        publications {

            create<MavenPublication>("default") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()

                from(project.components["java"])
                artifact(dokkaJar)
                artifact(sourcesJar)

                pom {

                    operator fun <T> Property<T>.invoke(value: T): Unit = set(value)

                    name("Kotlin Tree Matchers")
                    packaging = "jar"

                    description("A testing DSL to specify the structure of a tree in a concise and readable way.")
                    url("https://github.com/oowekyala/kt-tree-matchers")
                    inceptionYear("2018")

                    scm {
                        url("https://github.com/oowekyala/kt-tree-matchers")
                        connection("scm:https://github.com/oowekyala/kt-tree-matchers.git")
                        developerConnection("scm:https://github.com/oowekyala/kt-tree-matchers.git")
                    }

                    licenses {
                        license {
                            name("The Unlicense")
                            url("http://unlicense.org/UNLICENSE")
                            distribution("repo")
                        }
                    }

                    developers {
                        developer {
                            id("oowekyala")
                            name("Clément Fournier")
                            email("clement.fournier76@gmail.com")
                        }
                    }
                }
            }
        }


        repositories {
            maven {
                val myUrl =
                        if (version.toString().endsWith("-SNAPSHOT"))
                            "https://oss.sonatype.org/content/repositories/snapshots"
                        else
                            "https://oss.sonatype.org/service/local/staging/deploy/maven2"

                url = uri(myUrl)

                credentials {
                    username = property("nexusUsername").toString()
                    password = property("nexusPassword").toString()
                }
            }
        }
    }


    signing {
        sign(publishing.publications["default"])
    }
}