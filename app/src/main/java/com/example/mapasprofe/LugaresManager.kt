package com.example.mapasprofe

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LugaresManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("lugares_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_LUGARES = "lugares_list"
    }

    fun guardarLugares(lugares: List<Lugar>) {
        val json = gson.toJson(lugares)
        sharedPreferences.edit().putString(KEY_LUGARES, json).apply()
    }

    fun cargarLugares(): MutableList<Lugar> {
        val json = sharedPreferences.getString(KEY_LUGARES, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<Lugar>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    fun agregarLugar(lugar: Lugar) {
        val lugares = cargarLugares()
        lugares.add(lugar)
        guardarLugares(lugares)
    }

    fun actualizarLugar(lugar: Lugar) {
        val lugares = cargarLugares()
        val index = lugares.indexOfFirst { it.id == lugar.id }
        if (index != -1) {
            lugares[index] = lugar
            guardarLugares(lugares)
        }
    }

    fun eliminarLugar(lugar: Lugar) {
        val lugares = cargarLugares()
        val lugaresToDelete = lugares.filter { it.id == lugar.id }
        lugares.removeAll(lugaresToDelete)
        guardarLugares(lugares)
    }
}
