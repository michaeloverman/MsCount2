package tech.michaeloverman.mscount.dataentry;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tech.michaeloverman.mscount.R;
import tech.michaeloverman.mscount.pojos.PieceOfMusic;
import timber.log.Timber;

/**
 * Fragment which handles UI and logic surrounding optional metadata variables of program
 * creation/editing.
 *
 * TODO: double check that it actually loads correct values when editing an existing work
 *
 * Created by Michael on 5/7/2017.
 */

public class MetaDataOptionsFragment extends Fragment {

    private Context mContext;
    private PieceOfMusic.Builder mBuilder;
    private int mBaselineRhythm;

    @BindView(R.id.measure_offset_entry) EditText mMeasureOffsetEntry;
    @BindView(R.id.tempo_multiplier_entry) EditText mTempoMultiplierEntry;
    @BindView(R.id.display_rhythmic_value_recycler)
    RecyclerView mDisplayValueEntry;
    private NoteValueAdapter mDisplayValueAdapter;

    public static Fragment newInstance(Context context, PieceOfMusic.Builder builder, int baselineRhythm) {
        MetaDataOptionsFragment fragment = new MetaDataOptionsFragment();
        fragment.mContext = context;
        fragment.mBuilder = builder;
        fragment.mBaselineRhythm = baselineRhythm;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.meta_data_options_layout, container, false);
        ButterKnife.bind(this, view);

        RecyclerView.LayoutManager manager = new LinearLayoutManager(mContext,
                LinearLayoutManager.HORIZONTAL, false);
        mDisplayValueEntry.setLayoutManager(manager);
        mDisplayValueAdapter = new NoteValueAdapter(mContext,
                getResources().obtainTypedArray(R.array.note_values),
                getResources().getStringArray(R.array.note_value_content_descriptions));
        mDisplayValueEntry.setAdapter(mDisplayValueAdapter);
        mDisplayValueAdapter.setSelectedPosition(mBaselineRhythm);

        // Remove soft keyboard when focus on recycler
        mDisplayValueEntry.setOnFocusChangeListener( (View v, boolean hasFocus) -> {
            if(hasFocus) {
                InputMethodManager imm = (InputMethodManager) v.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });

        return view;
    }

    @OnClick(R.id.options_cancel_button)
    public void cancelOptionsWithoutSave() {
        if(getFragmentManager() != null) getFragmentManager().popBackStackImmediate();
    }

    @OnClick(R.id.save_options_button)
    public void save() {
        String temp = mMeasureOffsetEntry.getText().toString();
        
        if (!temp.equals("")) {
            int offset = Integer.parseInt(temp);
            mBuilder.firstMeasureNumber(offset);
        }

        temp = mTempoMultiplierEntry.getText().toString();
        
        if (!temp.equals("")) {
            float multiplier = Float.parseFloat(temp);
            mBuilder.tempoMultiplier(multiplier);
            Timber.d("mBuilder should have 0.5 multiplier...");
        }

        int display = mDisplayValueAdapter.getSelectedRhythm();
        mBuilder.displayNoteValue(display);

        if(getFragmentManager() != null) getFragmentManager().popBackStackImmediate();
    }


}
