/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.dataentry

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.perf.metrics.AddTrace
import kotlinx.android.synthetic.main.meta_data_input_instructions.*
import kotlinx.android.synthetic.main.meta_data_input_layout.*
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.database.LoadNewProgramActivity
import tech.michaeloverman.mscount.database.ProgramDatabaseSchema
import tech.michaeloverman.mscount.dataentry.DataEntryFragment.Companion.newInstance
import tech.michaeloverman.mscount.dataentry.DataEntryFragment.DataMultipliedListener
import tech.michaeloverman.mscount.pojos.DataEntry
import tech.michaeloverman.mscount.pojos.PieceOfMusic
import tech.michaeloverman.mscount.programmed.ProgrammedMetronomeActivity
import tech.michaeloverman.mscount.utils.Metronome
import tech.michaeloverman.mscount.utils.Utilities.Companion.getContentValuesFromPiece
import timber.log.Timber
import java.util.*

/**
 * This fragment manages the UI and handles the logic for all metadata surrounding a program,
 * such as title, composer, foundational data, etc. Opens MetaDataOptionsFragment for optional
 * items of metadata.
 */
class MetaDataEntryFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor?>, DataMultipliedListener {

    private var mBaselineRhythmicValueAdapter: NoteValueAdapter? = null
    private var mTemporaryBaselineRhythm = 4
    private lateinit var mPieceOfMusic: PieceOfMusic
    private lateinit var mBuilder: PieceOfMusic.Builder
    private var mCurrentPieceKey: String? = null
    private var mFirebaseId: String? = null
    private var mDataEntries: MutableList<DataEntry>? = null
    private var mDataMultiplier = 1.0f
    private val mDownBeats: List<Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")
        retainInstance = true
        setHasOptionsMenu(true)
        mBuilder = PieceOfMusic.Builder()
        mPieceOfMusic = PieceOfMusic()
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
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.metadata_entry_menu, menu)
        //        menu.removeItem(R.id.create_new_program_option);
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("onCreateView()")

        return inflater.inflate(R.layout.meta_data_input_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        enter_beats_button.setOnClickListener { enterBeatsClicked() }
        options_button.setOnClickListener { optionsButtonClicked() }
        help_cancel_button.setOnClickListener { instructionsCancelled() }
        save_program_button.setOnClickListener { saveProgram() }
        // When a countoff value is entered, make sure it is an even divisor of the baseline subdivisions
        countoff_subdivision_entry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
//                try {
                val temp = baseline_subdivision_entry.text.toString()
                val primary = if (temp == "") 0 else temp.toInt()
                val countoff = if (s.toString() == "") 1 else s.toString().toInt()
                if (primary % countoff != 0) {
                    Toast.makeText(context,
                        R.string.countoff_must_fit_subdivisions,
                        Toast.LENGTH_SHORT).show()
                }
                //                } catch (NumberFormatException n) {
//                    // not used: only integers can be entered
//                }
            }
        })

        // When default tempo is entered, make sure it is in the metronome's range
        default_tempo_entry.onFocusChangeListener =
            View.OnFocusChangeListener { v: View?, hasFocus: Boolean ->
                if (!hasFocus) {
                    try {
                        val tempo = default_tempo_entry.text.toString().toInt()
                        if (tempo < Metronome.MIN_TEMPO || tempo > Metronome.MAX_TEMPO) {
                            Toast.makeText(
                                context, getString(
                                    R.string.tempo_between_min_max,
                                    Metronome.MIN_TEMPO, Metronome.MAX_TEMPO
                                ), Toast.LENGTH_SHORT
                            )
                                .show()
                            default_tempo_entry.setText("")
                        }
                    } catch (n: NumberFormatException) {
                        Toast.makeText(context, R.string.tempo_must_be_integer, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

        val manager: RecyclerView.LayoutManager = LinearLayoutManager(mActivity,
            LinearLayoutManager.HORIZONTAL, false)
        baseline_rhythmic_value_recycler.layoutManager = manager
        mBaselineRhythmicValueAdapter = NoteValueAdapter(mActivity,
            resources.obtainTypedArray(R.array.note_values),
            resources.getStringArray(R.array.note_value_content_descriptions))
        baseline_rhythmic_value_recycler.adapter = mBaselineRhythmicValueAdapter
        mBaselineRhythmicValueAdapter!!.setSelectedPosition(mTemporaryBaselineRhythm)

        // Remove soft keyboard when display on recycler
        baseline_rhythmic_value_recycler.onFocusChangeListener =
            View.OnFocusChangeListener { v: View, hasFocus: Boolean ->
                if (hasFocus) {
                    val imm = v.context
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        if (mDataMultiplier != 1.0f) {
            var currentBaseline = Integer.valueOf(baseline_subdivision_entry.text.toString())
            currentBaseline *= mDataMultiplier.toInt()
            baseline_subdivision_entry.setText(currentBaseline.toString())
            mDataMultiplier = 1.0f
        }
    }

    private fun enterBeatsClicked() {
        Timber.d("enterBeatsClicked()")
        val title = title_text_entry.text.toString()
        if (title == "") {
            toastError()
            return
        }
        mBuilder!!.title(title)
        mTemporaryBaselineRhythm = mBaselineRhythmicValueAdapter!!.selectedRhythm
        //        Timber.d("mTemporaryBaselineRhythm" + mTemporaryBaselineRhythm);
        gotoDataEntryFragment(title)
    }

    private fun gotoDataEntryFragment(title: String) {
        val fragment: Fragment = if (mDataEntries == null) {
            newInstance(title, mBuilder, this)
        } else {
            newInstance(title, mBuilder, mDataEntries!!, this)
        }
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun optionsButtonClicked() {
        mTemporaryBaselineRhythm = mBaselineRhythmicValueAdapter!!.selectedRhythm
        val fragment = MetaDataOptionsFragment.newInstance(mActivity,
                mBuilder, mTemporaryBaselineRhythm)
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit_existing_program_option -> {
                loadProgram()
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
        help_overlay.visibility = View.VISIBLE
    }

    private fun instructionsCancelled() {
        help_overlay.visibility = View.INVISIBLE
    }

    private fun loadProgram() {
        val intent = Intent(mActivity, LoadNewProgramActivity::class.java)
        startActivityForResult(intent, REQUEST_NEW_PROGRAM)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(mActivity, R.string.problem_loading, Toast.LENGTH_SHORT).show()
            return
        }
        if (requestCode == REQUEST_NEW_PROGRAM) {
            getPieceFromKey(data!!.getStringExtra(LoadNewProgramActivity.EXTRA_NEW_PROGRAM))
        }
    }

    private fun getPieceFromKey(key: String?) {
        mCurrentPieceKey = key
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

    private val pieceFromFirebase: Unit
        get() {
            Timber.d("getPieceFromFirebase()")
            FirebaseDatabase.getInstance().reference.child("pieces").child(mCurrentPieceKey!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            mPieceOfMusic = dataSnapshot.getValue(PieceOfMusic::class.java) ?: PieceOfMusic()
                            updateVariables()
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Toast.makeText(context, getString(R.string.database_error_try_again),
                                    Toast.LENGTH_SHORT).show()
                        }
                    })
        }

    //        mCursor.close();
    private val pieceFromSql: Unit
        get() {
            Timber.d("getPieceFromSql()")
            val localDbId = mCurrentPieceKey!!.toInt()
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
            //        mCursor.close();
            mPieceOfMusic = builder.build()
            updateVariables()
        }

    private fun programNotFoundError(id: Int) {
        Toast.makeText(mActivity, getString(R.string.id_not_found, id),
                Toast.LENGTH_SHORT).show()
    }

    private fun updateVariables() {
        if (mPieceOfMusic.rawData == null) {
            mPieceOfMusic.constructRawData()
        }
        mFirebaseId = mPieceOfMusic.firebaseId
        updateGUI()
    }

    private fun updateGUI() {
        Timber.d("updating the GUI")
        val names = mPieceOfMusic.author!!.split(", ").toTypedArray()
        if (names.size >= 2) {
            composer_last_name_text_entry.setText(names[0])
            composer_first_name_text_entry.setText(names[1])
        } else if (names.size == 1) {
            composer_last_name_text_entry.setText(mPieceOfMusic.author)
            composer_first_name_text_entry.setText("")
        } else {
            composer_last_name_text_entry.setText("")
            composer_first_name_text_entry.setText("")
        }
        title_text_entry.setText(mPieceOfMusic.title)
        baseline_subdivision_entry.setText(java.lang.String.valueOf(mPieceOfMusic.subdivision))
        countoff_subdivision_entry.setText(mPieceOfMusic.countOffSubdivision.toString())
        default_tempo_entry.setText(java.lang.String.valueOf(mPieceOfMusic.defaultTempo))
        mBaselineRhythmicValueAdapter!!.setSelectedPosition(mPieceOfMusic.baselineNoteValue)
        mBaselineRhythmicValueAdapter!!.notifyDataSetChanged()
        mDataEntries = mPieceOfMusic.rawData
    }

    override fun dataValuesMultipliedBy(multiplier: Float) {
        mDataMultiplier *= multiplier
    }

    private fun saveProgram() {
        Timber.d("checking data entries")
        if (!validateDataEntries()) return

        // get all the metadata fields
        Timber.d("checking metadata entries")
        if (!validateMetaDataEntries()) return
        Timber.d("building rest of mBuilder")
        mBuilder.firebaseId(mFirebaseId)
        mBuilder.creatorId(firebaseAuthId)
        mPieceOfMusic = mBuilder.build()
        if (mActivity.useFirebase) {
            checkFirebaseForExistingData() // beginning of method chain to save to cloud
        } else {
            saveToSqlDatabase()
        }
    }

    private val firebaseAuthId: String
        get() {
            val auth = FirebaseAuth.getInstance()
            return auth.currentUser!!.uid
        }

    private fun validateMetaDataEntries(): Boolean {
        val composerLast = composer_last_name_text_entry.text.toString()
        val composerFirst = composer_first_name_text_entry.text.toString()
        val title = title_text_entry.text.toString()
        val subd = baseline_subdivision_entry.text.toString()
        val countoff = countoff_subdivision_entry.text.toString()
        val defaultTempo = default_tempo_entry.text.toString()
        val rhythm = mBaselineRhythmicValueAdapter!!.selectedRhythm

        // Check for null entries...
        if (composerLast == "") {
            Toast.makeText(context, R.string.error_no_composer_message,
                    Toast.LENGTH_SHORT).show()
            composer_last_name_text_entry.requestFocus()
            return false
        }
        if (composerFirst == "") {
            Toast.makeText(context, R.string.error_no_composer_message,
                    Toast.LENGTH_SHORT).show()
            composer_first_name_text_entry.requestFocus()
            return false
        }
        if (title == "") {
            Toast.makeText(context, R.string.error_no_title_message,
                    Toast.LENGTH_SHORT).show()
            title_text_entry.requestFocus()
            return false
        }
        if (subd == "") {
            Toast.makeText(context, R.string.error_no_subdivision_message,
                    Toast.LENGTH_SHORT).show()
            baseline_subdivision_entry.requestFocus()
            return false
        }
        if (countoff == "") {
            Toast.makeText(context, R.string.error_no_countoff_message,
                    Toast.LENGTH_SHORT).show()
            countoff_subdivision_entry.requestFocus()
            return false
        }

        // Check for valid data input...
        val subdInt: Int
        val countoffInt: Int
        try {
            subdInt = subd.toInt()
            if (subdInt < 1 || subdInt > 24) {
                Toast.makeText(context, R.string.subdivs_out_of_range, Toast.LENGTH_SHORT).show()
            } else if (subdInt > 12) {
                //TODO dialog box to confirm unusually large baseline subdivision
            }
        } catch (nfe: NumberFormatException) {
            Toast.makeText(context, R.string.enter_only_number_subdivs, Toast.LENGTH_SHORT).show()
            baseline_subdivision_entry.requestFocus()
            return false
        }
        try {
            countoffInt = countoff.toInt()
            if (countoffInt > subdInt || countoffInt == 0) {
                Toast.makeText(context, R.string.countoff_must_be_even_divisor,
                        Toast.LENGTH_SHORT).show()
                countoff_subdivision_entry.requestFocus()
                return false
            }
        } catch (nfe: NumberFormatException) {
            Toast.makeText(context, R.string.please_enter_only_numbers_countoff,
                    Toast.LENGTH_SHORT).show()
            baseline_subdivision_entry.requestFocus()
            return false
        }
        val composer = composerLast.trim { it <= ' ' } + ", " + composerFirst.trim { it <= ' ' }
        mBuilder!!.author(composer)
                .title(title)
                .subdivision(subdInt)
                .countOffSubdivision(countoffInt)
        val tempoInt: Int
        try {
            if (defaultTempo != "") {
                tempoInt = defaultTempo.toInt()
                if (tempoInt < 15 || tempoInt > 250) {
                    Toast.makeText(context, R.string.tempo_between_min_max, Toast.LENGTH_SHORT).show()
                    return false
                }
                mBuilder!!.defaultTempo(tempoInt)
            }
        } catch (nfe: NumberFormatException) {
            Timber.d("You should not be here: should not be able to enter anything but numbers, and any numbers entered have already been checked for range.")
        }
        mBuilder!!.baselineNoteValue(rhythm)
        return true
    }

    private fun validateDataEntries(): Boolean {
        return if (mBuilder!!.hasData()) {
            true
        } else {
            Toast.makeText(context, R.string.enter_data_before_saving,
                    Toast.LENGTH_SHORT).show()
            false
        }
        //        mBuilder.dataEntries(mDataEntries);
    }

    @AddTrace(name = "saveToSqlDatabase")
    private fun saveToSqlDatabase() {
        Timber.d("this where it should be saving to sql")
        val contentValues = getContentValuesFromPiece(mPieceOfMusic!!)
        val resolver = context?.contentResolver
        val returnUri = resolver?.insert(ProgramDatabaseSchema.MetProgram.CONTENT_URI, contentValues)
        if (returnUri != null) {
            parentFragmentManager.popBackStackImmediate()
        } else {
            databaseSaveErrorStayHere()
        }
    }

    private fun checkFirebaseForExistingData() {
        val database = FirebaseDatabase.getInstance()
        val databaseReference = database.reference

        // Look for piece first, and if exists, get that key to update; otherwise push() to create
        // new key for new piece.
        val composer = mPieceOfMusic!!.author
        val title = mPieceOfMusic!!.title
        databaseReference.child("composers").child(composer!!).child(title!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            val key = dataSnapshot.value.toString()
                            checkIfAuthorizedCreator(key)
                        } else {
                            saveToFirebase(mPieceOfMusic)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(context, R.string.database_error_save_cancelled,
                                Toast.LENGTH_SHORT).show()
                    }
                })
    }

    private fun checkIfAuthorizedCreator(key: String) {
        FirebaseDatabase.getInstance().reference.child("pieces").child(key)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val pieceFromFirebase = dataSnapshot.getValue(PieceOfMusic::class.java)
                        if (pieceFromFirebase!!.creatorId == mPieceOfMusic.creatorId) {
                            overwriteFirebaseDataAlertDialog(
                                mPieceOfMusic.title,
                                    mPieceOfMusic.author)
                        } else {
                            Toast.makeText(mActivity, R.string.not_authorized_save_local,
                                    Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }

    /**
     * Called prior to saving, if piece by same title already exists in database. If confirmed,
     * overwrites data, if canceled, does nothing.
     *
     * @param title - title of the piece
     * @param composer - composer of the piece
     */
    private fun overwriteFirebaseDataAlertDialog(title: String?, composer: String?) {
        val dialog = AlertDialog.Builder(mActivity)
        dialog.setCancelable(false)
        dialog.setTitle("Overwrite Data?")
        dialog.setMessage(getString(R.string.overwrite_data_confirmation, title, composer))
        dialog.setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int -> saveToFirebase(mPieceOfMusic) }
                .setNegativeButton(R.string.cancel) { dialogInt: DialogInterface, _: Int -> dialogInt.dismiss() }
        val alert = dialog.create()
        alert.show()
    }

    @AddTrace(name = "saveToFirebase")
    private fun saveToFirebase(p: PieceOfMusic?) {
        Timber.d("Saving to local database, or to Firebase: " + p!!.title + " by " + p.author)
        //        Timber.d("Pieces is " + p.getDownBeats().size() + " measures long.");
        val mDatabase = FirebaseDatabase.getInstance()
        val mPiecesDatabaseReference = mDatabase.reference

        // Look for piece first, and if exists, get that key to update; otherwise push() to create
        // new key for new piece.
        mPiecesDatabaseReference.child("composers").child(p.author!!).child(p.title!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val key: String? = if (dataSnapshot.exists()) {
                            // update
                            dataSnapshot.value.toString()
                        } else {
                            // push to create
                            mPiecesDatabaseReference.child("pieces").push().key
                        }
                        if (p.firebaseId == null) {
                            p.firebaseId = key
                        }
                        val updates: MutableMap<String, Any?> = HashMap()
                        updates["/pieces/$key"] = p
                        updates["/composers/" + p.author + "/" + p.title] = key
                        mPiecesDatabaseReference.updateChildren(updates)
                        fragmentManager!!.popBackStackImmediate()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        databaseSaveErrorStayHere()
                    }
                })
    }

    private fun databaseSaveErrorStayHere() {
        Toast.makeText(context, R.string.error_save_canceled,
                Toast.LENGTH_SHORT).show()
    }

    private fun toastError() {
        Toast.makeText(this.context, R.string.enter_composer_name_and_title,
                Toast.LENGTH_SHORT).show()
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
            mPieceOfMusic = PieceOfMusic()
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
        private const val REQUEST_NEW_PROGRAM = 1984
        private lateinit var mActivity: ProgrammedMetronomeActivity
        private const val ID_PIECE_LOADER = 435
        private var mCursor: Cursor? = null
        fun newInstance(a: ProgrammedMetronomeActivity, c: Cursor?): Fragment {
            Timber.d("newInstance()")
            mActivity = a
            mCursor = c
            return MetaDataEntryFragment()
        }
    }
}