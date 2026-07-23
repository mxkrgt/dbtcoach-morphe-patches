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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

val patchListGeneratorClasspath: Configuration by configurations.creating

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"
        dependsOn(build)
        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    publish {
        dependsOn("generatePatchesList")
    }
}
