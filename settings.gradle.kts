rootProject.name = "cds-gradle-plugin"
val dryRunner = "dry-runner"
include(":$dryRunner")
project(":$dryRunner").projectDir = file(dryRunner)