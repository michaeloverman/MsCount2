package tech.michaeloverman.mscount.dataentry

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.note_value_image_view.view.*
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.dataentry.NoteValueAdapter.NoteViewHolder
import tech.michaeloverman.mscount.pojos.PieceOfMusic
import timber.log.Timber

/**
 * Adapter class handles the recycler view which provides options for baseline rhythmic values.
 *
 * Created by Michael on 5/24/2017.
 */
internal class NoteValueAdapter(private val mContext: Context, private val noteValueImages: TypedArray, private val noteValueDescriptions: Array<String>) : RecyclerView.Adapter<NoteViewHolder>() {
    private var selectedPosition = 0
    val selectedRhythm: Int
        get() = when (selectedPosition) {
            0 -> PieceOfMusic.SIXTEENTH
            1 -> PieceOfMusic.DOTTED_SIXTEENTH
            2 -> PieceOfMusic.EIGHTH
            3 -> PieceOfMusic.DOTTED_EIGHTH
            5 -> PieceOfMusic.DOTTED_QUARTER
            6 -> PieceOfMusic.HALF
            7 -> PieceOfMusic.DOTTED_HALF
            8 -> PieceOfMusic.WHOLE
            4 -> PieceOfMusic.QUARTER
            else -> PieceOfMusic.QUARTER
        }

    fun setSelectedPosition(rhythm: Int) {
        Timber.d("setting selected rhythmic value: %s", rhythm)
        selectedPosition = when (rhythm) {
            PieceOfMusic.SIXTEENTH -> 0
            PieceOfMusic.DOTTED_SIXTEENTH -> 1
            PieceOfMusic.EIGHTH -> 2
            PieceOfMusic.DOTTED_EIGHTH -> 3
            PieceOfMusic.DOTTED_QUARTER -> 5
            PieceOfMusic.HALF -> 6
            PieceOfMusic.DOTTED_HALF -> 7
            PieceOfMusic.WHOLE -> 8
            PieceOfMusic.QUARTER -> 4
            else -> 4
        }
        Timber.d("selectedPosition = %s", selectedPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val item = LayoutInflater.from(mContext)
                .inflate(R.layout.note_value_image_view, parent, false)
        return NoteViewHolder(item)
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.image.setImageDrawable(noteValueImages.getDrawable(position))
        Timber.d("onBindViewHolder, position: " + position + " selected: " + (position == selectedPosition))
        if (selectedPosition == position) {
            holder.itemView.background = ContextCompat
                    .getDrawable(mContext, R.drawable.roundcorner_accent)
        } else {
            holder.itemView.background = ContextCompat
                    .getDrawable(mContext, R.drawable.roundcorner_parchment)
        }
        holder.itemView.setOnClickListener { v: View? ->
            notifyItemChanged(selectedPosition)
            selectedPosition = position
            notifyItemChanged(selectedPosition)
        }
        holder.itemView.contentDescription = noteValueDescriptions[position]
    }

    override fun getItemCount(): Int {
        return noteValueImages.length()
    }

    internal inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.note_value_image

        init {
            Timber.d("NoteViewHolder created, image: ")
        }
    }
}