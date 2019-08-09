/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.dataentry;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.perf.metrics.AddTrace;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tech.michaeloverman.mscount.R;
import tech.michaeloverman.mscount.database.LoadNewProgramActivity;
import tech.michaeloverman.mscount.database.ProgramDatabaseSchema;
import tech.michaeloverman.mscount.pojos.DataEntry;
import tech.michaeloverman.mscount.pojos.PieceOfMusic;
import tech.michaeloverman.mscount.programmed.ProgrammedMetronomeActivity;
import tech.michaeloverman.mscount.utils.Metronome;
import tech.michaeloverman.mscount.utils.Utilities;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;

/**
 * This fragment manages the UI and handles the logic for all metadata surrounding a program,
 * such as title, composer, foundational data, etc. Opens MetaDataOptionsFragment for optional
 * items of metadata.
 */
public class MetaDataEntryFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, DataEntryFragment.DataMultipliedListener {

    private static final int REQUEST_NEW_PROGRAM = 1984;

    @BindView(R.id.composer_last_name_text_entry) EditText mComposerLastEntry;
    @BindView(R.id.composer_first_name_text_entry) EditText mComposerFirstEntry;
    @BindView(R.id.title_text_entry) EditText mTitleEntry;
    @BindView(R.id.baseline_subdivision_entry) EditText mBaselineSubdivisionEntry;
    @BindView(R.id.countoff_subdivision_entry) EditText mCountoffSubdivisionEntry;
    @BindView(R.id.default_tempo_label) TextView mDefaultTempoLabel;
    @BindView(R.id.default_tempo_entry) EditText mDefaultTempoEntry;
    @BindView(R.id.options_button) Button mMetaDataOptionsButton;
    @BindView(R.id.enter_beats_button) Button mEnterBeatsButton;
    @BindView(R.id.baseline_rhythmic_value_recycler) RecyclerView mBaselineRhythmicValueEntry;

    @BindView(R.id.help_overlay) FrameLayout mInstructionsLayout;

    private NoteValueAdapter mBaselineRhythmicValueAdapter;
    private int mTemporaryBaselineRhythm = 4;

    private PieceOfMusic mPieceOfMusic;
    private PieceOfMusic.Builder mBuilder;
    private String mCurrentPieceKey;
    private String mFirebaseId;
    private List<DataEntry> mDataEntries;
    private float mDataMultiplier = 1.0f;
    private List<Integer> mDownBeats;

    private static ProgrammedMetronomeActivity mActivity;
    private static final int ID_PIECE_LOADER = 435;
    private static Cursor mCursor;

    public static Fragment newInstance(ProgrammedMetronomeActivity a, Cursor c) {
        Timber.d("newInstance()");
        mActivity = a;
        mCursor = c;
        return new MetaDataEntryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate()");
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mBuilder = new PieceOfMusic.Builder();
        mPieceOfMusic = new PieceOfMusic();

    }

//    This method was used to make changes to the structure of the Firebase Database. It is not
//    used or needed anymore, but is being left here for the sake of posterity.
//    private void rewriteAllProgramsWithCreatorId() {
//        final DatabaseReference dRef = FirebaseDatabase.getInstance().getReference();
//        dRef.child("pieces")
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        Iterable<DataSnapshot> programs = dataSnapshot.getChildren();
//                        for(DataSnapshot snap : programs) {
//                            PieceOfMusic p = snap.getValue(PieceOfMusic.class);
//                            p.setCreatorId("dvM60nH1mHYBYjuBAxminpA4Zve2");
//                            Map<String, Object> update = new HashMap<>();
//                            update.put("/pieces/" + snap.getKey(), p);
//                            dRef.updateChildren(update);
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//
//                    }
//                });
//    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.metadata_entry_menu, menu);
//        menu.removeItem(R.id.create_new_program_option);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView()");
        View view = inflater.inflate(R.layout.meta_data_input_layout, container, false);
        ButterKnife.bind(this, view);

        // When a countoff value is entered, make sure it is an even divisor of the baseline subdivisions
        mCountoffSubdivisionEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //noinspection EmptyCatchBlock
//                try {
                String temp = mBaselineSubdivisionEntry.getText().toString();
                int primary = temp.equals("") ? 0 : Integer.parseInt(temp);
                int countoff = s.toString().equals("") ? 1 : Integer.parseInt(s.toString());
                if(primary % countoff != 0) {
                    Toast.makeText(getContext(),
                            R.string.countoff_must_fit_subdivisions,
                            Toast.LENGTH_SHORT).show();
                }
//                } catch (NumberFormatException n) {
//                    // not used: only integers can be entered
//                }
            }
        });

        // When default tempo is entered, make sure it is in the metronome's range
        mDefaultTempoEntry.setOnFocusChangeListener( (View v, boolean hasFocus) -> {
            if(!hasFocus) {
                try {
                    int tempo = Integer.parseInt(mDefaultTempoEntry.getText().toString());
                    if(tempo < Metronome.MIN_TEMPO || tempo > Metronome.MAX_TEMPO) {
                        Toast.makeText(getContext(), getString(R.string.tempo_between_min_max,
                                Metronome.MIN_TEMPO, Metronome.MAX_TEMPO), Toast.LENGTH_SHORT)
                                .show();
                        mDefaultTempoEntry.setText("");
                    }
                } catch (NumberFormatException n) {
                    Toast.makeText(getContext(), R.string.tempo_must_be_integer, Toast.LENGTH_SHORT).show();
                }
            }
        });

        RecyclerView.LayoutManager manager = new LinearLayoutManager(mActivity,
                LinearLayoutManager.HORIZONTAL, false);
        mBaselineRhythmicValueEntry.setLayoutManager(manager);
        mBaselineRhythmicValueAdapter = new NoteValueAdapter(mActivity,
                getResources().obtainTypedArray(R.array.note_values),
                getResources().getStringArray(R.array.note_value_content_descriptions));
        mBaselineRhythmicValueEntry.setAdapter(mBaselineRhythmicValueAdapter);
        mBaselineRhythmicValueAdapter.setSelectedPosition(mTemporaryBaselineRhythm);

        // Remove soft keyboard when display on recycler
        mBaselineRhythmicValueEntry.setOnFocusChangeListener( (View v, boolean hasFocus) -> {
            if(hasFocus) {
                InputMethodManager imm = (InputMethodManager) v.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mDataMultiplier != 1.0f) {
            int currentBaseline = Integer.valueOf(mBaselineSubdivisionEntry.getText().toString());
            currentBaseline *= mDataMultiplier;
            mBaselineSubdivisionEntry.setText(String.valueOf(currentBaseline));
            mDataMultiplier = 1.0f;
        }
    }

    @OnClick(R.id.enter_beats_button)
    public void enterBeatsClicked() {
        Timber.d("enterBeatsClicked()");

        String title = mTitleEntry.getText().toString();
        if(title.equals("")) {
            toastError();
            return;
        }

        mBuilder.title(title);

        mTemporaryBaselineRhythm = mBaselineRhythmicValueAdapter.getSelectedRhythm();
//        Timber.d("mTemporaryBaselineRhythm" + mTemporaryBaselineRhythm);
        gotoDataEntryFragment(title);
    }

    private void gotoDataEntryFragment(String title) {
        Fragment fragment;
        if(mDataEntries == null) {
            fragment = DataEntryFragment.newInstance(title, mBuilder, this);
        } else {
            fragment = DataEntryFragment.newInstance(title, mBuilder, mDataEntries, this);
        }
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @OnClick(R.id.options_button)
    public void optionsButtonClicked() {

        mTemporaryBaselineRhythm = mBaselineRhythmicValueAdapter.getSelectedRhythm();

        Fragment fragment = MetaDataOptionsFragment.newInstance(mActivity,
                mBuilder, mTemporaryBaselineRhythm);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_existing_program_option:
                loadProgram();
                return true;
            case R.id.help_menu_item:
                makeInstructionsVisible();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void makeInstructionsVisible() {
        mInstructionsLayout.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.help_cancel_button)
    public void instructionsCancelled() {
        mInstructionsLayout.setVisibility(View.INVISIBLE);
    }

    private void loadProgram() {
        Intent intent = new Intent(mActivity, LoadNewProgramActivity.class);
        startActivityForResult(intent, REQUEST_NEW_PROGRAM);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK) {
            Toast.makeText(mActivity, R.string.problem_loading, Toast.LENGTH_SHORT).show();
            return;
        }
        if (requestCode == REQUEST_NEW_PROGRAM) {
            getPieceFromKey(data.getStringExtra(LoadNewProgramActivity.EXTRA_NEW_PROGRAM));
        }
    }

    private void getPieceFromKey(String key) {

        mCurrentPieceKey = key;

        if(mCurrentPieceKey.charAt(0) == '-') {
            getPieceFromFirebase();
        } else {
            if(mCursor == null) {
                Timber.d("mCursor is null, initing loader...");
                LoaderManager.getInstance(mActivity).initLoader(ID_PIECE_LOADER, null, this);
            } else {
                Timber.d("mCursor exists, going straight to data");
                getPieceFromSql();
            }
        }

    }

    private void getPieceFromFirebase() {
        Timber.d("getPieceFromFirebase()");
        FirebaseDatabase.getInstance().getReference().child("pieces").child(mCurrentPieceKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                        mPieceOfMusic = dataSnapshot.getValue(PieceOfMusic.class);
                        updateVariables();
                    }

                    @Override
                    public void onCancelled(@NotNull DatabaseError databaseError) {
                        Toast.makeText(getContext(), getString(R.string.database_error_try_again),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getPieceFromSql() {
        Timber.d("getPieceFromSql()");
        int localDbId = Integer.parseInt(mCurrentPieceKey);
        mCursor.moveToFirst();
        while(mCursor.getInt(ProgramDatabaseSchema.MetProgram.POSITION_ID) != localDbId) {
            Timber.d("_id: " + mCursor.getInt(ProgramDatabaseSchema.MetProgram.POSITION_ID)
                    + " != " + localDbId);
            if(!mCursor.moveToNext()) {
                programNotFoundError(localDbId);
                return;
            }
        }
        PieceOfMusic.Builder builder = new PieceOfMusic.Builder()
                .author(mCursor.getString(ProgramDatabaseSchema.MetProgram.POSITION_COMPOSER))
                .title(mCursor.getString(ProgramDatabaseSchema.MetProgram.POSITION_TITLE))
                .subdivision(mCursor.getInt(ProgramDatabaseSchema.MetProgram.POSITION_PRIMANY_SUBDIVISIONS))
                .countOffSubdivision(mCursor.getInt(ProgramDatabaseSchema.MetProgram.POSITION_COUNOFF_SUBDIVISIONS))
                .defaultTempo(mCursor.getInt(ProgramDatabaseSchema.MetProgram.POSITION_DEFAULT_TEMPO))
                .baselineNoteValue(mCursor.getInt(ProgramDatabaseSchema.MetProgram.POSITION_DEFAULT_RHYTHM))
                .tempoMultiplier(mCursor.getDouble(ProgramDatabaseSchema.MetProgram.POSITION_TEMPO_MULTIPLIER))
                .firstMeasureNumber(mCursor.getInt(ProgramDatabaseSchema.MetProgram.POSITION_MEASURE_COUNTE_OFFSET))
                .dataEntries(mCursor.getString(ProgramDatabaseSchema.MetProgram.POSITION_DATA_ARRAY))
                .firebaseId(mCursor.getString(ProgramDatabaseSchema.MetProgram.POSITION_FIREBASE_ID));
//        mCursor.close();
        mPieceOfMusic = builder.build();
        updateVariables();
    }

    private void programNotFoundError(int id) {
        Toast.makeText(mActivity, getString(R.string.id_not_found, id),
                Toast.LENGTH_SHORT).show();
    }

    private void updateVariables() {
        if(mPieceOfMusic == null || mPieceOfMusic.getRawData() == null) {
            mPieceOfMusic.constructRawData();
        }
        mFirebaseId = mPieceOfMusic.getFirebaseId();
        updateGUI();
    }

    private void updateGUI() {
        Timber.d("updating the GUI");
        String[] names = mPieceOfMusic.getAuthor().split(", ");
        if(names.length >= 2) {
            mComposerLastEntry.setText(names[0]);
            mComposerFirstEntry.setText(names[1]);
        } else if(names.length == 1) {
            mComposerLastEntry.setText(mPieceOfMusic.getAuthor());
            mComposerFirstEntry.setText("");
        } else {
            mComposerLastEntry.setText("");
            mComposerFirstEntry.setText("");
        }
        mTitleEntry.setText(mPieceOfMusic.getTitle());
        mBaselineSubdivisionEntry.setText(String.valueOf(mPieceOfMusic.getSubdivision()));
        mCountoffSubdivisionEntry.setText(String.valueOf(mPieceOfMusic.getCountOffSubdivision()));
        mDefaultTempoEntry.setText(String.valueOf(mPieceOfMusic.getDefaultTempo()));
        mBaselineRhythmicValueAdapter.setSelectedPosition(mPieceOfMusic.getBaselineNoteValue());
        mBaselineRhythmicValueAdapter.notifyDataSetChanged();
        mDataEntries = mPieceOfMusic.getRawData();
    }

    @Override
    public void dataValuesMultipliedBy(float multiplier) {


        mDataMultiplier *= multiplier;
    }

    @OnClick(R.id.save_program_button)
    public void saveProgram() {
        Timber.d("checking data entries");
        if (!validateDataEntries()) return;

        // get all the metadata fields
        Timber.d("checking metadata entries");
        if (!validateMetaDataEntries()) return;

        Timber.d("building rest of mBuilder");
        mBuilder.firebaseId(mFirebaseId);

        mBuilder.creatorId(getFirebaseAuthId());

        mPieceOfMusic = mBuilder.build();

        if(mActivity.useFirebase) {
            checkFirebaseForExistingData(); // beginning of method chain to save to cloud
        } else {
            saveToSqlDatabase();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private String getFirebaseAuthId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if(auth != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    private boolean validateMetaDataEntries() {
        String composerLast = mComposerLastEntry.getText().toString();
        String composerFirst = mComposerFirstEntry.getText().toString();
        String title = mTitleEntry.getText().toString();
        String subd = mBaselineSubdivisionEntry.getText().toString();
        String countoff = mCountoffSubdivisionEntry.getText().toString();
        String defaultTempo = mDefaultTempoEntry.getText().toString();
        int rhythm = mBaselineRhythmicValueAdapter.getSelectedRhythm();

        // Check for null entries...
        if(composerLast.equals("")) {
            Toast.makeText(getContext(), R.string.error_no_composer_message,
                    Toast.LENGTH_SHORT).show();
            mComposerLastEntry.requestFocus();
            return false;
        }
    
        if(composerFirst.equals("")) {
            Toast.makeText(getContext(), R.string.error_no_composer_message,
                    Toast.LENGTH_SHORT).show();
            mComposerFirstEntry.requestFocus();
            return false;
        }

        if(title.equals("")) {
            Toast.makeText(getContext(), R.string.error_no_title_message,
                    Toast.LENGTH_SHORT).show();
            mTitleEntry.requestFocus();
            return false;
        }

        if(subd.equals("")) {
            Toast.makeText(getContext(), R.string.error_no_subdivision_message,
                    Toast.LENGTH_SHORT).show();
            mBaselineSubdivisionEntry.requestFocus();
            return false;
        }

        if(countoff.equals("")) {
            Toast.makeText(getContext(), R.string.error_no_countoff_message,
                    Toast.LENGTH_SHORT).show();
            mCountoffSubdivisionEntry.requestFocus();
            return false;
        }

        // Check for valid data input...
        int subdInt, countoffInt;
        try {
            subdInt = Integer.parseInt(subd);
            if(subdInt < 1 || subdInt > 24) {
                Toast.makeText(getContext(), R.string.subdivs_out_of_range, Toast.LENGTH_SHORT).show();
            } else //noinspection StatementWithEmptyBody
                if(subdInt > 12) {
                //TODO dialog box to confirm unusually large baseline subdivision
            }
        } catch (NumberFormatException nfe) {
            Toast.makeText(getContext(), R.string.enter_only_number_subdivs, Toast.LENGTH_SHORT).show();
            mBaselineSubdivisionEntry.requestFocus();
            return false;
        }

        try {
            countoffInt = Integer.parseInt(countoff);
            if(countoffInt > subdInt || countoffInt == 0) {
                Toast.makeText(getContext(), R.string.countoff_must_be_even_divisor,
                        Toast.LENGTH_SHORT).show();
                mCountoffSubdivisionEntry.requestFocus();
                return false;
            }
        } catch (NumberFormatException nfe) {
            Toast.makeText(getContext(), R.string.please_enter_only_numbers_countoff,
                    Toast.LENGTH_SHORT).show();
            mBaselineSubdivisionEntry.requestFocus();
            return false;
        }

        String composer = composerLast.trim() + ", " + composerFirst.trim();
        mBuilder.author(composer)
                .title(title)
                .subdivision(subdInt)
                .countOffSubdivision(countoffInt);

        int tempoInt;
        try {
            if(!defaultTempo.equals("")) {
                tempoInt = Integer.parseInt(defaultTempo);
                if (tempoInt < 15 || tempoInt > 250) {
                    Toast.makeText(getContext(), R.string.tempo_between_min_max, Toast.LENGTH_SHORT).show();
                    return false;
                }
                mBuilder.defaultTempo(tempoInt);
            }
        } catch (NumberFormatException nfe) {
            Timber.d("You should not be here: should not be able to enter anything but numbers, and any numbers entered have already been checked for range.");
        }

        mBuilder.baselineNoteValue(rhythm);

        return true;
    }

    private boolean validateDataEntries() {
        if(mBuilder.hasData()) {
            return true;
        } else {
            Toast.makeText(getContext(), R.string.enter_data_before_saving,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
//        mBuilder.dataEntries(mDataEntries);
    }

    @AddTrace(name = "saveToSqlDatabase")
    private void saveToSqlDatabase() {
        Timber.d("this where it should be saving to sql");
        ContentValues contentValues = Utilities.getContentValuesFromPiece(mPieceOfMusic);
        ContentResolver resolver = getContext().getContentResolver();
        Uri returnUri = resolver.insert(ProgramDatabaseSchema.MetProgram.CONTENT_URI, contentValues);
        if(returnUri != null) {
            getFragmentManager().popBackStackImmediate();
        } else {
            databaseSaveErrorStayHere();
        }
    }

    private void checkFirebaseForExistingData() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference databaseReference = database.getReference();

        // Look for piece first, and if exists, get that key to update; otherwise push() to create
        // new key for new piece.
        final String composer = mPieceOfMusic.getAuthor();
        final String title = mPieceOfMusic.getTitle();
        databaseReference.child("composers").child(composer).child(title)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String key = dataSnapshot.getValue().toString();
                            checkIfAuthorizedCreator(key);
                        } else {
                            saveToFirebase(mPieceOfMusic);
                        }
                    }

                    @Override
                    public void onCancelled(@NotNull DatabaseError databaseError) {
                        Toast.makeText(getContext(), R.string.database_error_save_cancelled,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIfAuthorizedCreator(String key) {
        FirebaseDatabase.getInstance().getReference().child("pieces").child(key)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                        PieceOfMusic pieceFromFirebase = dataSnapshot.getValue(PieceOfMusic.class);

                        if(pieceFromFirebase.getCreatorId().equals(mPieceOfMusic.getCreatorId())) {
                            overwriteFirebaseDataAlertDialog(mPieceOfMusic.getTitle(),
                                    mPieceOfMusic.getAuthor());
                        } else {
                            Toast.makeText(mActivity, R.string.not_authorized_save_local,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NotNull DatabaseError databaseError) {

                    }
                });
    }

    /**
     * Called prior to saving, if piece by same title already exists in database. If confirmed,
     * overwrites data, if canceled, does nothing.
     *
     * @param title - title of the piece
     * @param composer - composer of the piece
     */
    private void overwriteFirebaseDataAlertDialog(final String title, final String composer) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity);
        dialog.setCancelable(false);
        dialog.setTitle("Overwrite Data?");
        dialog.setMessage(getString(R.string.overwrite_data_confirmation, title, composer));
        dialog.setPositiveButton(android.R.string.yes, (DialogInterface dialogInt, int id) -> saveToFirebase(mPieceOfMusic) )
                .setNegativeButton(R.string.cancel, (DialogInterface dialogInt, int which) -> dialogInt.dismiss() );

        final AlertDialog alert = dialog.create();
        alert.show();
    }

    @AddTrace(name = "saveToFirebase")
    private void saveToFirebase(final PieceOfMusic p) {
        Timber.d("Saving to local database, or to Firebase: " + p.getTitle() + " by " + p.getAuthor());
//        Timber.d("Pieces is " + p.getDownBeats().size() + " measures long.");

        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference mPiecesDatabaseReference = mDatabase.getReference();

        // Look for piece first, and if exists, get that key to update; otherwise push() to create
        // new key for new piece.
        mPiecesDatabaseReference.child("composers").child(p.getAuthor()).child(p.getTitle())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                        String key;
                        if(dataSnapshot.exists()) {
                            // update
                            key = dataSnapshot.getValue().toString();
                        } else {
                            // push to create
                            key = mPiecesDatabaseReference.child("pieces").push().getKey();
                        }

                        if(p.getFirebaseId() == null) {
                            p.setFirebaseId(key);
                        }

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("/pieces/" + key, p);
                        updates.put("/composers/" + p.getAuthor() + "/" + p.getTitle(), key);
                        mPiecesDatabaseReference.updateChildren(updates);

                        getFragmentManager().popBackStackImmediate();
                    }

                    @Override
                    public void onCancelled(@NotNull DatabaseError databaseError) {
                        databaseSaveErrorStayHere();
                    }
                });
    }

    private void databaseSaveErrorStayHere() {
        Toast.makeText(getContext(), R.string.error_save_canceled,
                Toast.LENGTH_SHORT).show();
    }

    private void toastError() {
        Toast.makeText(this.getContext(), R.string.enter_composer_name_and_title,
                Toast.LENGTH_SHORT).show();
    }

    @NotNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Timber.d("loader creation...");
        if(id == ID_PIECE_LOADER) {
            Uri queryUri = ProgramDatabaseSchema.MetProgram.CONTENT_URI;
            Timber.d("Uri: %s", queryUri.toString());
            return new CursorLoader(mActivity,
                    queryUri,
                    null,
                    null,
                    null,
                    null);
        } else {
            throw new RuntimeException("Unimplemented Loader Problem: " + id);
        }

    }

    @Override
    public void onLoadFinished(@NotNull Loader<Cursor> loader, Cursor data) {
        Timber.d("loader finished...");
        if(data == null || data.getCount() == 0) {
            Toast.makeText(mActivity, "Program Load Error", Toast.LENGTH_SHORT).show();
            mPieceOfMusic = null;
            updateGUI();
        } else {
            mCursor = data;
            getPieceFromSql();
        }
    }

    @Override
    public void onLoaderReset(@NotNull Loader<Cursor> loader) {
        Timber.d("onLoaderReset()");
        Timber.d("currentKey = %s", mCurrentPieceKey);
        mCursor = null;
    }


}
