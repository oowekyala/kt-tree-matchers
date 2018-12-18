plugins {
    kotlin("jvm")
}

group = "com.github.oowekyala.kt-tree-utils"
version = "2.0"

repositories {
    mavenCentral()
}

dependencies {
    compile(project(":tree-printers"))
    compile(kotlin("test"))
}
