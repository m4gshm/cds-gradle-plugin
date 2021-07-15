# cds-gradle-plugin

# Use case

**build.gradle.kts**

````kotlin
plugins {
    id("m4gshm.gradle.plugin.cds") version "0.0.1"
}

cds {
    mainClass.set("app.Main")
}

````
Replace ```mainClass``` value by your main class and execute the next command:
````bash
./gradlew runSharedClassesJar
````