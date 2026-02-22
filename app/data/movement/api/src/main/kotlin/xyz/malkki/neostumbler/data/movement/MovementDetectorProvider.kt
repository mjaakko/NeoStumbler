package xyz.malkki.neostumbler.data.movement

fun interface MovementDetectorProvider {
    suspend fun getMovementDetector(): MovementDetector
}
