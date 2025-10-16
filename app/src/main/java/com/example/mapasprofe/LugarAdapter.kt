package com.example.mapasprofe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LugarAdapter(
    private var lugares: MutableList<Lugar>,
    private val onItemClick: (Lugar) -> Unit,
    private val onEditClick: (Lugar) -> Unit,
    private val onDeleteClick: (Lugar) -> Unit
) : RecyclerView.Adapter<LugarAdapter.LugarViewHolder>() {

    private var selectedPosition = 0

    class LugarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        val tvCoordenadas: TextView = itemView.findViewById(R.id.tvCoordenadas)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LugarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lugar, parent, false)
        return LugarViewHolder(view)
    }

    override fun onBindViewHolder(holder: LugarViewHolder, position: Int) {
        val lugar = lugares[position]
        holder.tvTitulo.text = lugar.titulo
        holder.tvCoordenadas.text = "Lat: ${String.format("%.5f", lugar.latitud)}, Lon: ${String.format("%.5f", lugar.longitud)}"

        // Resaltar el elemento seleccionado
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.selected_item))
        } else {
            holder.itemView.setBackgroundColor(holder.itemView.context.getColor(android.R.color.transparent))
        }

        holder.itemView.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
            onItemClick(lugar)
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(lugar)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(lugar)
        }
    }

    override fun getItemCount() = lugares.size

    fun updateLugares(newLugares: MutableList<Lugar>) {
        lugares = newLugares
        if (selectedPosition >= lugares.size) {
            selectedPosition = if (lugares.isNotEmpty()) 0 else -1
        }
        notifyDataSetChanged()
    }

    fun getSelectedPosition() = selectedPosition

    fun setSelectedPosition(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }
}

