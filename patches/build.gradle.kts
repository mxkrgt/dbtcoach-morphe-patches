group = "app.dbtcoach.patches"

patches {
    about {
        name = "DBT Coach Morphe Patches"
        description = "Patches Morphe pour l'application DBT Coach — déverrouille les fonctionnalités premium."
        source = "https://github.com/mxkrgt/dbtcoach-morphe-patches"
        author = "mxkrgt"
        contact = "na"
        website = "na"
        license = "GPLv3"
    }
}

val patchListGeneratorClasspath: Configuration by configurations.creating

dependencies {
    compileOnly(libs.morphe.patcher)

    // Used by PatchListGenerator at runtime.
    implementation(libs.gson)

    // Required due to smali, or build fails.
    implementation(libs.guava)

    // Needed at runtime for generatePatchesList task (compileOnly excluded from runtimeClasspath).
    patchListGeneratorClasspath(libs.morphe.patcher)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"
        dependsOn(build)
        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("app.morphe.util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
}
