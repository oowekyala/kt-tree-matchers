plugins {
    kotlin("jvm")
}


dependencies {
    compile(project(":tree-printers"))
    compileOnly(kotlin("test"))
}
