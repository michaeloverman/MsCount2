/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.programmed

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.perf.metrics.AddTrace
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.database.LoadNewProgramActivity
import tech.michaeloverman.mscount.database.ProgramDatabaseSchema
import tech.michaeloverman.mscount.dataentry.MetaDataEntryFragment
import tech.michaeloverman.mscount.favorites.FavoritesContract
import tech.michaeloverman.mscount.favorites.FavoritesDBHelper
import tech.michaeloverman.mscount.pojos.PieceOfMusic
import tech.michaeloverman.mscount.utils.*
import timber.log.Timber

/**
 * Created by Michael on 2/24/2017.
 */
class ProgrammedMetronomeFragment : Fragment(), MetronomeStartStopListener, ProgrammedMetronomeListener, LoaderManager.LoaderCallbacks<Cursor?> {
    private var mCurrentPiece: PieceOfMusic? = null
    private var mCurrentPieceKey: String? = null
    private var mCurrentTempo = 0
    private var mCurrentComposer: String? = null
    private var mIsCurrentFavorite = false
    private var mMetronome: Metronome? = null
    private var mMetronomeRunning = false

    //    private WearNotification mWearNotification;
    private var mMetronomeBroadcastReceiver: BroadcastReceiver? = null

    //    private boolean mHasWearDevice;
    @JvmField
    @BindView(R.id.current_composer_name)
    var mTVCurrentComposer: TextView? = null

    @JvmField
    @BindView(R.id.current_program_title)
    var mTVCurrentPiece: TextView? = null

    @JvmField
    @BindView(R.id.current_tempo_setting)
    var mTVCurrentTempo: TextView? = null

    @JvmField
    @BindView(R.id.primary_beat_length_image)
    var mBeatLengthImage: ImageView? = null

    @JvmField
    @BindView(R.id.start_stop_fab)
    var mStartStopButton: FloatingActionButton? = null

    @JvmField
    @BindView(R.id.tempo_up_button)
    var mTempoUpButton: ImageButton? = null

    @JvmField
    @BindView(R.id.tempo_down_button)
    var mTempoDownButton: ImageButton? = null

    @JvmField
    @BindView(R.id.current_measure_number)
    var mCurrentMeasureNumber: TextView? = null

    @JvmField
    @BindView(R.id.help_overlay)
    var mInstructionsLayout: FrameLayout? = null

    //    private InterstitialAd mInterstitialAd;
    private var mRunnableHandler: Handler? = null
    private var mDownRunnable: Runnable? = null
    private var mUpRunnable: Runnable? = null
    private var mActivity: ProgrammedMetronomeActivity? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private fun setUpMetronome() {
        mMetronome = Metronome(mActivity)
        mMetronome!!.setMetronomeStartStopListener(this)
        mMetronome!!.setProgrammedMetronomeListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
        mActivity = activity as ProgrammedMetronomeActivity?
        setUpMetronome()

//        mHasWearDevice = PrefUtils.wearPresent(mActivity);
//        if(mHasWearDevice) {
//            createAndRegisterBroadcastReceiver();
//        }
        mActivity!!.title = getString(R.string.app_name)
        if (savedInstanceState != null) {
            Timber.d("found savedInstanceState")
            //            mCurrentTempo = savedInstanceState.getInt(CURRENT_TEMPO_KEY);
            mCurrentPieceKey = savedInstanceState.getString(CURRENT_PIECE_KEY_KEY)
            //            mCurrentComposer = savedInstanceState.getString(CURRENT_COMPOSER_KEY);
            Timber.d("savedInstanceState retrieved: composer: %s", mCurrentComposer)
            pieceFromKey
        } else {
            Timber.d("savedInstanceState not found - looking to SharedPrefs")
            //            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            mCurrentPieceKey = PrefUtils.getSavedPieceKey(mActivity)
            if (mCurrentPieceKey != null) {
                checkKeyFormat()
                pieceFromKey
            }
            mCurrentTempo = PrefUtils.getSavedTempo(mActivity)
        }
        mMetronomeRunning = false
        mRunnableHandler = Handler()
        mTempoChangeDelay = INITIAL_TEMPO_CHANGE_DELAY
        mDownRunnable = object : Runnable {
            override fun run() {
                if (mTempoChangeDelay == ONE_LESS) mTempoChangeDelay = FIRST_FASTER_SPEED_DELAY else if (mTempoChangeDelay < MIN_TEMPO_CHANGE_DELAY) mTempoChangeDelay = MIN_TEMPO_CHANGE_DELAY
                changeTempo(DOWN)
                mRunnableHandler!!.postDelayed(this, mTempoChangeDelay--.toLong())
            }
        }
        mUpRunnable = object : Runnable {
            override fun run() {
                if (mTempoChangeDelay == ONE_LESS) mTempoChangeDelay = FIRST_FASTER_SPEED_DELAY else if (mTempoChangeDelay < MIN_TEMPO_CHANGE_DELAY) mTempoChangeDelay = MIN_TEMPO_CHANGE_DELAY
                changeTempo(UP)
                mRunnableHandler!!.postDelayed(this, RATE_OF_DELAY_CHANGE.let { mTempoChangeDelay -= it; mTempoChangeDelay }.toLong())
            }
        }
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.programmed_fragment, container, false)
        ButterKnife.bind(this, view)

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
        mTempoDownButton!!.setOnTouchListener { v: View?, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> mRunnableHandler!!.post(mDownRunnable!!)
                MotionEvent.ACTION_UP -> {
                    mRunnableHandler!!.removeCallbacks(mDownRunnable!!)
                    mTempoChangeDelay = INITIAL_TEMPO_CHANGE_DELAY
                }
                else -> return@setOnTouchListener false
            }
            true
        }
        mTempoUpButton!!.setOnTouchListener { v: View?, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> mRunnableHandler!!.post(mUpRunnable!!)
                MotionEvent.ACTION_UP -> {
                    mRunnableHandler!!.removeCallbacks(mUpRunnable!!)
                    mTempoChangeDelay = INITIAL_TEMPO_CHANGE_DELAY
                }
                else -> return@setOnTouchListener false
            }
            true
        }
        mInstructionsLayout!!.isSoundEffectsEnabled = false
        if (mCurrentPiece != null) {
            updateGUI()
        }
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Timber.d("onCreateOptionsMenu")
        inflater.inflate(R.menu.programmed_menu, menu)
        val item = menu.findItem(R.id.mark_as_favorite_menu)
        if (mIsCurrentFavorite) {
            fillFavoriteMenuItem(item)
        } else {
            unfillFavoriteMenuItem(item)
        }
    }

    override fun onPause() {
        Timber.d("onPause()")
        if (mMetronomeRunning) metronomeStartStop()

//        cancelWearNotification();
        if (mMetronomeBroadcastReceiver != null) {
            requireActivity().unregisterReceiver(mMetronomeBroadcastReceiver)
        }
        PrefUtils.saveCurrentProgramToPrefs(mActivity, mActivity!!.useFirebase,
                mCurrentPieceKey, mCurrentTempo)
        super.onPause()
    }

    override fun onResume() {
        Timber.d("onResume()")
        super.onResume()
        createAndRegisterBroadcastReceiver()

//        if(mCurrentPiece != null) {
//            updateWearNotif();
//        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (mCurrentPiece != null) {
            outState.putString(CURRENT_PIECE_TITLE_KEY, mCurrentPiece!!.title)
            outState.putString(CURRENT_PIECE_KEY_KEY, mCurrentPieceKey)
            outState.putInt(CURRENT_TEMPO_KEY, mCurrentTempo)
            outState.putString(CURRENT_COMPOSER_KEY, mCurrentComposer)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        Timber.d("onDestroy() saving prefs....")
        PrefUtils.saveCurrentProgramToPrefs(mActivity, mActivity!!.useFirebase,
                mCurrentPieceKey, mCurrentTempo)
        Timber.d("Should have just saved $mCurrentPieceKey at $mCurrentTempo BPM")
        mCursor = null
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("FRAGMENT: onActivityResult()")
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(mActivity, R.string.return_result_problem, Toast.LENGTH_SHORT).show()
            return
        }
        if (requestCode == REQUEST_NEW_PROGRAM) {
            Timber.d("REQUEST_NEW_PROGRAM result received")
            mActivity!!.useFirebase = PrefUtils.usingFirebase(mActivity)
            mCurrentPieceKey = data!!.getStringExtra(LoadNewProgramActivity.EXTRA_NEW_PROGRAM)
            pieceFromKey
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("FRAGMENT: onOptionsItemSelected()")
        return when (item.itemId) {
            R.id.create_new_program_option -> {
                openProgramEditor()
                true
            }
            R.id.mark_as_favorite_menu -> {
                if (mCurrentPiece == null) {
                    Toast.makeText(mActivity, R.string.need_program_before_favorite,
                            Toast.LENGTH_SHORT).show()
                    return true
                }
                mIsCurrentFavorite = !mIsCurrentFavorite
                if (mIsCurrentFavorite) {
                    fillFavoriteMenuItem(item)
                    makePieceFavorite()
                    saveToSql()
                } else {
                    unfillFavoriteMenuItem(item)
                    makePieceUnfavorite()
                }
                true
            }
            R.id.help_menu_item -> {
                makeInstructionsVisible()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun makeInstructionsVisible() {
        mInstructionsLayout!!.visibility = View.VISIBLE
    }

    @OnClick(R.id.help_cancel_button)
    fun instructionsCancelled() {
        mInstructionsLayout!!.visibility = View.INVISIBLE
    }

    @OnClick(R.id.current_composer_name, R.id.current_program_title)
    fun selectNewProgram() {
//        cancelWearNotification();
        val intent = Intent(mActivity, LoadNewProgramActivity::class.java)
                .putExtra(EXTRA_COMPOSER_NAME, mCurrentComposer)
                .putExtra(EXTRA_USE_FIREBASE, mActivity!!.useFirebase)
        startActivityForResult(intent, REQUEST_NEW_PROGRAM)
    }

    @OnClick(R.id.start_stop_fab)
    override fun metronomeStartStop() {
        if (mCurrentTempo == 0) {
            return
        }
        if (mCurrentPiece == null) {
            Toast.makeText(mActivity, R.string.select_program_first, Toast.LENGTH_SHORT).show()
            return
        }
        if (mMetronome == null) {
            val bundle = Bundle()
            bundle.putString("piece", mCurrentPiece!!.title)
            bundle.putString("tempo", ":$mCurrentTempo")
            mFirebaseAnalytics!!.logEvent("nullMetronomeStartStop", bundle)
        }
        if (mMetronomeRunning) {
            Timber.d("metronomeStop() %s", mCurrentComposer)
            mMetronome!!.stop()
            mMetronomeRunning = false
            mStartStopButton!!.setImageResource(android.R.drawable.ic_media_play)
            mCurrentMeasureNumber!!.setText(R.string.double_dash_no_measure_number)
        } else {
            Timber.d("metronomeStart() %s", mCurrentPiece!!.title)
            mMetronomeRunning = true
            mStartStopButton!!.setImageResource(android.R.drawable.ic_media_pause)
            mMetronome!!.play(mCurrentPiece, mCurrentTempo)
        }
        //        if(mHasWearDevice) mWearNotification.sendStartStop();
    }

    override fun metronomeMeasureNumber(mm: String) {
        mCurrentMeasureNumber!!.text = mm
    }

    override fun metronomeStopAndShowAd() {
        metronomeStartStop()

//        if(mInterstitialAd.isLoaded()) {
//            mInterstitialAd.show();
//        }
    }

    @OnClick(R.id.help_overlay)
    fun ignoreClicks() {
        // catch and ignore click on the help screen, so other buttons aren't functional0
    }

    private fun checkKeyFormat() {
        Timber.d("Firebase: " + mActivity!!.useFirebase + " :: key: " + mCurrentPieceKey!![0])
        if (mCurrentPieceKey!![0] == '-') {
            mActivity!!.useFirebase = true
            PrefUtils.saveFirebaseStatus(mActivity, true)
        } else {
            mActivity!!.useFirebase = false
            PrefUtils.saveFirebaseStatus(mActivity, false)
        }
    }

    private val pieceFromKey: Unit
        private get() {
            Timber.d("getPieceFromKey() %s", mCurrentPieceKey)
            if (mCurrentPieceKey!![0] == '-') {
                pieceFromFirebase
            } else {
                if (mCursor == null) {
                    Timber.d("mCursor is null, initing loader...")
                    LoaderManager.getInstance(mActivity!!).initLoader(ID_PIECE_LOADER, null, this)
                } else {
                    Timber.d("mCursor exists, going straight to data")
                    pieceFromSql
                }
            }
        }

    @get:AddTrace(name = "getPieceFromFirebase", enabled = true)
    private val pieceFromFirebase: Unit
        private get() {
            Timber.d("getPieceFromFirebase()")
            FirebaseDatabase.getInstance().reference.child("pieces").child(mCurrentPieceKey!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            mCurrentPiece = dataSnapshot.getValue(PieceOfMusic::class.java)
                            updateVariables()
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Toast.makeText(context, R.string.database_error_try_again,
                                    Toast.LENGTH_SHORT).show()
                        }
                    })
        }

    // TODO This needs to be handled better. This method should NOT be called unless we are in the local database. How is it being called with Firebase ids in the first place?
    //        mCursor.close();
    @get:AddTrace(name = "getPieceFromSql", enabled = true)
    private val pieceFromSql: Unit
        private get() {
            Timber.d("getPieceFromSql()")
            val localDbId: Int
            localDbId = try { // TODO This needs to be handled better. This method should NOT be called unless we are in the local database. How is it being called with Firebase ids in the first place?
                mCurrentPieceKey!!.toInt()
            } catch (nfe: NumberFormatException) {
                Timber.d("Piece id not a number (Firebase/local databases confused again...")
                programNotFoundError(-1)
                return
            }
            mCursor!!.moveToFirst()
            while (mCursor!!.getInt(ProgramDatabaseSchema.MetProgram.POSITION_ID) != localDbId) {
                Timber.d("_id: " + mCursor!!.getInt(ProgramDatabaseSchema.MetProgram.POSITION_ID)
                        + " != " + localDbId)
                if (!mCursor!!.moveToNext()) {
                    programNotFoundError(localDbId)
                    return
                }
            }
            val builder = PieceOfMusic.Builder()
                    .author(mCursor!!.getString(ProgramDatabaseSchema.MetProgram.POSITION_COMPOSER))
                    .title(mCursor!!.getString(ProgramDatabaseSchema.MetProgram.POSITION_TITLE))
                    .subdivision(mCursor!!.getInt(ProgramDatabaseSchema.MetProgram.POSITION_PRIMANY_SUBDIVISIONS))
                    .countOffSubdivision(mCursor!!.getInt(ProgramDatabaseSchema.MetProgram.POSITION_COUNOFF_SUBDIVISIONS))
                    .defaultTempo(mCursor!!.getInt(ProgramDatabaseSchema.MetProgram.POSITION_DEFAULT_TEMPO))
                    .baselineNoteValue(mCursor!!.getInt(ProgramDatabaseSchema.MetProgram.POSITION_DEFAULT_RHYTHM))
                    .tempoMultiplier(mCursor!!.getDouble(ProgramDatabaseSchema.MetProgram.POSITION_TEMPO_MULTIPLIER))
                    .firstMeasureNumber(mCursor!!.getInt(ProgramDatabaseSchema.MetProgram.POSITION_MEASURE_COUNTE_OFFSET))
                    .dataEntries(mCursor!!.getString(ProgramDatabaseSchema.MetProgram.POSITION_DATA_ARRAY))
                    .firebaseId(mCursor!!.getString(ProgramDatabaseSchema.MetProgram.POSITION_FIREBASE_ID))
                    .creatorId(mCursor!!.getString(ProgramDatabaseSchema.MetProgram.POSITION_CREATOR_ID))
                    .displayNoteValue(mCursor!!.getInt(ProgramDatabaseSchema.MetProgram.POSITION_DISPLAY_RHYTHM))
            //        mCursor.close();
            mCurrentPiece = builder.build()
            updateVariables()
        }

    private fun programNotFoundError(id: Int) {
        Toast.makeText(mActivity, getString(R.string.id_not_found, id),
                Toast.LENGTH_SHORT).show()
    }

    private fun changeTempo(direction: Boolean) {
        if (direction) {
            mCurrentTempo++
        } else {
            mCurrentTempo--
        }
        if (mCurrentTempo < MINIMUM_TEMPO) {
            mCurrentTempo = MINIMUM_TEMPO
        } else if (mCurrentTempo > MAXIMUM_TEMPO) {
            mCurrentTempo = MAXIMUM_TEMPO
        }
        updateTempoView()
    }

    private fun updateTempoView() {
        mTVCurrentTempo!!.text = mCurrentTempo.toString()
    }

    private fun getNoteImageResource(noteValue: Int): Int {
        return when (noteValue) {
            PieceOfMusic.SIXTEENTH -> R.drawable.ic_16thnote
            PieceOfMusic.DOTTED_SIXTEENTH -> R.drawable.ic_dotted16th
            PieceOfMusic.EIGHTH -> R.drawable.ic_8th
            PieceOfMusic.DOTTED_EIGHTH -> R.drawable.ic_dotted8th
            PieceOfMusic.DOTTED_QUARTER -> R.drawable.ic_dotted_4th
            PieceOfMusic.HALF -> R.drawable.ic_half
            PieceOfMusic.DOTTED_HALF -> R.drawable.ic_dotted_2th
            PieceOfMusic.WHOLE -> R.drawable.ic_whole
            PieceOfMusic.QUARTER -> R.drawable.ic_quarter
            else -> R.drawable.ic_quarter
        }
    }

    private fun getBeatLengthContentDescription(noteValue: Int): String {
        return when (noteValue) {
            PieceOfMusic.SIXTEENTH -> getString(R.string.sixteenth_content_description)
            PieceOfMusic.DOTTED_SIXTEENTH -> getString(R.string.dotted_sixteenth_content_description)
            PieceOfMusic.EIGHTH -> getString(R.string.eighth_content_description)
            PieceOfMusic.DOTTED_EIGHTH -> getString(R.string.dotted_eighth_content_description)
            PieceOfMusic.DOTTED_QUARTER -> getString(R.string.dotted_quarter_content_description)
            PieceOfMusic.HALF -> getString(R.string.half_content_description)
            PieceOfMusic.DOTTED_HALF -> getString(R.string.dotted_half_content_description)
            PieceOfMusic.WHOLE -> getString(R.string.whole_content_description)
            PieceOfMusic.QUARTER -> getString(R.string.quarter_content_description)
            else -> getString(R.string.quarter_content_description)
        }
    }

    private fun updateVariables() {
        if (mCurrentPiece == null) {
            selectNewProgram()
            return
        }
        Timber.d("newPiece() %s", mCurrentPiece!!.title)
        mCurrentComposer = mCurrentPiece!!.author
        //        mCurrentPieceKey = mCurrentPiece.getFirebaseId();
        if (mCurrentPiece!!.defaultTempo != 0) {
            mCurrentTempo = mCurrentPiece!!.defaultTempo
        }
        updateGUI()

//        updateWearNotif();
        if (mCurrentPiece!!.firebaseId != null) {
            CheckIfFavoriteTask().execute(mCurrentPiece!!.firebaseId)
        } else {
            CheckIfFavoriteTask().execute(mCurrentPieceKey)
        }
    }

    private fun updateGUI() {
        if (mCurrentPiece == null) {
            mTVCurrentPiece!!.setText(R.string.no_composer_empty_space)
            mTVCurrentComposer!!.setText(R.string.select_a_program)
        } else {
            mTVCurrentPiece!!.text = mCurrentPiece!!.title
            mTVCurrentComposer!!.text = mCurrentComposer
            mBeatLengthImage!!.setImageResource(getNoteImageResource(mCurrentPiece!!.displayNoteValue))
            mBeatLengthImage!!.contentDescription = getString(R.string.note_value_note_equals,
                    getBeatLengthContentDescription(mCurrentPiece!!.displayNoteValue))
            mCurrentTempo = mCurrentPiece!!.defaultTempo
            updateTempoView()
        }
    }

    private fun createAndRegisterBroadcastReceiver() {
        if (mMetronomeBroadcastReceiver == null) {
            mMetronomeBroadcastReceiver = MetronomeBroadcastReceiver(this)
        }
        val filter = IntentFilter(Metronome.ACTION_METRONOME_START_STOP)
        //        BroadcastManager manager = LocalBroadcastManager.getInstance(mActivity);
        mActivity!!.registerReceiver(mMetronomeBroadcastReceiver, filter)
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
    private fun openProgramEditor() {
        val fragment = MetaDataEntryFragment.newInstance(mActivity, mCursor)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun makePieceFavorite() {
        Timber.d("making piece a favorite!")
        val db = FavoritesDBHelper(mActivity).writableDatabase
        val values = ContentValues()
        if (mCurrentPiece!!.firebaseId == null) {
            if (mCurrentPieceKey!![0] == '-') {
                mCurrentPiece!!.firebaseId = mCurrentPieceKey
            } else {
                Toast.makeText(mActivity,
                        R.string.favorite_database_error_try_again, Toast.LENGTH_SHORT).show()
            }
        }
        values.put(FavoritesContract.FavoriteEntry.COLUMN_PIECE_ID, mCurrentPiece!!.firebaseId)
        db.insert(FavoritesContract.FavoriteEntry.TABLE_NAME, null, values)
        db.close()
    }

    private fun makePieceUnfavorite() {
        Timber.d("unfavoriting the piece...")
        val db = FavoritesDBHelper(mActivity).writableDatabase
        val selection = FavoritesContract.FavoriteEntry.COLUMN_PIECE_ID + " LIKE ?"
        val selectionArgs = arrayOf(mCurrentPiece!!.firebaseId)
        db.delete(FavoritesContract.FavoriteEntry.TABLE_NAME, selection, selectionArgs)
        db.close()
    }

    private fun fillFavoriteMenuItem(item: MenuItem) {
        item.setIcon(R.drawable.ic_heart)
        item.title = getString(R.string.mark_as_unfavorite_menu)
    }

    private fun unfillFavoriteMenuItem(item: MenuItem) {
        item.setIcon(R.drawable.ic_heart_outline)
        item.title = getString(R.string.mark_as_favorite_menu)
    }

    private fun saveToSql() {
        val contentValues = Utilities.getContentValuesFromPiece(mCurrentPiece)
        val resolver = requireContext().contentResolver
        resolver.insert(ProgramDatabaseSchema.MetProgram.CONTENT_URI, contentValues)
    }

    private inner class CheckIfFavoriteTask : AsyncTask<String?, Void?, Boolean>() {
        override fun doInBackground(vararg params: String?): Boolean {
            Timber.d("CheckIfFavoriteTask running in background!!")
            if (params[0] == null) {
                Timber.d("...no id to check against favorites db")
                return false
            }
            val db = FavoritesDBHelper(mActivity).readableDatabase
            val cursor = db.query(FavoritesContract.FavoriteEntry.TABLE_NAME,
                    null,
                    FavoritesContract.FavoriteEntry.COLUMN_PIECE_ID + " =?",
                    params,
                    null, null, null)
            val exists = cursor.count > 0
            cursor.close()
            db.close()
            return exists
        }

        override fun onPostExecute(aBoolean: Boolean) {
            mIsCurrentFavorite = aBoolean
            mActivity!!.invalidateOptionsMenu()
        }

    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        Timber.d("loader creation...")
        return if (id == ID_PIECE_LOADER) {
            val queryUri = ProgramDatabaseSchema.MetProgram.CONTENT_URI
            Timber.d("Uri: %s", queryUri.toString())
            CursorLoader(mActivity!!,
                    queryUri,
                    null,
                    null,
                    null,
                    null)
        } else {
            throw RuntimeException("Unimplemented Loader Problem: $id")
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor?>, data: Cursor?) {
        Timber.d("loader finished...")
        if (data == null || data.count == 0) {
            Toast.makeText(mActivity, "Program Load Error", Toast.LENGTH_SHORT).show()
            mCurrentPiece = null
            updateGUI()
        } else {
            mCursor = data
            pieceFromSql
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor?>) {
        Timber.d("onLoaderReset()")
        Timber.d("currentKey = %s", mCurrentPieceKey)
        mCursor = null
    }

    companion object {
        private const val UP = true
        private const val DOWN = false
        private const val MAXIMUM_TEMPO = 350
        private const val MINIMUM_TEMPO = 1
        private const val CURRENT_PIECE_TITLE_KEY = "current_piece_title_key"
        private const val CURRENT_PIECE_KEY_KEY = "current_piece_key_key"
        private const val CURRENT_TEMPO_KEY = "current_tempo_key"
        private const val CURRENT_COMPOSER_KEY = "current_composer_key"
        private const val ID_PIECE_LOADER = 434
        private const val REQUEST_NEW_PROGRAM = 44
        const val EXTRA_COMPOSER_NAME = "composer_name_extra"
        const val EXTRA_USE_FIREBASE = "program_database_option"
        private const val INITIAL_TEMPO_CHANGE_DELAY = 400
        private const val FIRST_FASTER_SPEED_DELAY = 80
        private const val RATE_OF_DELAY_CHANGE = 2
        private var mTempoChangeDelay = 0
        private const val ONE_LESS = INITIAL_TEMPO_CHANGE_DELAY - 2
        private const val MIN_TEMPO_CHANGE_DELAY = 20
        private var mCursor: Cursor? = null
        fun newInstance(): Fragment {
            return ProgrammedMetronomeFragment()
        }
    }
}