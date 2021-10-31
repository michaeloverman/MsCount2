/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.programmed

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.perf.metrics.AddTrace
import tech.michaeloverman.mscount.BuildConfig
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.database.LoadNewProgramActivity
import tech.michaeloverman.mscount.database.ProgramDatabaseSchema
import tech.michaeloverman.mscount.databinding.ProgrammedFragmentBinding
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
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var useFirebase = false
    private var databaseMenuItem: MenuItem? = null

    private var mCurrentPiece: PieceOfMusic? = null
    private var mCurrentPieceKey: String? = null
    private var mCurrentTempo = 0
    private var mCurrentComposer: String? = null
    private var mIsCurrentFavorite = false
    private lateinit var mMetronome: Metronome
    private var mMetronomeRunning = false

    private var mRunnableHandler: Handler? = null
    private var mDownRunnable: Runnable? = null
    private var mUpRunnable: Runnable? = null
    private lateinit var mActivity: AppCompatActivity
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    private fun setUpMetronome() {
        mMetronome = Metronome(requireContext())
        mMetronome.setMetronomeStartStopListener(this)
        mMetronome.setProgrammedMetronomeListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
        mActivity = activity as AppCompatActivity
        setUpMetronome()

        useFirebase = PrefUtils.usingFirebase(mActivity)
        mAuth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth: FirebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                Timber.d("onAuthStateChanged:signed_in: %s", user.uid)
                //                    useFirebase = true;
            } else {
                // User is signed out
                Timber.d("onAuthStateChanged:signed_out")
                goLocal()
            }
        }
        if (mAuth!!.currentUser == null) {
            signInToFirebase()
        }

        mActivity.title = getString(R.string.app_name)
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

        mAuth!!.addAuthStateListener(mAuthListener!!)
        useFirebase = PrefUtils.usingFirebase(mActivity)
        if (databaseMenuItem != null) {
            updateDatabaseOptionMenuItem()
        }
    }

    private var _binding: ProgrammedFragmentBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ProgrammedFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (mAuthListener != null) {
            mAuth!!.removeAuthStateListener(mAuthListener!!)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.overlayInclude.helpCancelButton.setOnClickListener { binding.helpOverlay.visibility = View.INVISIBLE }
        binding.currentComposerName.setOnClickListener { selectNewProgram() }
        binding.currentProgramTitle.setOnClickListener { selectNewProgram() }
        binding.startStopFab.setOnClickListener { metronomeStartStop() }
        binding.helpOverlay.setOnClickListener { ignoreClicks() }

        binding.tempoDownButton.setOnTouchListener { v: View?, event: MotionEvent ->
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
        binding.tempoUpButton.setOnTouchListener { v: View?, event: MotionEvent ->
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
        binding.helpOverlay.isSoundEffectsEnabled = false
        if (mCurrentPiece != null) {
            updateGUI()
        }

        if (!PrefUtils.initialHelpShown(context, PrefUtils.PREF_PROGRAM_HELP)) {
            binding.helpOverlay.visibility = View.VISIBLE
            PrefUtils.helpScreenShown(context, PrefUtils.PREF_PROGRAM_HELP)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Timber.d("onCreateOptionsMenu")
        inflater.inflate(R.menu.programmed_global_menu, menu)
        inflater.inflate(R.menu.programmed_menu, menu)
        databaseMenuItem = menu.findItem(R.id.firebase_local_database)
        updateDatabaseOptionMenuItem()
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

        PrefUtils.saveCurrentProgramToPrefs(mActivity, useFirebase,
                mCurrentPieceKey, mCurrentTempo)
        super.onPause()
    }

    override fun onResume() {
        Timber.d("onResume()")
        super.onResume()
//        createAndRegisterBroadcastReceiver()

//        if(mCurrentPiece != null) {
//            updateWearNotif();
//        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_USE_FIREBASE, useFirebase)
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
        PrefUtils.saveCurrentProgramToPrefs(mActivity, useFirebase,
                mCurrentPieceKey, mCurrentTempo)
        Timber.d("Should have just saved $mCurrentPieceKey at $mCurrentTempo BPM")
        mCursor = null
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("FRAGMENT: onActivityResult()")
        if (requestCode == FIREBASE_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            // Successfully signed in
            if (resultCode == AppCompatActivity.RESULT_OK) {
//                startActivity(SignedInActivity.createIntent(this, response));
//                finish();
                Timber.d("signed into Firebase")
            } else {
                // Sign in failed
                when {
                    response == null -> {
                        // User pressed back button
                        showToast(R.string.sign_in_cancelled)
                    }
                    response.error!!.errorCode == ErrorCodes.NO_NETWORK -> {
                        showToast(R.string.no_internet_connection)
                    }
                    response.error!!.errorCode == ErrorCodes.UNKNOWN_ERROR -> {
                        showToast(R.string.unknown_error)
                    }
                    else -> {
                        showToast(R.string.unknown_sign_in_response)
                    }
                }
                goLocal()
            }
        }
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(mActivity, R.string.return_result_problem, Toast.LENGTH_SHORT).show()
            return
        }
        if (requestCode == REQUEST_NEW_PROGRAM) {
            Timber.d("REQUEST_NEW_PROGRAM result received")
            useFirebase = PrefUtils.usingFirebase(mActivity)
            mCurrentPieceKey = data!!.getStringExtra(LoadNewProgramActivity.EXTRA_NEW_PROGRAM)
            pieceFromKey
        }
    }
    private fun showToast(message: Int) {
        val m = getString(message)
        Toast.makeText(mActivity, m, Toast.LENGTH_SHORT).show()
        Timber.d(m)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("FRAGMENT: onOptionsItemSelected()")
        return when (item.itemId) {
            R.id.firebase_local_database -> {
                useFirebase = !useFirebase
                if (useFirebase) {
                    if (mAuth!!.currentUser == null) {
                        signInToFirebase()
                    }
                }
                updateDatabaseOptionMenuItem()
                true
            }
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
    private fun signInToFirebase() {
        val providers = listOf(
            AuthUI.IdpConfig.EmailBuilder().build() //				new AuthUI.IdpConfig.PhoneBuilder().build(),
            //				new AuthUI.IdpConfig.GoogleBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                .setTheme(R.style.AppTheme_FirebaseSignIn)
                .setAvailableProviders(providers)
                .build(),
            FIREBASE_SIGN_IN
        )
    }

    private fun updateDatabaseOptionMenuItem() {
        Timber.d("updateDatabaseOptionMenuItem")
        PrefUtils.saveFirebaseStatus(mActivity, useFirebase)
        databaseMenuItem?.setTitle(if (useFirebase) R.string.use_local_database else R.string.use_cloud_database)
    }

    private fun goLocal() {
        useFirebase = false
        updateDatabaseOptionMenuItem()
    }


    private fun makeInstructionsVisible() {
        binding.helpOverlay.visibility = View.VISIBLE
    }

    private fun selectNewProgram() {
        val intent = Intent(mActivity, LoadNewProgramActivity::class.java)
                .putExtra(EXTRA_COMPOSER_NAME, mCurrentComposer)
                .putExtra(EXTRA_USE_FIREBASE, useFirebase)
        startActivityForResult(intent, REQUEST_NEW_PROGRAM)
    }

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
            mMetronome.stop()
            mMetronomeRunning = false
            binding.startStopFab.setImageResource(android.R.drawable.ic_media_play)
            binding.currentMeasureNumber.setText(R.string.double_dash_no_measure_number)
        } else {
            Timber.d("metronomeStart() %s", mCurrentPiece!!.title)
            mMetronomeRunning = true
            binding.startStopFab.setImageResource(android.R.drawable.ic_media_pause)
            mMetronome.play(mCurrentPiece!!, mCurrentTempo)
        }
    }

    override fun metronomeMeasureNumber(mm: String?) {
        binding.currentMeasureNumber.text = mm
    }

    override fun metronomeStopAndShowAd() {
        metronomeStartStop()

    }

    private fun ignoreClicks() {
        // catch and ignore click on the help screen, so other buttons aren't functional0
    }

    private fun checkKeyFormat() {
        Timber.d("Firebase: " + useFirebase + " :: key: " + mCurrentPieceKey!![0])
        if (mCurrentPieceKey!![0] == '-') {
            useFirebase = true
            PrefUtils.saveFirebaseStatus(mActivity, true)
        } else {
            useFirebase = false
            PrefUtils.saveFirebaseStatus(mActivity, false)
        }
    }

    private val pieceFromKey: Unit
        get() {
            Timber.d("getPieceFromKey() %s", mCurrentPieceKey)
            if (mCurrentPieceKey!![0] == '-') {
                pieceFromFirebase
            } else {
                if (mCursor == null) {
                    Timber.d("mCursor is null, initing loader...")
                    LoaderManager.getInstance(mActivity).initLoader(ID_PIECE_LOADER, null, this)
                } else {
                    Timber.d("mCursor exists, going straight to data")
                    pieceFromSql
                }
            }
        }

    @get:AddTrace(name = "getPieceFromFirebase", enabled = true)
    private val pieceFromFirebase: Unit
        get() {
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
        get() {
            Timber.d("getPieceFromSql()")
            val localDbId: Int = try { // TODO This needs to be handled better. This method should NOT be called unless we are in the local database. How is it being called with Firebase ids in the first place?
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
        binding.currentTempoSetting.text = mCurrentTempo.toString()
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

        if (mCurrentPiece!!.firebaseId != null) {
            CheckIfFavoriteTask().execute(mCurrentPiece!!.firebaseId)
        } else {
            CheckIfFavoriteTask().execute(mCurrentPieceKey)
        }
    }

    private fun updateGUI() {
        if (mCurrentPiece == null) {
            binding.currentProgramTitle.setText(R.string.no_composer_empty_space)
            binding.currentComposerName.setText(R.string.select_a_program)
        } else {
            binding.currentProgramTitle.text = mCurrentPiece!!.title
            binding.currentComposerName.text = mCurrentComposer
            binding.primaryBeatLengthImage.setImageResource(getNoteImageResource(mCurrentPiece!!.displayNoteValue))
            binding.primaryBeatLengthImage.contentDescription = getString(R.string.note_value_note_equals,
                    getBeatLengthContentDescription(mCurrentPiece!!.displayNoteValue))
            mCurrentTempo = mCurrentPiece!!.defaultTempo
            updateTempoView()
        }
    }

    private fun openProgramEditor() {
        val fragment = MetaDataEntryFragment.newInstance(mActivity, mCursor, useFirebase)
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
        val contentValues = Utilities.getContentValuesFromPiece(mCurrentPiece!!)
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
            mActivity.invalidateOptionsMenu()
        }

    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        Timber.d("loader creation...")
        return if (id == ID_PIECE_LOADER) {
            val queryUri = ProgramDatabaseSchema.MetProgram.CONTENT_URI
            Timber.d("Uri: %s", queryUri.toString())
            CursorLoader(mActivity,
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
        private const val FIREBASE_SIGN_IN = 456
        private const val KEY_USE_FIREBASE = "use_firebase_key"
        const val PROGRAM_ID_EXTRA = "program_id_extra_from_widget"
        fun newInstance(): Fragment {
            return ProgrammedMetronomeFragment()
        }
    }
}