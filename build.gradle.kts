plugins {
    id("eclipse")
    id("idea")
    id("fabric-loom") version "1.6.12"
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example.presentationmod"
version = "2.0"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

val shade by configurations.creating

fun org.gradle.api.artifacts.dsl.DependencyHandler.embedLibrary(
    group: String,
    name: String,
    version: String
) {
    val dependency = "$group:$name:$version"
    val implementationDependency = add("implementation", dependency) as org.gradle.api.artifacts.ExternalModuleDependency
    implementationDependency.exclude(group = "org.apache.logging.log4j")

    val shadedDependency = add(shade.name, dependency) as org.gradle.api.artifacts.ExternalModuleDependency
    shadedDependency.exclude(group = "org.apache.logging.log4j")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:1.19.2")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.16.14")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.77.0+1.19.2")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.10.18+kotlin.1.9.22")

    implementation(kotlin("stdlib"))

    embedLibrary("org.apache.pdfbox", "pdfbox", "2.0.36")
    embedLibrary("org.apache.pdfbox", "fontbox", "2.0.36")
    embedLibrary("org.apache.poi", "poi-ooxml", "5.3.0")
    embedLibrary("org.apache.poi", "poi", "5.3.0")
    embedLibrary("org.apache.poi", "poi-ooxml-lite", "5.3.0")
    embedLibrary("org.apache.xmlbeans", "xmlbeans", "5.2.1")
    embedLibrary("org.apache.commons", "commons-math3", "3.6.1")
    embedLibrary("org.apache.commons", "commons-collections4", "4.4")
    embedLibrary("com.github.virtuald", "curvesapi", "1.08")
    embedLibrary("com.zaxxer", "SparseBitSet", "1.3")

}

loom {
    runs {
        named("client") {
            runDir("run")
        }
        named("server") {
            runDir("run-server")
        }
    }
}

sourceSets.named("main") {
    java.srcDir("src/main/kotlin")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

tasks.named<Jar>("jar") {
    manifest.attributes["Implementation-Title"] = project.name
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("dev-shadow")
    configurations = listOf(shade)
    exclude("org/apache/logging/log4j/**")
    exclude("META-INF/versions/9/org/apache/logging/log4j/**")
}

tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    dependsOn(tasks.named("shadowJar"))
    inputFile.set(tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").flatMap { it.archiveFile })
}
