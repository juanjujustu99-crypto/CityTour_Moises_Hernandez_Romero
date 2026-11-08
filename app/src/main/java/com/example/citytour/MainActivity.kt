package com.example.citytour

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // -----------------------------------------
    // COMPONENTES DE LA GALERIA
    // -----------------------------------------

    private lateinit var ivMonumento: ImageView
    private lateinit var tvFotoDescripcion: TextView
    private lateinit var tvNumeroImagen: TextView

    private lateinit var btnAnterior: Button
    private lateinit var btnSiguiente: Button

    // -----------------------------------------
    // COMPONENTES DEL REPRODUCTOR
    // -----------------------------------------

    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button
    private lateinit var btnStop: Button

    private lateinit var seekBarAudio: SeekBar
    private lateinit var tvAudioEstado: TextView
    private lateinit var seekBarVolumen: SeekBar

    private var volumenActual = 70
    private lateinit var tvAudioTiempo: TextView

    // -----------------------------------------
    // MODELO DE LAS FOTOGRAFIAS
    // -----------------------------------------

    private data class FotoMonumento(
        val imagen: Int,
        val descripcion: Int
    )

    private val fotografias = listOf(
        FotoMonumento(
            R.drawable.arco_frontal,
            R.string.gallery_caption_front
        ),
        FotoMonumento(
            R.drawable.arco_lateral,
            R.string.gallery_caption_lateral
        ),
        FotoMonumento(
            R.drawable.arco_detalle,
            R.string.gallery_caption_detail
        )
    )

    private var indiceActual = 0

    // -----------------------------------------
    // ESTADOS DEL REPRODUCTOR
    // -----------------------------------------

    private enum class EstadoAudio {
        LISTO,
        REPRODUCIENDO,
        PAUSADO,
        DETENIDO,
        FINALIZADO,
        ERROR
    }

    private var estadoAudio = EstadoAudio.LISTO

    private var mediaPlayer: MediaPlayer? = null

    // Administrador de tareas para actualizar
    // periodicamente la barra de progreso.
    private val handler = Handler(Looper.getMainLooper())

    // -----------------------------------------
    // ACTUALIZACION DEL PROGRESO DEL AUDIO
    // -----------------------------------------

    private val actualizarProgreso = object : Runnable {

        override fun run() {

            val reproductor = mediaPlayer ?: return

            if (estadoAudio != EstadoAudio.REPRODUCIENDO) {
                return
            }

            try {

                val posicion = reproductor.currentPosition
                val duracion = reproductor.duration

                seekBarAudio.progress = posicion

                actualizarTiempo(posicion, duracion)

                handler.postDelayed(this, 500)

            } catch (e: IllegalStateException) {

                cambiarEstado(EstadoAudio.ERROR)
            }
        }
    }

    // -----------------------------------------
    // CICLO DE VIDA DE LA ACTIVIDAD
    // -----------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        configurarBarrasSistema()

        inicializarGaleria()

        inicializarControlesAudio()

        configurarEventosAudio()

        // Preparar el reproductor al abrir la aplicacion.
        inicializarReproductor(EstadoAudio.LISTO)
    }

    // -----------------------------------------
    // CONFIGURACION DE LAS BARRAS DEL SISTEMA
    // -----------------------------------------

    private fun configurarBarrasSistema() {

        val paddingBase =
            (20 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { vista, insets ->

            val barrasSistema = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            vista.setPadding(
                barrasSistema.left + paddingBase,
                barrasSistema.top + paddingBase,
                barrasSistema.right + paddingBase,
                barrasSistema.bottom + paddingBase
            )

            insets
        }
    }

    // -----------------------------------------
    // GALERIA DE FOTOGRAFIAS
    // -----------------------------------------

    private fun inicializarGaleria() {

        ivMonumento = findViewById(R.id.ivMonumento)
        tvFotoDescripcion = findViewById(R.id.tvFotoDescripcion)
        tvNumeroImagen = findViewById(R.id.tvNumeroImagen)

        btnAnterior = findViewById(R.id.btnAnterior)
        btnSiguiente = findViewById(R.id.btnSiguiente)

        mostrarFoto(indiceActual)

        btnAnterior.setOnClickListener {
            mostrarFoto(indiceActual - 1)
        }

        btnSiguiente.setOnClickListener {
            mostrarFoto(indiceActual + 1)
        }
    }

    private fun mostrarFoto(nuevoIndice: Int) {

        // Permitir navegar circularmente.
        indiceActual = (
                (nuevoIndice % fotografias.size) +
                        fotografias.size
                ) % fotografias.size

        val fotografia = fotografias[indiceActual]

        ivMonumento.setImageResource(fotografia.imagen)

        val descripcion = getString(
            fotografia.descripcion
        )

        tvFotoDescripcion.text = descripcion

        ivMonumento.contentDescription = getString(
            R.string.gallery_image_content_description,
            descripcion
        )

        tvNumeroImagen.text = getString(
            R.string.gallery_counter,
            indiceActual + 1,
            fotografias.size
        )
    }

    // -----------------------------------------
    // INICIALIZAR CONTROLES DE AUDIO
    // -----------------------------------------

    private fun inicializarControlesAudio() {

        btnPlay = findViewById(R.id.btnPlay)
        btnPause = findViewById(R.id.btnPause)
        btnStop = findViewById(R.id.btnStop)

        seekBarAudio = findViewById(R.id.seekBarAudio)

        tvAudioEstado = findViewById(R.id.tvAudioEstado)
        tvAudioTiempo = findViewById(R.id.tvAudioTiempo)
        seekBarVolumen = findViewById(R.id.seekBarVolumen)

        configurarControlVolumen()



        configurarBarraProgreso()
    }
// -----------------------------------------
// CONTROL DE VOLUMEN
// -----------------------------------------

    private fun configurarControlVolumen() {

        seekBarVolumen.max = 100
        seekBarVolumen.progress = volumenActual

        seekBarVolumen.setOnSeekBarChangeListener(

            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    if (fromUser) {

                        volumenActual = progress
                        aplicarVolumen()
                    }
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar
                ) {
                    // El usuario comienza a modificar
                    // el volumen.
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar
                ) {
                    // Se conserva el volumen seleccionado.
                }
            }
        )
    }

    // Aplicar el volumen al reproductor
    private fun aplicarVolumen() {

        val nivel = volumenActual.coerceIn(0, 100) / 100f

        mediaPlayer?.setVolume(nivel, nivel)
    }

    // -----------------------------------------
    // EVENTOS DE LOS BOTONES
    // -----------------------------------------

    private fun configurarEventosAudio() {

        btnPlay.setOnClickListener {
            reproducirAudio()
        }

        btnPause.setOnClickListener {
            pausarAudio()
        }

        btnStop.setOnClickListener {
            detenerAudio()
        }
    }

    // -----------------------------------------
    // CONFIGURAR BARRA DE PROGRESO
    // -----------------------------------------

    private fun configurarBarraProgreso() {

        seekBarAudio.setOnSeekBarChangeListener(

            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    if (fromUser) {

                        val duracion =
                            mediaPlayer?.duration ?: 0

                        actualizarTiempo(
                            progress,
                            duracion
                        )
                    }
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar
                ) {

                    handler.removeCallbacks(
                        actualizarProgreso
                    )
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar
                ) {

                    try {

                        mediaPlayer?.seekTo(
                            seekBar.progress
                        )

                        if (
                            estadoAudio ==
                            EstadoAudio.REPRODUCIENDO
                        ) {

                            handler.post(
                                actualizarProgreso
                            )
                        }

                    } catch (e: IllegalStateException) {

                        cambiarEstado(EstadoAudio.ERROR)
                    }
                }
            }
        )
    }

    // -----------------------------------------
    // CREAR Y PREPARAR MEDIA PLAYER
    // -----------------------------------------

    private fun inicializarReproductor(
        estadoInicial: EstadoAudio
    ) {

        try {

            val reproductor = MediaPlayer.create(
                this,
                R.raw.narracion_arco
            ) ?: throw IllegalStateException(
                getString(R.string.no_fue_posible_cargar_el_archivo_de_audio)
            )

            mediaPlayer = reproductor
            aplicarVolumen()

            // Detectar cuando termina la narracion.
            reproductor.setOnCompletionListener { mp ->

                handler.removeCallbacks(
                    actualizarProgreso
                )

                seekBarAudio.progress = seekBarAudio.max

                actualizarTiempo(
                    mp.duration,
                    mp.duration
                )

                cambiarEstado(
                    EstadoAudio.FINALIZADO
                )
            }

            // Configurar la duracion total.
            seekBarAudio.max = reproductor.duration

            seekBarAudio.progress = 0
            seekBarAudio.isEnabled = true

            actualizarTiempo(
                0,
                reproductor.duration
            )

            cambiarEstado(estadoInicial)

        } catch (e: Exception) {

            mediaPlayer?.release()
            mediaPlayer = null

            seekBarAudio.isEnabled = false

            cambiarEstado(EstadoAudio.ERROR)
        }
    }

    // -----------------------------------------
    // REPRODUCIR Y REANUDAR AUDIO
    // -----------------------------------------

    private fun reproducirAudio() {

        val reproductor = mediaPlayer

        if (reproductor == null) {

            inicializarReproductor(
                EstadoAudio.LISTO
            )

            return
        }

        try {

            // Si termino la narracion, comenzar desde cero.
            if (
                estadoAudio ==
                EstadoAudio.FINALIZADO
            ) {

                reproductor.seekTo(0)
                seekBarAudio.progress = 0
            }

            reproductor.start()

            cambiarEstado(
                EstadoAudio.REPRODUCIENDO
            )

            handler.removeCallbacks(
                actualizarProgreso
            )

            handler.post(
                actualizarProgreso
            )

        } catch (e: IllegalStateException) {

            cambiarEstado(EstadoAudio.ERROR)
        }
    }

    // -----------------------------------------
    // PAUSAR AUDIO
    // -----------------------------------------

    private fun pausarAudio() {

        val reproductor = mediaPlayer ?: return

        try {

            if (reproductor.isPlaying) {

                reproductor.pause()

                handler.removeCallbacks(
                    actualizarProgreso
                )

                actualizarTiempo(
                    reproductor.currentPosition,
                    reproductor.duration
                )

                cambiarEstado(
                    EstadoAudio.PAUSADO
                )
            }

        } catch (e: IllegalStateException) {

            cambiarEstado(EstadoAudio.ERROR)
        }
    }

    // -----------------------------------------
    // DETENER Y REINICIAR AUDIO
    // -----------------------------------------

    private fun detenerAudio() {

        handler.removeCallbacks(
            actualizarProgreso
        )

        // Liberar el reproductor actual.
        liberarReproductor()

        // Crear uno nuevo para dejarlo preparado
        // y permitir una nueva reproduccion.
        inicializarReproductor(
            EstadoAudio.DETENIDO
        )
    }

    // -----------------------------------------
    // ACTUALIZAR ESTADO E INTERFAZ
    // -----------------------------------------

    private fun cambiarEstado(
        nuevoEstado: EstadoAudio
    ) {

        estadoAudio = nuevoEstado

        tvAudioEstado.text = when (nuevoEstado) {

            EstadoAudio.LISTO ->
                getString(R.string.audio_ready)

            EstadoAudio.REPRODUCIENDO ->
                getString(R.string.audio_playing)

            EstadoAudio.PAUSADO ->
                getString(R.string.audio_paused)

            EstadoAudio.DETENIDO ->
                getString(R.string.audio_stopped)

            EstadoAudio.FINALIZADO ->
                getString(R.string.audio_completed)

            EstadoAudio.ERROR ->
                getString(R.string.audio_load_error)
        }

        // Mostrar Reanudar cuando el audio esta pausado.
        btnPlay.text = if (
            nuevoEstado == EstadoAudio.PAUSADO
        ) {

            getString(R.string.audio_resume)

        } else {

            getString(R.string.button_play)
        }

        btnPlay.isEnabled =
            nuevoEstado != EstadoAudio.REPRODUCIENDO &&
                    nuevoEstado != EstadoAudio.ERROR

        btnPause.isEnabled =
            nuevoEstado == EstadoAudio.REPRODUCIENDO

        btnStop.isEnabled =
            nuevoEstado == EstadoAudio.REPRODUCIENDO ||
                    nuevoEstado == EstadoAudio.PAUSADO
    }

    // -----------------------------------------
    // FORMATO DEL TIEMPO DE REPRODUCCION
    // -----------------------------------------

    private fun actualizarTiempo(
        posicion: Int,
        duracion: Int
    ) {

        tvAudioTiempo.text = getString(
            R.string.audio_time_format,
            formatearTiempo(posicion),
            formatearTiempo(duracion)
        )
    }

    private fun formatearTiempo(
        milisegundos: Int
    ): String {

        val segundosTotales =
            milisegundos.coerceAtLeast(0) / 1000

        val minutos = segundosTotales / 60
        val segundos = segundosTotales % 60

        return String.format(
            Locale.getDefault(),
            "%02d:%02d",
            minutos,
            segundos
        )
    }

    // -----------------------------------------
    // LIBERAR RECURSOS DEL REPRODUCTOR
    // -----------------------------------------

    private fun liberarReproductor() {

        handler.removeCallbacks(
            actualizarProgreso
        )

        mediaPlayer?.let { reproductor ->

            try {

                reproductor.setOnCompletionListener(null)

                reproductor.release()

            } catch (e: IllegalStateException) {

                // El recurso ya no esta disponible.
            }
        }

        mediaPlayer = null
    }

    // -----------------------------------------
    // GESTION DEL CICLO DE VIDA
    // -----------------------------------------

    override fun onStop() {
        super.onStop()

        // Pausar el audio cuando la aplicacion
        // pasa a segundo plano.
        if (
            ::btnPause.isInitialized &&
            mediaPlayer?.isPlaying == true
        ) {

            pausarAudio()
        }
    }

    override fun onDestroy() {

        liberarReproductor()

        super.onDestroy()
    }
}