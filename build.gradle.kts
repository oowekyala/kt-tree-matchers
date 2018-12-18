import io.codearte.gradle.nexus.NexusStagingPlugin
import org.gradle.kotlin.dsl.accessors.kotlinTypeStringFor
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import java.util.Date

plugins {
    kotlin("jvm") version "1.3.10"
    id("org.jetbrains.dokka") version "0.9.17"
    id("io.codearte.nexus-staging") version "0.11.0"
    `maven-publish`
    signing
}

val kotlinVersion by extra { "1.3.10" } // sync with above

group = "com.github.oowekyala.kt-tree-utils"
version = "2.0"


repositories {
    jcenter()
}

dependencies {
    constraints {
        testImplementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
        testImplementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    }
}

subprojects {

    apply<KotlinPluginWrapper>()
    apply<DokkaPlugin>()
    apply<MavenPublishPlugin>()
    apply<SigningPlugin>()

    dependencies {
        implementation(kotlin("stdlib-jdk7"))

        // test use case
        testCompile("net.sourceforge.pmd:pmd-java:6.6.0")

        testImplementation("io.kotlintest:kotlintest-runner-junit5:3.1.11")
        testRuntime("org.slf4j:slf4j-api:1.7.25")
        testRuntime("org.slf4j:slf4j-log4j12:1.7.25")
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

                val repoAddress = "https://github.com/oowekyala/kt-tree-matchers"

                create<MavenPublication>("default") {
                    groupId = group.toString()
                    artifactId = name
                    version = version.toString()

                    from(this@subprojects.components["java"])
                    artifact(dokkaJar)
                    artifact(sourcesJar)

                    pom {

                        operator fun <T> Property<T>.invoke(value: T): Unit = set(value)

                        val mvnName = name

                        name("Kotlin Tree Matchers")
                        packaging = "jar"

                        description("A testing DSL to specify the structure of a tree in a concise and readable way.")
                        url(repoAddress)
                        inceptionYear("2018")

                        scm {
                            url(repoAddress)
                            connection("scm:$repoAddress.git")
                            developerConnection("scm:$repoAddress.git")
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
}