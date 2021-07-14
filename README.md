# cds-gradle-plugin

# Use case
````
val appMainClassName = "app.Main"
tasks.getByName<m4gshm.gradle.plugin.cds.SharedClassesDump>("sharedClassesDump") {
    classpath = files(tasks.getByName<Jar>("jar").archiveFile)
}

tasks.getByName<m4gshm.gradle.plugin.cds.SharedClassesList>("sharedClassesList") {
    dependsOn("jar")
    dryRunMainClass = appMainClassName
}

tasks.jar {
    val jarDir = archiveFile.get().asFile.parent
    doFirst {
        val libsDir = "lib"
        val classpath = project.configurations["runtimeClasspath"].files
        classpath.forEach { it.copyTo(File(jarDir + "/" + libsDir + "/" + it.name), true) }
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to appMainClassName,
                    "Class-Path" to classpath.joinToString(" ") { libsDir + "/" + it.name }
                )
            )
        }
    }
}
````