pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "Projeto5369"
include(":core", ":app")

include(":newtube-shared"); project(":newtube-shared").projectDir = file("native/shared")
include(":newtube-interfaces"); project(":newtube-interfaces").projectDir = file("native/interfaces")
include(":newtube-v8"); project(":newtube-v8").projectDir = file("native/v8")
include(":newtube-api"); project(":newtube-api").projectDir = file("native/api")
include(":newtube-sabr"); project(":newtube-sabr").projectDir = file("native/sabr")
