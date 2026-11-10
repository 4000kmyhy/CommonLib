pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("http://192.168.8.41:8092/nexus/repository/maven-snapshots/")
            isAllowInsecureProtocol = true
        }
        maven {
            setUrl("http://192.168.8.41:8092/nexus/repository/maven-releases/")
            isAllowInsecureProtocol = true
        }
        maven {
            setUrl("http://192.168.8.41:8092/nexus/repository/maven-3rd-party/")
            isAllowInsecureProtocol = true
        }
        maven {
            setUrl("https://jitpack.io")
            isAllowInsecureProtocol = true
        }
        maven {
            setUrl("https://artifact.bytedance.com/repository/pangle")
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "CommonLib"
include(":app")

include(":lib_media")
include(":lib_auto_mix")
include(":lib_bass")
include(":lib_eq_preset")
include(":lib_lyric")
include(":api-lyric")
include(":lib_2048")
include(":lib_retrofit")
//include(":lib_web_pic")
include(":lib_compose")
include(":lib_piano")
