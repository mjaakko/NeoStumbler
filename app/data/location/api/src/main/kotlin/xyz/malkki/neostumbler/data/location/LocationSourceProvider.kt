package xyz.malkki.neostumbler.data.location

fun interface LocationSourceProvider {
    suspend fun getLocationSource(): LocationSource
}
