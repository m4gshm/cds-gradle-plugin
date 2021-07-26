rootProject.name = "cds-gradle-plugin"
val dryRunner = "dry-runner"
include(":$dryRunner")
project(":$dryRunner").projectDir = file(dryRunner)
include(":test-app")

//C:\Users\mfour\AppData\Local\Temp\gradle4907590541184912824projectDir\build\cds\jar\test.jar;
// C:\Users\mfour\AppData\Local\Temp\gradle4907590541184912824projectDir\build\cds\dry-runner.jar