plugins { `kotlin-dsl` }

dependencies {
    implementation(libs.plugins.kotlinJvm.asLibraryDependency)
    implementation(libs.plugins.kotlinAndroid.asLibraryDependency)

    implementation(libs.plugins.androidApplication.asLibraryDependency)
    implementation(libs.plugins.androidLibrary.asLibraryDependency)

    implementation(libs.plugins.ktfmt.asLibraryDependency)

    implementation(libs.plugins.detekt.asLibraryDependency)
}

private val Provider<PluginDependency>.asLibraryDependency: Provider<String>
    get() {
        return map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
    }
