plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.apply {
        encoding = "UTF-8"
        debugOptions.debugLevel = "source,lines,vars"
    }
}