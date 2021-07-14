# cds-gradle-plugin

# Use case
````kotlin
val appMainClassName = "app.Main"

tasks.create<Jar>("sharedClassesJar") {
    dependsOn("build")
    group = "cds"
    destinationDirectory.set(file("$buildDir/cds/jar"))

    val jarTask = tasks.getByName<Jar>("jar")
    with(jarTask)
    inputs.files(jarTask.inputs.files)

    val jarFile = this.archiveFile.get().asFile
    project.logger.warn("jarFile $jarFile")
    val jarDir = jarFile.parentFile
    doFirst {
        val libsDirName = "lib"
        val absoluteLibsDir = jarDir.toPath().resolve(libsDirName).toFile()
        absoluteLibsDir.mkdirs()
        val classpath = project.configurations["runtimeClasspath"].files
        classpath.forEach {
            val target = File(absoluteLibsDir, it.name)
            project.logger.info("copy $it -> $target")
            it.copyTo(target, true)
        }
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to appMainClassName,
                    "Class-Path" to classpath.joinToString(" ") { libsDirName + "/" + it.name }
                )
            )
        }
    }
}

tasks.create<JavaExec>("runJarShareOn") {
    val sharedClassesDumpTask = tasks.getByName<m4gshm.gradle.plugin.cds.SharedClassesDump>("sharedClassesDump")
    val sharedClassesJarTask = tasks.getByName<Jar>("sharedClassesJar")
    dependsOn(sharedClassesDumpTask)
    group = "application"
    classpath = files()
    mainClass.set("")
    doFirst {
        val sharedClassesFile = sharedClassesDumpTask.outputFile.asFile.get()
        jvmArgs(
            "-Xshare:on",
            "-XX:SharedArchiveFile=$sharedClassesFile",
            "-jar", sharedClassesJarTask.archiveFile.get().asFile.absolutePath
        )
    }
}

tasks.getByName<m4gshm.gradle.plugin.cds.SharedClassesDump>("sharedClassesDump") {
    val jar = tasks.getByName<Jar>("sharedClassesJar").archiveFile
    classpath = files(jar)
    inputs.file(jar)
}

tasks.getByName<m4gshm.gradle.plugin.cds.SharedClassesList>("sharedClassesList") {
    dependsOn("sharedClassesJar")
    dryRunMainClass = appMainClassName
}

````

replace ```appMainClassName``` value by yours main class name and run next command:

````bash
./gradlew runJarShareOn
````