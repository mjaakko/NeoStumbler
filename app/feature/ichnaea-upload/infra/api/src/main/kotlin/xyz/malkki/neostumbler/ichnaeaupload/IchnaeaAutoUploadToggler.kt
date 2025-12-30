package xyz.malkki.neostumbler.ichnaeaupload

import kotlinx.coroutines.flow.Flow

enum class AutoUploadMode {
    NEVER,
    ANY_NETWORK,
    UNMETERED_NETWORK,
}

interface IchnaeaAutoUploadToggler {
    fun getAutoUploadMode(): Flow<AutoUploadMode>

    suspend fun setAutoUploadMode(autoUploadMode: AutoUploadMode)
}
