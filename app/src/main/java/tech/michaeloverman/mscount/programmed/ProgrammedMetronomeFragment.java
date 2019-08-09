/* Copyright (C) 2017 Michael Overman - All Rights Reserved */

package tech.michaeloverman.mscount.programmed;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.perf.metrics.AddTrace;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tech.michaeloverman.mscount.R;
import tech.michaeloverman.mscount.database.LoadNewProgramActivity;
import tech.michaeloverman.mscount.database.ProgramDatabaseSchema;
import tech.michaeloverman.mscount.dataentry.MetaDataEntryFragment;
import tech.michaeloverman.mscount.favorites.FavoritesContract;
import tech.michaeloverman.mscount.favorites.FavoritesDBHelper;
import tech.michaeloverman.mscount.pojos.PieceOfMusic;
import tech.michaeloverman.mscount.utils.Metronome;
import tech.michaeloverman.mscount.utils.MetronomeBroadcastReceiver;
import tech.michaeloverman.mscount.utils.MetronomeStartStopListener;
import tech.michaeloverman.mscount.utils.PrefUtils;
import tech.michaeloverman.mscount.utils.ProgrammedMetronomeListener;
import tech.michaeloverman.mscount.utils.Utilities;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Michael on 2/24/2017.
 */

public class ProgrammedMetronomeFragment extends Fragment
        implements MetronomeStartStopListener, ProgrammedMetronomeListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final boolean UP = true;
    private static final boolean DOWN = false;
    private static final int MAXIMUM_TEMPO = 350;
    private static final int MINIMUM_TEMPO = 1;
    private static final String CURRENT_PIECE_TITLE_KEY = "current_piece_title_key";
    private static final String CURRENT_PIECE_KEY_KEY = "current_piece_key_key";
    private static final String CURRENT_TEMPO_KEY = "current_tempo_key";
    private static final String CURRENT_COMPOSER_KEY = "current_composer_key";
    private static final int ID_PIECE_LOADER = 434;

    private static final int REQUEST_NEW_PROGRAM = 44;
    public static final String EXTRA_COMPOSER_NAME = "composer_name_extra";
    public static final String EXTRA_USE_FIREBASE = "program_database_option";

    private PieceOfMusic mCurrentPiece;
    private String mCurrentPieceKey;
    private int mCurrentTempo;
    private String mCurrentComposer;
    private boolean mIsCurrentFavorite;
    private Metronome mMetronome;
    private boolean mMetronomeRunning;

//    private WearNotification mWearNotification;
    private BroadcastReceiver mMetronomeBroadcastReceiver;
//    private boolean mHasWearDevice;

    @BindView(R.id.current_composer_name) TextView mTVCurrentComposer;
    @BindView(R.id.current_program_title) TextView mTVCurrentPiece;
    @BindView(R.id.current_tempo_setting) TextView mTVCurrentTempo;
    @BindView(R.id.primary_beat_length_image) ImageView mBeatLengthImage;
    @BindView(R.id.start_stop_fab) FloatingActionButton mStartStopButton;
    @BindView(R.id.tempo_up_button) ImageButton mTempoUpButton;
    @BindView(R.id.tempo_down_button) ImageButton mTempoDownButton;
    @BindView(R.id.current_measure_number) TextView mCurrentMeasureNumber;

    @BindView(R.id.help_overlay) FrameLayout mInstructionsLayout;

//    private InterstitialAd mInterstitialAd;

    private Handler mRunnableHandler;
    private Runnable mDownRunnable;
    private Runnable mUpRunnable;
    private static final int INITIAL_TEMPO_CHANGE_DELAY = 400;
    private static final int FIRST_FASTER_SPEED_DELAY = 80;
    private static final int RATE_OF_DELAY_CHANGE = 2;
    private static int mTempoChangeDelay;
    private static final int ONE_LESS = INITIAL_TEMPO_CHANGE_DELAY - 2;
    private static final int MIN_TEMPO_CHANGE_DELAY = 20;

    private ProgrammedMetronomeActivity mActivity;
    private static Cursor mCursor;

    public static Fragment newInstance() {
        return new ProgrammedMetronomeFragment();
    }
    
    private FirebaseAnalytics mFirebaseAnalytics;

    private void setUpMetronome() {
        mMetronome = new Metronome(mActivity);
        mMetronome.setMetronomeStartStopListener(this);
        mMetronome.setProgrammedMetronomeListener(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mActivity = (ProgrammedMetronomeActivity) getActivity();

        setUpMetronome();

//        mHasWearDevice = PrefUtils.wearPresent(mActivity);
//        if(mHasWearDevice) {
//            createAndRegisterBroadcastReceiver();
//        }

        mActivity.setTitle(getString(R.string.app_name));

        if(savedInstanceState != null) {
            Timber.d("found savedInstanceState");
//            mCurrentTempo = savedInstanceState.getInt(CURRENT_TEMPO_KEY);
            mCurrentPieceKey = savedInstanceState.getString(CURRENT_PIECE_KEY_KEY);
//            mCurrentComposer = savedInstanceState.getString(CURRENT_COMPOSER_KEY);
            Timber.d("savedInstanceState retrieved: composer: %s", mCurrentComposer);
            getPieceFromKey();
        } else {
            Timber.d("savedInstanceState not found - looking to SharedPrefs");
//            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            mCurrentPieceKey = PrefUtils.getSavedPieceKey(mActivity);

            if(mCurrentPieceKey != null) {
                checkKeyFormat();
                getPieceFromKey();
            }
            mCurrentTempo = PrefUtils.getSavedTempo(mActivity);
        }

        mMetronomeRunning = false;

        mRunnableHandler = new Handler();
        mTempoChangeDelay = INITIAL_TEMPO_CHANGE_DELAY;
        mDownRunnable = new Runnable() {
            @Override
            public void run() {
                if(mTempoChangeDelay == ONE_LESS) mTempoChangeDelay = FIRST_FASTER_SPEED_DELAY;
                else if (mTempoChangeDelay < MIN_TEMPO_CHANGE_DELAY) mTempoChangeDelay = MIN_TEMPO_CHANGE_DELAY;
                changeTempo(DOWN);
                mRunnableHandler.postDelayed(this, mTempoChangeDelay--);
            }
        };
        mUpRunnable = new Runnable() {
            @Override
            public void run() {
                if(mTempoChangeDelay == ONE_LESS) mTempoChangeDelay = FIRST_FASTER_SPEED_DELAY;
                else if (mTempoChangeDelay < MIN_TEMPO_CHANGE_DELAY) mTempoChangeDelay = MIN_TEMPO_CHANGE_DELAY;
                changeTempo(UP);
                mRunnableHandler.postDelayed(this, mTempoChangeDelay -= RATE_OF_DELAY_CHANGE);
            }
        };
    
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.programmed_fragment, container, false);
        ButterKnife.bind(this, view);

//        mInterstitialAd = new InterstitialAd(mActivity);
//        mInterstitialAd.setAdUnitId(getString(R.string.programmed_interstitial_unit_id));
//        AdRequest.Builder adBuilder = new AdRequest.Builder();
//        if(BuildConfig.DEBUG) {
//            adBuilder.addTestDevice(getString(R.string.test_device_code));
//        }
//        final AdRequest adRequest = adBuilder.build();
//        mInterstitialAd.loadAd(adRequest);
//        mInterstitialAd.setAdListener(new AdListener() {
//            @Override
//            public void onAdClosed() {
//                super.onAdClosed();
//                mInterstitialAd.loadAd(adRequest);
//            }
//        });


        mTempoDownButton.setOnTouchListener( (View v, MotionEvent event) -> {
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mRunnableHandler.post(mDownRunnable);
                    break;
                case MotionEvent.ACTION_UP:
                    mRunnableHandler.removeCallbacks(mDownRunnable);
                    mTempoChangeDelay = INITIAL_TEMPO_CHANGE_DELAY;
                    break;
                default:
                    return false;
            }
            return true;
        });
        mTempoUpButton.setOnTouchListener( (View v, MotionEvent event) -> {
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mRunnableHandler.post(mUpRunnable);
                    break;
                case MotionEvent.ACTION_UP:
                    mRunnableHandler.removeCallbacks(mUpRunnable);
                    mTempoChangeDelay = INITIAL_TEMPO_CHANGE_DELAY;
                    break;
                default:
                    return false;
            }
            return true;
        });
    
        mInstructionsLayout.setSoundEffectsEnabled(false);
    
        if(mCurrentPiece != null) {
            updateGUI();
        }
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Timber.d("onCreateOptionsMenu");
        inflater.inflate(R.menu.programmed_menu, menu);
        MenuItem item = menu.findItem(R.id.mark_as_favorite_menu);
        if(mIsCurrentFavorite) {
            fillFavoriteMenuItem(item);
        } else {
            unfillFavoriteMenuItem(item);
        }
    }

    @Override
    public void onPause() {
        Timber.d("onPause()");
        if(mMetronomeRunning) metronomeStartStop();

//        cancelWearNotification();
        if(mMetronomeBroadcastReceiver != null) {
            getActivity().unregisterReceiver(mMetronomeBroadcastReceiver);
        }
        
        PrefUtils.saveCurrentProgramToPrefs(mActivity, mActivity.useFirebase,
                mCurrentPieceKey, mCurrentTempo);
        super.onPause();
    }

    @Override
    public void onResume() {
        Timber.d("onResume()");
        super.onResume();

        createAndRegisterBroadcastReceiver();

//        if(mCurrentPiece != null) {
//            updateWearNotif();
//        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        if(mCurrentPiece != null) {
            outState.putString(CURRENT_PIECE_TITLE_KEY, mCurrentPiece.getTitle());
            outState.putString(CURRENT_PIECE_KEY_KEY, mCurrentPieceKey);
            outState.putInt(CURRENT_TEMPO_KEY, mCurrentTempo);
            outState.putString(CURRENT_COMPOSER_KEY, mCurrentComposer);
        }
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy() saving prefs....");
        PrefUtils.saveCurrentProgramToPrefs(mActivity, mActivity.useFirebase,
                mCurrentPieceKey, mCurrentTempo);

        Timber.d("Should have just saved " + mCurrentPieceKey + " at " + mCurrentTempo + " BPM");

        mCursor = null;

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.d("FRAGMENT: onActivityResult()");
        if(resultCode != RESULT_OK) {
            Toast.makeText(mActivity, R.string.return_result_problem, Toast.LENGTH_SHORT).show();
            return;
        }
        if (requestCode == REQUEST_NEW_PROGRAM) {
            Timber.d("REQUEST_NEW_PROGRAM result received");
            mActivity.useFirebase = PrefUtils.usingFirebase(mActivity);
            mCurrentPieceKey = data.getStringExtra(LoadNewProgramActivity.EXTRA_NEW_PROGRAM);
            getPieceFromKey();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("FRAGMENT: onOptionsItemSelected()");
        switch (item.getItemId()) {
            case R.id.create_new_program_option:
                openProgramEditor();
                return true;
            case R.id.mark_as_favorite_menu:
                if(mCurrentPiece == null) {
                    Toast.makeText(mActivity, R.string.need_program_before_favorite,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                mIsCurrentFavorite = !mIsCurrentFavorite;
                if(mIsCurrentFavorite) {
                    fillFavoriteMenuItem(item);
                    makePieceFavorite();
                    saveToSql();
                } else {
                    unfillFavoriteMenuItem(item);
                    makePieceUnfavorite();
                }
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


    @OnClick( { R.id.current_composer_name, R.id.current_program_title } )
    public void selectNewProgram() {
//        cancelWearNotification();
        Intent intent = new Intent(mActivity, LoadNewProgramActivity.class)
                .putExtra(EXTRA_COMPOSER_NAME, mCurrentComposer)
                .putExtra(EXTRA_USE_FIREBASE, mActivity.useFirebase);

        startActivityForResult(intent, REQUEST_NEW_PROGRAM);
    }

    @OnClick(R.id.start_stop_fab)
    public void metronomeStartStop() {
        if(mCurrentTempo == 0) {
            return;
        }
        if(mCurrentPiece == null) {
            Toast.makeText(mActivity, R.string.select_program_first, Toast.LENGTH_SHORT).show();
            return;
        }
        if(mMetronome == null) {
            Bundle bundle = new Bundle();
            bundle.putString("piece", mCurrentPiece.getTitle());
            bundle.putString("tempo", ":"+mCurrentTempo);
            mFirebaseAnalytics.logEvent("nullMetronomeStartStop", bundle);
        }
        if(mMetronomeRunning) {
            Timber.d("metronomeStop() %s", mCurrentComposer);
            mMetronome.stop();
            mMetronomeRunning = false;
            mStartStopButton.setImageResource(android.R.drawable.ic_media_play);
            mCurrentMeasureNumber.setText(R.string.double_dash_no_measure_number);
        } else {
            Timber.d("metronomeStart() %s", mCurrentPiece.getTitle());
            mMetronomeRunning = true;
            mStartStopButton.setImageResource(android.R.drawable.ic_media_pause);
            mMetronome.play(mCurrentPiece, mCurrentTempo);
        }
//        if(mHasWearDevice) mWearNotification.sendStartStop();
    }

    @Override
    public void metronomeMeasureNumber(String mm) {
        mCurrentMeasureNumber.setText(mm);
    }

    @Override
    public void metronomeStopAndShowAd() {
        metronomeStartStop();

//        if(mInterstitialAd.isLoaded()) {
//            mInterstitialAd.show();
//        }
    }
    
    @SuppressWarnings("EmptyMethod")
    @OnClick(R.id.help_overlay)
    public void ignoreClicks() {
        // catch and ignore click on the help screen, so other buttons aren't functional0
    }

    private void checkKeyFormat() {
        Timber.d("Firebase: " + mActivity.useFirebase + " :: key: " + mCurrentPieceKey.charAt(0));

        if(mCurrentPieceKey.charAt(0) == '-') {
            mActivity.useFirebase = true;
            PrefUtils.saveFirebaseStatus(mActivity, true);
        } else {
            mActivity.useFirebase = false;
            PrefUtils.saveFirebaseStatus(mActivity, false);
        }

    }

    private void getPieceFromKey() {
        Timber.d("getPieceFromKey() %s", mCurrentPieceKey);

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

    @AddTrace(name = "getPieceFromFirebase", enabled = true)
    private void getPieceFromFirebase() {
        Timber.d("getPieceFromFirebase()");
        FirebaseDatabase.getInstance().getReference().child("pieces").child(mCurrentPieceKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                        mCurrentPiece = dataSnapshot.getValue(PieceOfMusic.class);
                        updateVariables();
                    }

                    @Override
                    public void onCancelled(@NotNull DatabaseError databaseError) {
                        Toast.makeText(getContext(), R.string.database_error_try_again,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    @AddTrace(name = "getPieceFromSql", enabled = true /* optional */)
    private void getPieceFromSql() {
        Timber.d("getPieceFromSql()");
        int localDbId;
        try { // TODO This needs to be handled better. This method should NOT be called unless we are in the local database. How is it being called with Firebase ids in the first place?
            localDbId = Integer.parseInt(mCurrentPieceKey);
        } catch (NumberFormatException nfe) {
            Timber.d("Piece id not a number (Firebase/local databases confused again...");
            programNotFoundError(-1);
            return;
        }
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
                .firebaseId(mCursor.getString(ProgramDatabaseSchema.MetProgram.POSITION_FIREBASE_ID))
                .creatorId(mCursor.getString(ProgramDatabaseSchema.MetProgram.POSITION_CREATOR_ID))
                .displayNoteValue(mCursor.getInt(ProgramDatabaseSchema.MetProgram.POSITION_DISPLAY_RHYTHM));
//        mCursor.close();
        mCurrentPiece = builder.build();
        updateVariables();
    }

    private void programNotFoundError(int id) {
        Toast.makeText(mActivity, getString(R.string.id_not_found, id),
                Toast.LENGTH_SHORT).show();
    }


    private void changeTempo(boolean direction) {
        if(direction) {
            mCurrentTempo++;
        } else {
            mCurrentTempo--;
        }
        if(mCurrentTempo < MINIMUM_TEMPO) {
            mCurrentTempo = MINIMUM_TEMPO;
        } else if(mCurrentTempo > MAXIMUM_TEMPO) {
            mCurrentTempo = MAXIMUM_TEMPO;
        }
        updateTempoView();

    }

    private void updateTempoView() {
        mTVCurrentTempo.setText(String.valueOf(mCurrentTempo));
    }

    private int getNoteImageResource(int noteValue) {
        switch(noteValue) {
            case PieceOfMusic.SIXTEENTH:
                return R.drawable.ic_16thnote;
            case PieceOfMusic.DOTTED_SIXTEENTH:
                return R.drawable.ic_dotted16th;
            case PieceOfMusic.EIGHTH:
                return R.drawable.ic_8th;
            case PieceOfMusic.DOTTED_EIGHTH:
                return R.drawable.ic_dotted8th;
            case PieceOfMusic.DOTTED_QUARTER:
                return R.drawable.ic_dotted_4th;
            case PieceOfMusic.HALF:
                return R.drawable.ic_half;
            case PieceOfMusic.DOTTED_HALF:
                return R.drawable.ic_dotted_2th;
            case PieceOfMusic.WHOLE:
                return R.drawable.ic_whole;
            case PieceOfMusic.QUARTER:
            default:
                return R.drawable.ic_quarter;
        }
    }

    private String getBeatLengthContentDescription(int noteValue) {
        switch(noteValue) {
            case PieceOfMusic.SIXTEENTH:
                return getString(R.string.sixteenth_content_description);
            case PieceOfMusic.DOTTED_SIXTEENTH:
                return getString(R.string.dotted_sixteenth_content_description);
            case PieceOfMusic.EIGHTH:
                return getString(R.string.eighth_content_description);
            case PieceOfMusic.DOTTED_EIGHTH:
                return getString(R.string.dotted_eighth_content_description);
            case PieceOfMusic.DOTTED_QUARTER:
                return getString(R.string.dotted_quarter_content_description);
            case PieceOfMusic.HALF:
                return getString(R.string.half_content_description);
            case PieceOfMusic.DOTTED_HALF:
                return getString(R.string.dotted_half_content_description);
            case PieceOfMusic.WHOLE:
                return getString(R.string.whole_content_description);
            case PieceOfMusic.QUARTER:
            default:
                return getString(R.string.quarter_content_description);
        }
    }

    private void updateVariables() {
        if(mCurrentPiece == null) {
            selectNewProgram();
            return;
        }

        Timber.d("newPiece() %s", mCurrentPiece.getTitle());

        mCurrentComposer = mCurrentPiece.getAuthor();
//        mCurrentPieceKey = mCurrentPiece.getFirebaseId();
        if(mCurrentPiece.getDefaultTempo() != 0) {
            mCurrentTempo = mCurrentPiece.getDefaultTempo();
        }

        updateGUI();

//        updateWearNotif();

        if(mCurrentPiece.getFirebaseId() != null ) {
            new CheckIfFavoriteTask().execute(mCurrentPiece.getFirebaseId());
        } else {
            new CheckIfFavoriteTask().execute(mCurrentPieceKey);
        }
    }

    private void updateGUI() {
        if(mCurrentPiece == null) {
            mTVCurrentPiece.setText(R.string.no_composer_empty_space);
            mTVCurrentComposer.setText(R.string.select_a_program);
        } else {
            mTVCurrentPiece.setText(mCurrentPiece.getTitle());
            mTVCurrentComposer.setText(mCurrentComposer);
            mBeatLengthImage.setImageResource(getNoteImageResource
                    (mCurrentPiece.getDisplayNoteValue()));
            mBeatLengthImage.setContentDescription(getString(R.string.note_value_note_equals,
                    getBeatLengthContentDescription(mCurrentPiece.getDisplayNoteValue())));
            mCurrentTempo = mCurrentPiece.getDefaultTempo();
            updateTempoView();
        }
    }

    private void createAndRegisterBroadcastReceiver() {
        if(mMetronomeBroadcastReceiver == null) {
            mMetronomeBroadcastReceiver = new MetronomeBroadcastReceiver(this);
        }
        IntentFilter filter = new IntentFilter(Metronome.ACTION_METRONOME_START_STOP);
//        BroadcastManager manager = LocalBroadcastManager.getInstance(mActivity);
        mActivity.registerReceiver(mMetronomeBroadcastReceiver, filter);
    }

//    private void updateWearNotif() {
//        if(mHasWearDevice) {
//            mWearNotification = new WearNotification(mActivity,
//                    mCurrentComposer, mCurrentPiece.getTitle());
//            mWearNotification.sendStartStop();
//        }
//    }

//    private void cancelWearNotification() {
//        if(mWearNotification != null) {
//            mWearNotification.cancel();
//        }
////        if(mMetronomeBroadcastReceiver != null) {
////            mActivity.unregisterReceiver(mMetronomeBroadcastReceiver);
////        }
//    }

    private void openProgramEditor() {
        Fragment fragment = MetaDataEntryFragment.newInstance(mActivity, mCursor);
        FragmentTransaction trans = getFragmentManager().beginTransaction();
        trans.replace(R.id.fragment_container, fragment);
        trans.addToBackStack(null);
        trans.commit();
    }

    private void makePieceFavorite() {
        Timber.d("making piece a favorite!");
        final SQLiteDatabase db = new FavoritesDBHelper(mActivity).getWritableDatabase();
        ContentValues values = new ContentValues();
        if(mCurrentPiece.getFirebaseId() == null) {
            if(mCurrentPieceKey.charAt(0) == '-') {
                mCurrentPiece.setFirebaseId(mCurrentPieceKey);
            } else {
                Toast.makeText(mActivity,
                        R.string.favorite_database_error_try_again, Toast.LENGTH_SHORT).show();
            }
        }
        values.put(FavoritesContract.FavoriteEntry.COLUMN_PIECE_ID, mCurrentPiece.getFirebaseId());
        db.insert(FavoritesContract.FavoriteEntry.TABLE_NAME, null, values);
        db.close();
    }

    private void makePieceUnfavorite() {
        Timber.d("unfavoriting the piece...");
        final SQLiteDatabase db = new FavoritesDBHelper(mActivity).getWritableDatabase();
        String selection = FavoritesContract.FavoriteEntry.COLUMN_PIECE_ID + " LIKE ?";
        String[] selectionArgs = { mCurrentPiece.getFirebaseId() };
        db.delete(FavoritesContract.FavoriteEntry.TABLE_NAME, selection, selectionArgs);
        db.close();
    }

    private void fillFavoriteMenuItem(MenuItem item) {
        item.setIcon(R.drawable.ic_heart);
        item.setTitle(getString(R.string.mark_as_unfavorite_menu));
    }

    private void unfillFavoriteMenuItem(MenuItem item) {
        item.setIcon(R.drawable.ic_heart_outline);
        item.setTitle(getString(R.string.mark_as_favorite_menu));
    }

    private void saveToSql() {
        ContentValues contentValues = Utilities.getContentValuesFromPiece(mCurrentPiece);
        ContentResolver resolver = getContext().getContentResolver();
        resolver.insert(ProgramDatabaseSchema.MetProgram.CONTENT_URI, contentValues);
    }

    private class CheckIfFavoriteTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            Timber.d("CheckIfFavoriteTask running in background!!");
            if(params[0] == null) {
                Timber.d("...no id to check against favorites db");
                return false;
            }
            SQLiteDatabase db = new FavoritesDBHelper(mActivity).getReadableDatabase();
            Cursor cursor = db.query(FavoritesContract.FavoriteEntry.TABLE_NAME,
                    null,
                    FavoritesContract.FavoriteEntry.COLUMN_PIECE_ID + " =?",
                    params,
                    null, null, null);
            boolean exists = (cursor.getCount() > 0);
            cursor.close();
            db.close();
            return exists;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            mIsCurrentFavorite = aBoolean;
            mActivity.invalidateOptionsMenu();
        }
    }

    @NotNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Timber.d("loader creation...");
        if (id == ID_PIECE_LOADER) {
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
            mCurrentPiece = null;
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
