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
````bash
./gradlew runSharedClassesJar
````