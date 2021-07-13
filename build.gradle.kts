import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "m4gshm"
version = "0.1-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.4.20"
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

val dryRunner = ":dry-runner"
dependencies {
    implementation(project(dryRunner))
    compileOnly(gradleApi())

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation(gradleApi())
}

tasks.test {
    useJUnit()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaCompile> {
    options.apply {
        encoding = "UTF-8"
        debugOptions.debugLevel = "source,lines,vars"
    }
}

gradlePlugin {
    plugins {
        create("cds-gradle-plugin") {
            id = "m4gshm.gradle.plugin.cds"
            implementationClass = "m4gshm.gradle.plugin.cds.CdsPlugin"
        }
    }
}

val copyRunner = tasks.create("copyRunner") {
    doFirst {
        val runnerProj = project(dryRunner)
        val runnerJar = runnerProj.tasks.getByName<org.gradle.jvm.tasks.Jar>("jar").archiveFile.get().asFile

        val resourcesDir = sourceSets["main"].output.resourcesDir!!
        val destination = resourcesDir.toPath().resolve("META-INF").resolve(runnerJar.name).toFile()
        runnerJar.copyTo(destination, true)
    }
}

tasks.jar {
    dependsOn(copyRunner)
}