package com.example.mapasprofe

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SearchView
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
    // Declaración de variables globales
    private lateinit var mapView: MapView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAgregarLugar: Button
    private lateinit var btnToggleRecyclerView: Button
    private lateinit var lugarAdapter: LugarAdapter
    private lateinit var lugaresManager: LugaresManager
    private lateinit var searchView: SearchView
    private var lugares = mutableListOf<Lugar>()
    private var selectedCircle: Polygon? = null

    // Método llamado al crear la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_main)

        // Configurar márgenes para las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar componentes
        mapView = findViewById(R.id.map)
        recyclerView = findViewById(R.id.recyclerViewLugares)
        btnAgregarLugar = findViewById(R.id.btnAgregarLugar)
        btnToggleRecyclerView = findViewById(R.id.btnToggleRecyclerView)
        searchView = findViewById(R.id.searchView)
        lugaresManager = LugaresManager(this)

        // Configurar el MapView
        setupMap()

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar SearchView
        setupSearchView()

        // Configurar botón para mostrar/ocultar RecyclerView
        btnToggleRecyclerView.setOnClickListener {
            toggleRecyclerViewVisibility()
        }

        // Inicializar las 19 ubicaciones del Campus Central
        inicializarLugaresCampus()

        // Cargar lugares
        cargarLugares()

        // Configurar botón agregar
        btnAgregarLugar.setOnClickListener {
            mostrarDialogoAgregarLugar()
        }
    }

    // Configurar el mapa
    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(true)
        mapView.controller.setZoom(16.0)
        val campusCenter = GeoPoint(19.24914, -103.69740)
        mapView.controller.setCenter(campusCenter)
    }

    // Configurar la barra de búsqueda
    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText?.trim()?.lowercase() ?: ""
                val lugaresFiltrados = if (query.isEmpty()) {
                    lugares
                } else {
                    lugares.filter { lugar ->
                        lugar.titulo.lowercase().contains(query)
                    }
                }
                lugarAdapter.updateLugares(lugaresFiltrados)
                return true
            }
        })
    }

    // Configurar el RecyclerView
    private fun setupRecyclerView() {
        lugarAdapter = LugarAdapter(
            lugares,
            onItemClick = { lugar ->
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

    // Alternar visibilidad del RecyclerView
    private fun toggleRecyclerViewVisibility() {
        if (recyclerView.visibility == View.GONE) {
            recyclerView.visibility = View.VISIBLE
            btnToggleRecyclerView.text = "Ocultar Lugares"
            val params = recyclerView.layoutParams as LinearLayout.LayoutParams
            params.weight = 1f
            recyclerView.layoutParams = params
        } else {
            recyclerView.visibility = View.GONE
            btnToggleRecyclerView.text = "Mostrar Lugares"
            val params = recyclerView.layoutParams as LinearLayout.LayoutParams
            params.weight = 0f
            recyclerView.layoutParams = params
        }
    }

    // Cargar lugares desde el gestor de lugares
    private fun cargarLugares() {
        lugares.clear()
        lugares.addAll(lugaresManager.cargarLugares())
        lugarAdapter.updateLugares(lugares)
        actualizarMarcadores()
        if (lugares.isNotEmpty()) {
            mostrarLugarEnMapa(lugares[0])
        }
    }

    // Actualizar marcadores en el mapa
    private fun actualizarMarcadores() {
        mapView.overlays.removeAll { it is Marker }
        lugares.forEach { lugar ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(lugar.latitud, lugar.longitud)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = lugar.titulo
            marker.setOnMarkerClickListener { clickedMarker, _ ->
                Toast.makeText(this, clickedMarker.title, Toast.LENGTH_SHORT).show()
                val index = lugares.indexOfFirst {
                    it.latitud == clickedMarker.position.latitude &&
                            it.longitud == clickedMarker.position.longitude
                }
                if (index != -1) {
                    lugarAdapter.setSelectedPosition(index)
                    mostrarCirculo(clickedMarker.position)
                    mapView.controller.animateTo(clickedMarker.position)
                    mapView.controller.setZoom(18.0)
                    recyclerView.smoothScrollToPosition(index)
                }
                true
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    // Mostrar un lugar en el mapa
    private fun mostrarLugarEnMapa(lugar: Lugar) {
        val point = GeoPoint(lugar.latitud, lugar.longitud)
        mapView.controller.animateTo(point)
        mapView.controller.setZoom(18.0)
        mostrarCirculo(point)
    }

    // Mostrar un círculo en el mapa alrededor de un punto
    private fun mostrarCirculo(point: GeoPoint) {
        selectedCircle?.let { mapView.overlays.remove(it) }
        selectedCircle = Polygon().apply {
            points = Polygon.pointsAsCircle(point, 30.0)
            fillPaint.color = 0x12FF0000
            outlinePaint.color = 0xFFFF0000.toInt()
            outlinePaint.strokeWidth = 3f
        }
        mapView.overlays.add(selectedCircle)
        mapView.invalidate()
    }

    // Mostrar diálogo para agregar un lugar
    private fun mostrarDialogoAgregarLugar() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_lugar, null)
        val etTitulo = dialogView.findViewById<android.widget.EditText>(R.id.etTitulo)
        val etLatitud = dialogView.findViewById<android.widget.EditText>(R.id.etLatitud)
        val etLongitud = dialogView.findViewById<android.widget.EditText>(R.id.etLongitud)
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

    // Mostrar diálogo para editar un lugar
    private fun mostrarDialogoEditarLugar(lugar: Lugar) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_lugar, null)
        val etTitulo = dialogView.findViewById<android.widget.EditText>(R.id.etTitulo)
        val etLatitud = dialogView.findViewById<android.widget.EditText>(R.id.etLatitud)
        val etLongitud = dialogView.findViewById<android.widget.EditText>(R.id.etLongitud)
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

    // Mostrar diálogo para eliminar un lugar
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

    // Inicializar lugares del campus
    private fun inicializarLugaresCampus() {
        val sharedPrefs = getSharedPreferences("campus_init", MODE_PRIVATE)
        val yaInicializado = sharedPrefs.getBoolean("inicializado", false)
        val lugaresCampusBase = listOf(
            Lugar(id = 1001, titulo = "Facultad de Telemática", latitud = 19.249197348831245, longitud = -103.69736392365898),
            Lugar(id = 1002, titulo = "Rectoría Universidad de Colima", latitud = 19.248844923842494, longitud = -103.69875992175082),
            Lugar(id = 1003, titulo = "Facultad de Contabilidad y Administración", latitud = 19.249034229016686, longitud = -103.7000273493406),
            Lugar(id = 1004, titulo = "Facultad de Ciencias", latitud = 19.244034959510827, longitud = -103.70286018672438),
            Lugar(id = 1005, titulo = "Facultad de Ingeniería Civil", latitud = 19.212860289021403, longitud = -103.80435467458996),
            Lugar(id = 1006, titulo = "Biblioteca Ciencias de la Salud", latitud = 19.247286485775675, longitud = -103.69754462336094),
            Lugar(id = 1007, titulo = "Centro Universitario de Investigaciones Sociales", latitud = 19.244279327390228, longitud = -103.70141146109604),
            Lugar(id = 1008, titulo = "Facultad de Pedagogía", latitud = 19.266288285542032, longitud = -103.74282020342453),
            Lugar(id = 1009, titulo = "Facultad de Letras y Comunicación", latitud = 19.248357150941153, longitud = -103.6978450457537),
            Lugar(id = 1010, titulo = "Facultad de Psicología", latitud = 19.248626285724974, longitud = -103.69707357077569),
            Lugar(id = 1011, titulo = "Cafetería Central", latitud = 19.249643986414963, longitud = -103.69895211377904),
            Lugar(id = 1012, titulo = "Teatro Universitario", latitud = 19.262466455159373, longitud = -103.68589491308768),
            Lugar(id = 1013, titulo = "Centro de Idiomas", latitud = 19.249462748643307, longitud = -103.698480946333),
            Lugar(id = 1014, titulo = "Gimnasio Universitario", latitud = 19.246463812710846, longitud = -103.69850765530549),
            Lugar(id = 1015, titulo = "Alberca Olímpica", latitud = 19.24598759583211, longitud = -103.70221641876695),
            Lugar(id = 1016, titulo = "Estadio Universitario", latitud = 19.246256812026072, longitud = -103.70113890388686),
            Lugar(id = 1017, titulo = "Centro de Cómputo", latitud = 19.249066455325533, longitud = -103.69915400261941),
            Lugar(id = 1018, titulo = "Facultad de Derecho", latitud = 19.261266216662275, longitud = -103.68722668010331),
            Lugar(id = 1019, titulo = "Centro de Investigación Científica y Educación Superior", latitud = 19.24411025478013, longitud = -103.70140488992524),
            Lugar(id = 1020, titulo = "Plaza Cívica Campus Central", latitud = 19.247594154419595, longitud = -103.69980902211155)
        )
        val lugaresGuardados = lugaresManager.cargarLugares()
        val lugaresCampusGuardados = lugaresGuardados.filter { it.id in 1001..1020 }
        val lugaresPersonalizados = lugaresGuardados.filter { it.id !in 1001..1020 }
        val lugaresCombinados = mutableListOf<Lugar>()
        lugaresCombinados.addAll(lugaresCampusBase)
        lugaresCombinados.addAll(lugaresPersonalizados)
        lugaresManager.guardarLugares(lugaresCombinados)
        if (!yaInicializado) {
            sharedPrefs.edit().putBoolean("inicializado", true).apply()
            Toast.makeText(this, "19 ubicaciones del Campus Central cargadas", Toast.LENGTH_LONG).show()
        }
    }

    // Métodos del ciclo de vida de la actividad
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
