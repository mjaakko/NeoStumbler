package constants

import com.android.build.api.dsl.CompileSdkSpec
import com.android.build.api.dsl.MinSdkSpec
import com.android.build.api.dsl.TargetSdkSpec

object AndroidSdkVersions {
    val CompileSdkSpec.COMPILE_SDK
        get() =
            release(37) {
                minorApiLevel = 1
            }

    val TargetSdkSpec.TARGET_SDK
        get() = release(37)

    val MinSdkSpec.MIN_SDK
        get() = release(29)
}
