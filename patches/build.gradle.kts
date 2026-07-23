group = "app.dbtcoach.patches"

plugins {
    alias(libs.plugins.morphe.patches)
}

morphePatches {
    about {
        name = "DBT Coach Morphe Patches"
        description = "Patches Morphe pour l'application DBT Coach (co.swasth.dbtcoach) — déverrouille les fonctionnalités premium."
        source = "https://github.com/mxkrgt/dbtcoach-morphe-patches"
        // Remplace YOUR_USERNAME par ton nom d'utilisateur GitHub
    }
}

dependencies {
    implementation(libs.morphe.patcher)
}
