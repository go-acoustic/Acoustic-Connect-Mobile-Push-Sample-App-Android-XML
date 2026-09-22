pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.huawei.agconnect") {
                useModule("com.huawei.agconnect:agcp:${requested.version}")
            }
        }
    }
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
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Beta builds of the Connect SDK (versions ending in -beta) are not on Maven Central: they
        // are served from the go-acoustic/Android_Maven GitHub repository as a plain Maven
        // repository (CA-157153). Scoped to the SDK's group so everything else still resolves
        // exactly as before, and listed after mavenCentral() so release versions are still taken
        // from Central. Not needed once `connectPush` points at a release version.
        maven {
            url = uri("https://raw.githubusercontent.com/go-acoustic/Android_Maven/master")
            content { includeGroup("io.github.go-acoustic") }
        }
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}


rootProject.name = "Connect Demo Application"
include(":app")
 
// ── Local SDK for development ─────────────────────────────────────────────────
// `./gradlew :app:assembleDebug -PconnectSdk=local` builds this sample against the in-tree
// SDK source (AndroidSrc/AndroidStudioConnect) instead of the published Maven artifacts, so
// SDK changes can be run and debugged here without publishing anything. Without the property
// the sample resolves `connectPush` from Maven Central exactly as a partner's clone does, and
// in a standalone clone of this repo the block is inert because the property is never set.
if (providers.gradleProperty("connectSdk").orNull == "local") {
    val sdkRoot = file("../../../AndroidSrc/AndroidStudioConnect")
    require(sdkRoot.isDirectory) { "connectSdk=local but $sdkRoot is not the Android-SDK checkout" }
    includeBuild(sdkRoot) {
        dependencySubstitution {
            substitute(module("io.github.go-acoustic:connect-push")).using(project(":connect-push"))
            substitute(module("io.github.go-acoustic:connect-push-fcm")).using(project(":connect-push-fcm"))
            substitute(module("io.github.go-acoustic:connect-push-hms")).using(project(":connect-push-hms"))
            substitute(module("io.github.go-acoustic:connect")).using(project(":connect"))
        }
    }
}
