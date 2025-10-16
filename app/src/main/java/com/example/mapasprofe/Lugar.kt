package com.example.mapasprofe

import java.io.Serializable

data class Lugar(
    var id: Long = System.currentTimeMillis(),
    var titulo: String,
    var latitud: Double,
    var longitud: Double
) : Serializable

