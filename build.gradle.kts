import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm") version "1.2.41"
    id("org.jetbrains.dokka") version "0.9.17"
    `maven-publish`

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

tasks.getByName<Test>("test") {
    useJUnitPlatform { }
}


val dokka by tasks.getting(DokkaTask::class) {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"

    includes += "dokka.md"
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    classifier = "javadoc"
    from(dokka)
}

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    classifier = "sources"
    from(java.sourceSets["main"].allSource)
}


with(project.extensions) {

    val publishing = getByType(PublishingExtension::class.java)

    with(publishing) {

        publications {

            create<MavenPublication>("default") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()

                from(components["java"])
                artifact(dokkaJar)
                artifact(sourcesJar)

                pom {

                    name.set("Kotlin Tree Matchers")
                    packaging = "jar"
                    description.set("A testing DSL to specify the structure of a tree in a concise and readable way.")
                    url.set("https://github.com/oowekyala/kt-tree-matchers")

                }
            }
        }


        repositories {
            //            mavenCentral()
            mavenLocal()
        }
    }
}