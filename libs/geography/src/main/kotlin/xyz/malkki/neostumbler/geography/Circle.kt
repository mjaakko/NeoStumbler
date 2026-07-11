package xyz.malkki.neostumbler.geography

data class Circle(
    val center: LatLng,
    /** Radius in meters */
    val radius: Double,
) {
    companion object {
        /** @return `true` if the point is inside the [circle] */
        fun LatLng.isInside(circle: Circle): Boolean {
            return distanceTo(circle.center) <= circle.radius
        }
    }
}
