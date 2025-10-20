package com.example.mapasprofe

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Clase que gestiona el almacenamiento y recuperación de lugares utilizando SharedPreferences y Gson.
 * Permite guardar, cargar, agregar, actualizar y eliminar lugares.
 */
class LugaresManager(private val context: Context) {

    /**
     * Instancia de SharedPreferences para almacenar los datos de manera persistente.
     * "lugares_prefs" es el nombre del archivo de preferencias donde se guardarán los lugares.
     */
    private val sharedPreferences = context.getSharedPreferences("lugares_prefs", Context.MODE_PRIVATE)

    /**
     * Instancia de Gson para convertir objetos a JSON y viceversa.
     */
    private val gson = Gson()

    companion object {
        /**
         * Clave utilizada para guardar y recuperar la lista de lugares en SharedPreferences.
         */
        private const val KEY_LUGARES = "lugares_list"
    }

    /**
     * Guarda una lista de lugares en SharedPreferences.
     *
     * @param lugares Lista de lugares a guardar.
     */
    fun guardarLugares(lugares: List<Lugar>) {
        // Convierte la lista de lugares a formato JSON
        val json = gson.toJson(lugares)
        // Guarda el JSON en SharedPreferences
        sharedPreferences.edit().putString(KEY_LUGARES, json).apply()
    }

    /**
     * Carga la lista de lugares desde SharedPreferences.
     *
     * @return Lista mutable de lugares. Si no hay lugares guardados, devuelve una lista vacía.
     */
    fun cargarLugares(): MutableList<Lugar> {
        // Obtiene el JSON guardado en SharedPreferences
        val json = sharedPreferences.getString(KEY_LUGARES, null)
        return if (json != null) {
            // Convierte el JSON de vuelta a una lista de lugares
            val type = object : TypeToken<MutableList<Lugar>>() {}.type
            gson.fromJson(json, type)
        } else {
            // Si no hay JSON guardado, devuelve una lista vacía
            mutableListOf()
        }
    }

    /**
     * Agrega un nuevo lugar a la lista de lugares guardados.
     *
     * @param lugar Lugar a agregar.
     */
    fun agregarLugar(lugar: Lugar) {
        // Carga los lugares actuales
        val lugares = cargarLugares()
        // Agrega el nuevo lugar a la lista
        lugares.add(lugar)
        // Guarda la lista actualizada
        guardarLugares(lugares)
    }

    /**
     * Actualiza un lugar existente en la lista de lugares guardados.
     *
     * @param lugar Lugar con los datos actualizados.
     */
    fun actualizarLugar(lugar: Lugar) {
        // Carga los lugares actuales
        val lugares = cargarLugares()
        // Busca el índice del lugar a actualizar
        val index = lugares.indexOfFirst { it.id == lugar.id }
        if (index != -1) {
            // Si encuentra el lugar, lo actualiza
            lugares[index] = lugar
            // Guarda la lista actualizada
            guardarLugares(lugares)
        }
    }

    /**
     * Elimina un lugar de la lista de lugares guardados.
     *
     * @param lugar Lugar a eliminar.
     */
    fun eliminarLugar(lugar: Lugar) {
        // Carga los lugares actuales
        val lugares = cargarLugares()
        // Filtra los lugares a eliminar
        val lugaresToDelete = lugares.filter { it.id == lugar.id }
        // Elimina los lugares filtrados
        lugares.removeAll(lugaresToDelete)
        // Guarda la lista actualizada
        guardarLugares(lugares)
    }
}
