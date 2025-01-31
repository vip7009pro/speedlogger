package com.hnpage.speedloggernew

data class LocationData(
    val speed: Float,     // Speed in m/s
    val lat: Double,      // Latitude
    val lng: Double       // Longitude
) {
    // Convert speed to km/h
    fun speedKmh() = speed * 3.6f

    // Format coordinates
    fun formattedCoordinates() = "%.6f, %.6f".format(lat, lng)
}