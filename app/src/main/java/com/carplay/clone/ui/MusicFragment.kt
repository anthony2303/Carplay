package com.carplay.clone.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.carplay.clone.R

class MusicFragment : Fragment() {
    
    private var isPlaying = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_music, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupMusicControls(view)
    }
    
    private fun setupMusicControls(view: View) {
        val playButton = view.findViewById<ImageButton>(R.id.playButton)
        val nextButton = view.findViewById<ImageButton>(R.id.nextButton)
        val prevButton = view.findViewById<ImageButton>(R.id.prevButton)
        val seekBar = view.findViewById<SeekBar>(R.id.seekBar)
        val currentTimeText = view.findViewById<TextView>(R.id.currentTime)
        val totalTimeText = view.findViewById<TextView>(R.id.totalTime)
        val songTitle = view.findViewById<TextView>(R.id.songTitle)
        val artistName = view.findViewById<TextView>(R.id.artistName)
        
        playButton.setOnClickListener {
            isPlaying = !isPlaying
            playButton.setImageResource(
                if (isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }
        
        nextButton.setOnClickListener {
            // Siguiente canción
            songTitle.text = "Nueva Canción"
            artistName.text = "Artista"
        }
        
        prevButton.setOnClickListener {
            // Canción anterior
            songTitle.text = "Canción Anterior"
            artistName.text = "Artista"
        }
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val minutes = progress / 60
                    val seconds = progress % 60
                    currentTimeText.text = String.format("%02d:%02d", minutes, seconds)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Valores iniciales
        songTitle.text = "Bohemian Rhapsody"
        artistName.text = "Queen"
        totalTimeText.text = "05:55"
        currentTimeText.text = "00:00"
        seekBar.max = 355
    }
}
