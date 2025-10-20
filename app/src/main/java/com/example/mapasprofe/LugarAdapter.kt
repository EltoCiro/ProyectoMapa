package com.example.mapasprofe

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adaptador personalizado para el RecyclerView que muestra una lista de lugares.
 * Este adaptador gestiona la visualización de los lugares y maneja eventos de clic en los elementos.
 *
 * @param lugares Lista mutable de lugares a mostrar.
 * @param onItemClick Función lambda que se ejecuta cuando se hace clic en un lugar.
 * @param onEditClick Función lambda que se ejecuta cuando se hace clic en el botón de editar.
 * @param onDeleteClick Función lambda que se ejecuta cuando se hace clic en el botón de eliminar.
 */
class LugarAdapter(
    private var lugares: MutableList<Lugar>,
    private val onItemClick: (Lugar) -> Unit,
    private val onEditClick: (Lugar) -> Unit,
    private val onDeleteClick: (Lugar) -> Unit
) : RecyclerView.Adapter<LugarAdapter.LugarViewHolder>() {

    /**
     * Posición del elemento actualmente seleccionado en la lista.
     * Se utiliza para resaltar visualmente el elemento seleccionado.
     */
    private var selectedPosition = 0

    /**
     * Clase interna que representa cada elemento de la lista en el RecyclerView.
     * Contiene referencias a las vistas del elemento individual.
     */
    class LugarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)       // TextView para el título del lugar
        val tvCoordenadas: TextView = itemView.findViewById(R.id.tvCoordenadas) // TextView para las coordenadas
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)     // Botón para editar el lugar
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete) // Botón para eliminar el lugar
    }

    /**
     * Crea y devuelve un nuevo ViewHolder.
     * Este metodo se llama cuando el RecyclerView necesita un nuevo ViewHolder para mostrar un elemento.
     *
     * @param parent El ViewGroup padre donde se inflará la vista.
     * @param viewType Tipo de vista (no se usa en este caso).
     * @return Un nuevo LugarViewHolder que contiene las vistas del elemento.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LugarViewHolder {
        // Inflar el diseño del elemento desde el archivo XML
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lugar, parent, false)
        // Crear y devolver un nuevo ViewHolder con la vista inflada
        return LugarViewHolder(view)
    }

    /**
     * Vincula los datos de un lugar a las vistas del ViewHolder en la posición especificada.
     * Este método se llama para mostrar los datos en un ViewHolder específico.
     *
     * @param holder El ViewHolder que contiene las vistas del elemento.
     * @param position La posición del elemento en la lista.
     */
    override fun onBindViewHolder(holder: LugarViewHolder, position: Int) {
        // Obtener el lugar en la posición actual
        val lugar = lugares[position]

        // Establecer el título del lugar en el TextView correspondiente
        holder.tvTitulo.text = lugar.titulo

        // Formatear y establecer las coordenadas del lugar en el TextView correspondiente
        // String.format("%.5f", ...) formatea el número a 5 decimales
        holder.tvCoordenadas.text = "Lat: ${String.format("%.5f", lugar.latitud)}, Lon: ${String.format("%.5f", lugar.longitud)}"

        // Resaltar el elemento seleccionado cambiando el color de fondo
        if (position == selectedPosition) {
            // Si es el elemento seleccionado, establecer un color de fondo destacado
            holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.selected_item))
        } else {
            // Si no es el elemento seleccionado, establecer un fondo transparente
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        // Configurar el listener para clics en el elemento
        holder.itemView.setOnClickListener {
            // Guardar la posición anterior
            val oldPosition = selectedPosition
            // Actualizar la posición seleccionada
            selectedPosition = holder.adapterPosition
            // Notificar al adaptador que los elementos en las posiciones antigua y nueva han cambiado
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
            // Llamar al callback de clic en el elemento
            onItemClick(lugar)
        }

        // Configurar el listener para el botón de editar
        holder.btnEdit.setOnClickListener {
            // Llamar al callback de clic en el botón de editar
            onEditClick(lugar)
        }

        // Configurar el listener para el botón de eliminar
        holder.btnDelete.setOnClickListener {
            // Llamar al callback de clic en el botón de eliminar
            onDeleteClick(lugar)
        }
    }

    /**
     * Devuelve el número total de elementos en la lista.
     *
     * @return El tamaño de la lista de lugares.
     */
    override fun getItemCount(): Int = lugares.size

    /**
     * Actualiza la lista de lugares con una nueva lista.
     * Este metodo se utiliza para actualizar los datos mostrados en el RecyclerView.
     *
     * @param newLugares Nueva lista de lugares.
     */
    fun updateLugares(newLugares: List<Lugar>) {
        // Actualizar la lista de lugares con la nueva lista
        lugares = newLugares.toMutableList()

        // Asegurarse de que la posición seleccionada sea válida después de la actualización
        if (selectedPosition >= lugares.size) {
            selectedPosition = if (lugares.isNotEmpty()) 0 else -1
        }

        // Notificar al adaptador que los datos han cambiado
        notifyDataSetChanged()
    }

    /**
     * Devuelve la posición actualmente seleccionada.
     *
     * @return La posición del elemento seleccionado.
     */
    fun getSelectedPosition(): Int = selectedPosition

    /**
     * Establece la posición seleccionada y notifica al adaptador para que actualice la vista.
     *
     * @param position La nueva posición seleccionada.
     */
    fun setSelectedPosition(position: Int) {
        // Guardar la posición anterior
        val oldPosition = selectedPosition
        // Actualizar la posición seleccionada
        selectedPosition = position
        // Notificar al adaptador que los elementos en las posiciones antigua y nueva han cambiado
        notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }
}
