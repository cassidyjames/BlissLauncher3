plugins { `kotlin-dsl` }

dependencies { implementation(libs.build.spotless) }

gradlePlugin {
    plugins {
        register("spotless") {
            id = "foundation.e.bliss.spotless"
            implementationClass = "foundation.e.bliss.SpotlessPlugin"
        }
        register("githooks") {
            id = "foundation.e.bliss.githooks"
            implementationClass = "foundation.e.bliss.GitHooksPlugin"
        }
    }
}
