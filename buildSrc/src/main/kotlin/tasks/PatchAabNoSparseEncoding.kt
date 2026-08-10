package tasks

import com.android.bundle.Config
import java.net.URI
import java.nio.file.FileSystems
import java.util.zip.ZipFile
import kotlin.io.path.writeBytes
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class PatchAabNoSparseEncoding : DefaultTask() {
    @get:InputFile abstract val aabFile: RegularFileProperty

    @get:OutputFile abstract val patchedAabFile: RegularFileProperty

    @TaskAction
    fun patchAab() {
        val buildConfigBytes =
            ZipFile(aabFile.get().asFile).use { zipFile ->
                zipFile.getInputStream(zipFile.getEntry("BundleConfig.pb")).use { it.readBytes() }
            }

        val buildConfig = Config.BundleConfig.parseFrom(buildConfigBytes)

        val patchedBuildConfig =
            buildConfig
                .toBuilder()
                .setOptimizations(
                    buildConfig.optimizations
                        .toBuilder()
                        .setResourceOptimizations(
                            buildConfig.optimizations.resourceOptimizations
                                .toBuilder()
                                .setSparseEncoding(
                                    Config.ResourceOptimizations.SparseEncoding.DISABLED
                                )
                        )
                        .setInjectMinSdkSetting(Config.Optimizations.InjectMinSdkSetting.DISABLED)
                )
                .build()

        aabFile.get().asFile.copyTo(patchedAabFile.get().asFile, overwrite = true)

        val uri = URI.create("jar:file:${patchedAabFile.get().asFile.absolutePath}")

        FileSystems.newFileSystem(uri, emptyMap<String, String>()).use { fs ->
            val targetPath = fs.getPath("BundleConfig.pb")

            targetPath.writeBytes(patchedBuildConfig.toByteArray())
        }
    }
}
