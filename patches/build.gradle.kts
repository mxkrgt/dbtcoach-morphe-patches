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

dependencies {
    compileOnly(libs.morphe.patcher)

    // Used by PatchListGenerator at runtime.
    implementation(libs.gson)

    // Required due to smali, or build fails.
    implementation(libs.guava)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"
        dependsOn(build)
        classpath = sourceSets["main"].runtimeClasspath
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
