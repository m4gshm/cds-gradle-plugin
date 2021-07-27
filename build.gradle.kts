import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.github.m4gshm.cds.gradle"
version = "0.0.2-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.4.20"
    `java-gradle-plugin`

    `maven-publish`
    id("com.gradle.plugin-publish") version "0.14.0"
}

repositories {
    mavenCentral()
}

val dryRunner = ":dry-runner"
dependencies {
    api("org.apache.bcel:bcel:6.5.0")

    compileOnly(project(dryRunner))
    compileOnly(gradleApi())
    compileOnly("org.springframework.boot:spring-boot-gradle-plugin:2.3.10.RELEASE")


    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation(gradleApi())
    testImplementation(project(":test-app"))
}

tasks.test {
    dependsOn(":test-app:jar")
    useJUnit()

    val jar = tasks.findByPath("test-app:jar") as Jar
    val file = jar.archiveFile.get().asFile
    jvmArgs("-Dcds.test.jar=$file")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

tasks.compileTestJava {
    targetCompatibility = JavaVersion.VERSION_11.majorVersion
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
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
            id = "com.github.m4gshm.cds"
            displayName = "Class Data Sharing (CDS) gradle plugin"
            description = "helper for generating and using shared classes archive in your applications."
            implementationClass = "com.github.m4gshm.cds.gradle.CdsPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/m4gshm/cds-gradle-plugin"
    vcsUrl = "https://github.com/m4gshm/cds-gradle-plugin"
    tags = listOf("cds", "jsa")
}

publishing {
    repositories {
        maven {
            name = "localPlugins"
            url = uri("${System.getProperty("user.home")}/gradle-plugin-repository")
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