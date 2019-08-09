package tech.michaeloverman.mscount.dataentry;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import tech.michaeloverman.mscount.R;
import tech.michaeloverman.mscount.pojos.PieceOfMusic;
import timber.log.Timber;

/**
 * Adapter class handles the recycler view which provides options for baseline rhythmic values.
 *
 * Created by Michael on 5/24/2017.
 */
class NoteValueAdapter extends RecyclerView.Adapter<NoteValueAdapter.NoteViewHolder> {
    private final Context mContext;
    private final TypedArray noteValueImages;
    private final String[] noteValueDescriptions;
    private int selectedPosition;

    public NoteValueAdapter(Context context, TypedArray images, String[] imageDescriptions) {
        mContext = context;
        noteValueImages = images;
        noteValueDescriptions = imageDescriptions;
    }

    public int getSelectedRhythm() {
        switch(selectedPosition) {
            case 0: return PieceOfMusic.SIXTEENTH;
            case 1: return PieceOfMusic.DOTTED_SIXTEENTH;
            case 2: return PieceOfMusic.EIGHTH;
            case 3: return PieceOfMusic.DOTTED_EIGHTH;
            case 5: return PieceOfMusic.DOTTED_QUARTER;
            case 6: return PieceOfMusic.HALF;
            case 7: return PieceOfMusic.DOTTED_HALF;
            case 8: return PieceOfMusic.WHOLE;
            case 4:
            default: return PieceOfMusic.QUARTER;
        }
    }

    public void setSelectedPosition(int rhythm) {
        Timber.d("setting selected rhythmic value: %s", rhythm);
        switch(rhythm) {
            case PieceOfMusic.SIXTEENTH:
                selectedPosition = 0;
                break;
            case PieceOfMusic.DOTTED_SIXTEENTH:
                selectedPosition = 1;
                break;
            case PieceOfMusic.EIGHTH:
                selectedPosition = 2;
                break;
            case PieceOfMusic.DOTTED_EIGHTH:
                selectedPosition = 3;
                break;
            case PieceOfMusic.DOTTED_QUARTER:
                selectedPosition = 5;
                break;
            case PieceOfMusic.HALF:
                selectedPosition = 6;
                break;
            case PieceOfMusic.DOTTED_HALF:
                selectedPosition = 7;
                break;
            case PieceOfMusic.WHOLE:
                selectedPosition = 8;
                break;
            case PieceOfMusic.QUARTER:
            default: selectedPosition = 4;
        }
        Timber.d("selectedPosition = %s", selectedPosition);
    }

    @NotNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(mContext)
                .inflate(R.layout.note_value_image_view, parent, false);
        return new NoteViewHolder(item);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(NoteViewHolder holder, final int position) {
        holder.image.setImageDrawable(noteValueImages.getDrawable(position));
        Timber.d("onBindViewHolder, position: " + position + " selected: " + (position == selectedPosition));

        if(selectedPosition == position) {
            holder.itemView.setBackground(ContextCompat
                    .getDrawable(mContext, R.drawable.roundcorner_accent));
        } else {
            holder.itemView.setBackground(ContextCompat
                    .getDrawable(mContext, R.drawable.roundcorner_parchment));
        }

        holder.itemView.setOnClickListener( (View v) -> {
            notifyItemChanged(selectedPosition);
            selectedPosition = position;
            notifyItemChanged(selectedPosition);
        });

        holder.itemView.setContentDescription(noteValueDescriptions[position]);
    }

    @Override
    public int getItemCount() {
        return noteValueImages.length();
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;

        public NoteViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.note_value_image);
            Timber.d("NoteViewHolder created, image: ");
        }
    }
}
