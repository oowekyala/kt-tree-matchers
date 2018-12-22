import io.codearte.gradle.nexus.NexusStagingPlugin
import org.gradle.api.publish.maven.MavenPom
import org.gradle.kotlin.dsl.accessors.kotlinTypeStringFor
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.LinkMapping
import org.jetbrains.dokka.gradle.SourceRoot
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeFirstWord
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.Date
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    kotlin("jvm") version "1.3.10"
    id("org.jetbrains.dokka") version "0.9.17"
    id("io.codearte.nexus-staging") version "0.11.0"
    `maven-publish`
    signing
}

val kotlinVersion by extra { "1.3.10" } // sync with above
val repoAddress = "https://github.com/oowekyala/kt-tree-matchers"

group = "com.github.oowekyala.treeutils"
version = "2.0.2"


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


    val sub = this@subprojects

    sub.group = rootProject.group
    sub.version = rootProject.version

    repositories {
        jcenter()
    }

    apply<KotlinPluginWrapper>()
    apply<DokkaPlugin>()
    apply<MavenPublishPlugin>()
    apply<SigningPlugin>()



    dependencies {
        compileOnly(kotlin("stdlib-jdk7"))

        // test use case
        testCompile("net.sourceforge.pmd:pmd-java:6.6.0")

        testImplementation(kotlin("test"))
        testImplementation("io.kotlintest:kotlintest-runner-junit5:3.1.11")
        testRuntime("org.slf4j:slf4j-api:1.7.25")
        testRuntime("org.slf4j:slf4j-log4j12:1.7.25")
    }


    tasks {

        compileKotlin {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }

        test {
            useJUnitPlatform { }
        }

        dokka {
            outputDirectory = "$buildDir/javadoc"
            outputFormat = "html"
            includes += "../dokka.md"

            linkMapping(delegateClosureOf<LinkMapping> {
                dir = "src/main/kotlin"
                url = "$repoAddress/blob/master/$dir"
                suffix = "#L"
            })
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
                    groupId = sub.group.toString()
                    artifactId = sub.name
                    version = sub.version.toString()

                    from(sub.components["java"])
                    artifact(dokkaJar)
                    artifact(sourcesJar)

                    pom {

                        operator fun <T> Property<T>.invoke(value: T): Unit = set(value)

                        val mvnName = sub.name
                                .replace('-', ' ')
                                .split(" ")
                                .map { it.capitalize() }
                                .joinToString(separator = " ") { it }
                                .let { "$it for Kotlin" }

                        name(mvnName)
                        packaging = "jar"


                        description(
                                if (sub.name == "tree-matchers")
                                    "A testing DSL to specify the structure of a tree in a concise and readable way."
                                else
                                    "A set of pretty printers for trees, any trees."
                        )
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

                        withXml {
                            fun NodeList.asList(): List<Node> = (0..length).map { item(it) }

                            val root = this.asElement().ownerDocument

                            fun elt(name: String, contents: (Element) -> Unit = {}) =
                                    root.createElement(name).also {
                                        contents(it)
                                    } as Element

                            fun Node.plus(name: String, text: String? = null): Element =
                                    appendChild(elt(name) {
                                        if (text != null)
                                            it.appendChild(root.createTextNode(text))
                                    }) as Element

                            val dependencies =
                                    root.getElementsByTagName("dependencies").asList().firstOrNull()
                                    ?: root.documentElement.plus("dependencies")


                            sub.configurations.compileOnly.allDependencies.forEach { dep ->

                                val node = elt("dependency") {
                                    it.plus("groupId", dep.group ?: "")
                                    it.plus("artifactId", dep.name)
                                    dep.version?.let { v -> it.plus("version", v) }
                                    it.plus("scope", "provided")
                                }

                                dependencies.appendChild(node)
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