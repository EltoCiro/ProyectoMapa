package com.example.mapasprofe

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAgregarLugar: Button
    private lateinit var lugarAdapter: LugarAdapter
    private lateinit var lugaresManager: LugaresManager
    private var lugares = mutableListOf<Lugar>()
    private var selectedCircle: Polygon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configurar la ruta de caché de OSMDroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar componentes
        mapView = findViewById(R.id.map)
        recyclerView = findViewById(R.id.recyclerViewLugares)
        btnAgregarLugar = findViewById(R.id.btnAgregarLugar)
        lugaresManager = LugaresManager(this)

        // Configurar el MapView
        setupMap()

        // Configurar RecyclerView
        setupRecyclerView()

        // SIEMPRE inicializar las 19 ubicaciones del Campus Central al inicio
        inicializarLugaresCampus()

        // Cargar lugares (ya incluye las 19 ubicaciones + las personalizadas)
        cargarLugares()

        // Configurar botón agregar
        btnAgregarLugar.setOnClickListener {
            mostrarDialogoAgregarLugar()
        }
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.controller.setZoom(16.0)
        mapView.setMultiTouchControls(true)

        // Centrar en Campus Central Universidad de Colima
        val campusCenter = GeoPoint(19.24914, -103.69740)
        mapView.controller.setCenter(campusCenter)
    }

    private fun setupRecyclerView() {
        lugarAdapter = LugarAdapter(
            lugares,
            onItemClick = { lugar ->
                // Al hacer clic en un lugar de la lista
                mostrarLugarEnMapa(lugar)
            },
            onEditClick = { lugar ->
                mostrarDialogoEditarLugar(lugar)
            },
            onDeleteClick = { lugar ->
                mostrarDialogoEliminar(lugar)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = lugarAdapter
    }

    private fun cargarLugares() {
        lugares.clear()
        lugares.addAll(lugaresManager.cargarLugares())
        lugarAdapter.updateLugares(lugares)
        actualizarMarcadores()

        // Mostrar círculo en el primer lugar si existe
        if (lugares.isNotEmpty()) {
            mostrarLugarEnMapa(lugares[0])
        }
    }

    private fun actualizarMarcadores() {
        // Limpiar marcadores existentes (excepto el círculo)
        mapView.overlays.removeAll { it is Marker }

        // Agregar marcadores para cada lugar
        lugares.forEach { lugar ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(lugar.latitud, lugar.longitud)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = lugar.titulo
            marker.setOnMarkerClickListener { clickedMarker, _ ->
                Toast.makeText(this, clickedMarker.title, Toast.LENGTH_SHORT).show()
                // Encontrar el lugar en la lista y seleccionarlo
                val index = lugares.indexOfFirst {
                    it.latitud == clickedMarker.position.latitude &&
                    it.longitud == clickedMarker.position.longitude
                }
                if (index != -1) {
                    lugarAdapter.setSelectedPosition(index)
                    mostrarCirculo(clickedMarker.position)
                }
                true
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    private fun mostrarLugarEnMapa(lugar: Lugar) {
        val point = GeoPoint(lugar.latitud, lugar.longitud)
        mapView.controller.animateTo(point)
        mostrarCirculo(point)
    }

    private fun mostrarCirculo(point: GeoPoint) {
        // Eliminar círculo anterior
        selectedCircle?.let { mapView.overlays.remove(it) }

        // Crear nuevo círculo
        selectedCircle = Polygon().apply {
            points = Polygon.pointsAsCircle(point, 30.0) // 30 metros de radio
            fillPaint.color = 0x12FF0000 // Rojo semi-transparente
            outlinePaint.color = 0xFFFF0000.toInt() // Rojo
            outlinePaint.strokeWidth = 3f
        }
        mapView.overlays.add(selectedCircle)
        mapView.invalidate()
    }

    private fun mostrarDialogoAgregarLugar() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_lugar, null)
        val etTitulo = dialogView.findViewById<EditText>(R.id.etTitulo)
        val etLatitud = dialogView.findViewById<EditText>(R.id.etLatitud)
        val etLongitud = dialogView.findViewById<EditText>(R.id.etLongitud)

        AlertDialog.Builder(this)
            .setTitle("Agregar Nuevo Lugar")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val titulo = etTitulo.text.toString()
                val latitudStr = etLatitud.text.toString()
                val longitudStr = etLongitud.text.toString()

                if (titulo.isNotEmpty() && latitudStr.isNotEmpty() && longitudStr.isNotEmpty()) {
                    try {
                        val lugar = Lugar(
                            titulo = titulo,
                            latitud = latitudStr.toDouble(),
                            longitud = longitudStr.toDouble()
                        )
                        lugaresManager.agregarLugar(lugar)
                        cargarLugares()
                        Toast.makeText(this, "Lugar agregado exitosamente", Toast.LENGTH_SHORT).show()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Coordenadas inválidas", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditarLugar(lugar: Lugar) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_lugar, null)
        val etTitulo = dialogView.findViewById<EditText>(R.id.etTitulo)
        val etLatitud = dialogView.findViewById<EditText>(R.id.etLatitud)
        val etLongitud = dialogView.findViewById<EditText>(R.id.etLongitud)

        // Pre-llenar con datos actuales
        etTitulo.setText(lugar.titulo)
        etLatitud.setText(lugar.latitud.toString())
        etLongitud.setText(lugar.longitud.toString())

        AlertDialog.Builder(this)
            .setTitle("Editar Lugar")
            .setView(dialogView)
            .setPositiveButton("Actualizar") { _, _ ->
                val titulo = etTitulo.text.toString()
                val latitudStr = etLatitud.text.toString()
                val longitudStr = etLongitud.text.toString()

                if (titulo.isNotEmpty() && latitudStr.isNotEmpty() && longitudStr.isNotEmpty()) {
                    try {
                        lugar.titulo = titulo
                        lugar.latitud = latitudStr.toDouble()
                        lugar.longitud = longitudStr.toDouble()
                        lugaresManager.actualizarLugar(lugar)
                        cargarLugares()
                        Toast.makeText(this, "Lugar actualizado exitosamente", Toast.LENGTH_SHORT).show()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(this, "Coordenadas inválidas", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEliminar(lugar: Lugar) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Lugar")
            .setMessage("¿Está seguro de eliminar '${lugar.titulo}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                lugaresManager.eliminarLugar(lugar)
                cargarLugares()
                Toast.makeText(this, "Lugar eliminado exitosamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun inicializarLugaresCampus() {
        val sharedPrefs = getSharedPreferences("campus_init", MODE_PRIVATE)
        val yaInicializado = sharedPrefs.getBoolean("inicializado", false)

        // Siempre cargar las ubicaciones base del campus
        val lugaresCampusBase = listOf(
            Lugar(id = 1001, titulo = "Facultad de Telemática", latitud = 19.24904, longitud = -103.69729),
            Lugar(id = 1002, titulo = "Rectoría Universidad de Colima", latitud = 19.24590, longitud = -103.69695),
            Lugar(id = 1003, titulo = "Facultad de Contabilidad y Administración", latitud = 19.24830, longitud = -103.69640),
            Lugar(id = 1004, titulo = "Facultad de Ciencias", latitud = 19.24950, longitud = -103.69890),
            Lugar(id = 1005, titulo = "Facultad de Ingeniería Civil", latitud = 19.24885, longitud = -103.69610),
            Lugar(id = 1006, titulo = "Biblioteca Central", latitud = 19.24760, longitud = -103.69740),
            Lugar(id = 1007, titulo = "Centro Universitario de Investigaciones Sociales", latitud = 19.24640, longitud = -103.69810),
            Lugar(id = 1008, titulo = "Facultad de Pedagogía", latitud = 19.24595, longitud = -103.69920),
            Lugar(id = 1009, titulo = "Facultad de Letras y Comunicación", latitud = 19.24710, longitud = -103.69530),
            Lugar(id = 1010, titulo = "Facultad de Psicología", latitud = 19.24820, longitud = -103.69900),
            Lugar(id = 1011, titulo = "Cafetería Central", latitud = 19.24680, longitud = -103.69760),
            Lugar(id = 1012, titulo = "Auditorio Universitario", latitud = 19.24570, longitud = -103.69690),
            Lugar(id = 1013, titulo = "Centro de Idiomas", latitud = 19.24930, longitud = -103.69710),
            Lugar(id = 1014, titulo = "Gimnasio Universitario", latitud = 19.24450, longitud = -103.69850),
            Lugar(id = 1015, titulo = "Alberca Olímpica", latitud = 19.24400, longitud = -103.69760),
            Lugar(id = 1016, titulo = "Estadio Universitario", latitud = 19.24330, longitud = -103.69930),
            Lugar(id = 1017, titulo = "Centro de Cómputo", latitud = 19.24865, longitud = -103.69540),
            Lugar(id = 1018, titulo = "Facultad de Derecho", latitud = 19.24610, longitud = -103.69570),
            Lugar(id = 1019, titulo = "Centro de Investigación Científica y Educación Superior", latitud = 19.24785, longitud = -103.69830),
            Lugar(id = 1020, titulo = "Plaza Cívica Campus Central", latitud = 19.24635, longitud = -103.69725)
        )

        // Obtener lugares guardados
        val lugaresGuardados = lugaresManager.cargarLugares()

        // Filtrar solo lugares del campus (IDs 1001-1020) de los guardados
        val lugaresCampusGuardados = lugaresGuardados.filter { it.id in 1001..1020 }

        // Filtrar lugares personalizados (usuarios agregaron)
        val lugaresPersonalizados = lugaresGuardados.filter { it.id !in 1001..1020 }

        // Crear lista combinada: siempre las 19 ubicaciones base + las personalizadas
        val lugaresCombinados = mutableListOf<Lugar>()
        lugaresCombinados.addAll(lugaresCampusBase)
        lugaresCombinados.addAll(lugaresPersonalizados)

        // Guardar la lista completa
        lugaresManager.guardarLugares(lugaresCombinados)

        if (!yaInicializado) {
            sharedPrefs.edit().putBoolean("inicializado", true).apply()
            Toast.makeText(this, "19 ubicaciones del Campus Central cargadas", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}