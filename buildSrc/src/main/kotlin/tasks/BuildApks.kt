package tasks

import com.android.tools.build.bundletool.androidtools.Aapt2Command
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.model.Password
import com.android.tools.build.bundletool.model.SigningConfiguration
import java.util.Optional
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class BuildApks : DefaultTask() {
    @get:InputFile abstract val aapt2Executable: RegularFileProperty

    @get:InputFile abstract val aabFile: RegularFileProperty

    @get:InputFile abstract val keyStoreFile: RegularFileProperty

    @get:Input abstract val keyStorePassword: Property<String>
    @get:Input abstract val keyAlias: Property<String>
    @get:Input abstract val keyPassword: Property<String>

    @get:OutputFile abstract val apksFile: RegularFileProperty

    @TaskAction
    fun generateApks() {
        BuildApksCommand.builder()
            .setBundlePath(aabFile.get().asFile.toPath())
            .setOutputFile(apksFile.get().asFile.toPath())
            .setAapt2Command(
                Aapt2Command.createFromExecutablePath(aapt2Executable.get().asFile.toPath())
            )
            .setSigningConfiguration(
                SigningConfiguration.extractFromKeystore(
                    keyStoreFile.get().asFile.toPath(),
                    keyAlias.get(),
                    Optional.of(Password.createFromStringValue("pass:" + keyStorePassword.get())),
                    Optional.of(Password.createFromStringValue("pass:" + keyPassword.get())),
                )
            )
            .build()
            .execute()
    }
}
