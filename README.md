# cds-gradle-plugin

# Use case

**build.gradle.kts**

````kotlin
plugins {
    id("com.github.m4gshm.cds") version "0.0.2"
}

cds {
    mainClass = "app.Main"
}

````
Replace ```mainClass``` value by your main class and execute the next command:
````shell
./gradlew runSharedClassesJar
````

#Build docker image example

**build.gradle.kts**

```kotlin
plugins {
    id("com.bmuschko.docker-java-application") version "7.1.0"
    id("com.github.m4gshm.cds") version "0.0.2"
}

cds {
    mainClass = "app.Main"
}

val sharedClassedDockerfile = tasks.create("sharedClassedDockerfile", com.bmuschko.gradle.docker.tasks.image.Dockerfile::class.java) {
    group = "cds"
    dependsOn(tasks.sharedClassesList)

    from("openjdk:11.0.7-jdk")

    val sharedClassesJar = tasks.sharedClassesJar.get()
    val archiveFile = sharedClassesJar.archiveFile.get().asFile

    val destPath = "/app/"
    val destJar = destPath + archiveFile.name
    addFile(archiveFile.name, destJar)
    val libsDir = sharedClassesJar.libsDir.asFile.get()
    addFile(libsDir.name, destPath + libsDir.name)

    val sharedClassesList = tasks.sharedClassesList.get()
    val classesTxt = sharedClassesList.dumpLoadedClassList.get().asFile

    val destClassesTxt = destPath + classesTxt.name
    addFile(classesTxt.name, destClassesTxt)

    val destSharedArchiveFile = destPath + "shared.jsa"
    runCommand(
        "java" +
                " -Xshare:dump" +
                " -XX:SharedClassListFile=$destClassesTxt" +
                " -XX:SharedArchiveFile=$destSharedArchiveFile" +
                " -cp $destJar"
    )

    workingDir(destPath)

    environmentVariable("CDS_SHARE", "on")
    instruction("CMD java \$JAVA_OPTS -Xshare:\$CDS_SHARE -XX:SharedArchiveFile=$destSharedArchiveFile -jar $destJar")

//    exposePort(8080)

    val stageDir = destDir.get().asFile

    inputs.apply {
        file(classesTxt)
        file(archiveFile)
        dir(libsDir)
    }
    outputs.dir(stageDir)

    doFirst {
        copy {
            from(classesTxt)
            into(stageDir)
        }
        copy {
            from(archiveFile)
            into(stageDir)
        }
        copy {
            from(libsDir)
            into(File(stageDir, libsDir.name))
        }
    }
}

tasks.create("sharedClassedDockerBuildImage", com.bmuschko.gradle.docker.tasks.image.DockerBuildImage::class.java) {
    group = "cds"
    dependsOn(sharedClassedDockerfile)
    inputDir.set(sharedClassedDockerfile.destDir)
    images.set(listOf("cds-example:latest"))
}
```

execute:
````shell
./gradlew sharedClassedDockerBuildImage
docker run -ti --rm cds-example:latest
#or
docker run -ti --rm -e CDS_SHARE=off cds-example:latest #to disable cds usage

````
