# cds-gradle-plugin

# Use case
````kotlin
val appMainClassName = "app.Main"

tasks.getByName<m4gshm.gradle.plugin.cds.SharedClassesDump>("sharedClassesDump") {
    classpath = files(tasks.getByName<Jar>("jar").archiveFile)
}

tasks.getByName<m4gshm.gradle.plugin.cds.SharedClassesList>("sharedClassesList") {
    dependsOn("jar")
    dryRunMainClass = appMainClassName
}

tasks.jar {
    project.subprojects.forEach { subProject ->
        val subJar = subProject.tasks.findByName("jar")
        if (subJar != null) {
            dependsOn(subJar)
        }
    }

    val jarDir = this.archiveFile.get().asFile.parentFile
    doFirst {
        val libsDirName = "lib"
        val absoluteLibsDir = jarDir.toPath().resolve(libsDirName).toFile()
        absoluteLibsDir.mkdirs()
        val classpath = project.configurations["runtimeClasspath"].files
        classpath.forEach { it ->
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
    val sharedClassesDump = tasks.getByName<m4gshm.gradle.plugin.cds.SharedClassesDump>("sharedClassesDump")
    dependsOn(sharedClassesDump)
    group = "application"
    classpath = files()
    mainClass.set("")
    doFirst {
        val sharedClassesFile = sharedClassesDump.outputFile.asFile.get()
        jvmArgs(
            "-Xshare:on",
            "-XX:SharedArchiveFile=$sharedClassesFile",
            "-jar", tasks.getByName<Jar>("jar").archiveFile.get().asFile.absolutePath
        )
    }
}
````