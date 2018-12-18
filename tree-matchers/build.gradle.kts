plugins {
    kotlin("jvm")
}

group = "com.github.oowekyala.kt-tree-utils"
version = "2.0"


dependencies {
    compile(project(":tree-printers"))
    compileOnly(kotlin("test"))
}
