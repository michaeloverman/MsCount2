/* Copyright (C) 2021 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.database.Cursor
import android.os.AsyncTask
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.database.WorksListAdapter.WorksListAdapterOnClickHandler
import tech.michaeloverman.mscount.databinding.ProgramSelectFragmentBinding
import tech.michaeloverman.mscount.pojos.TitleKeyObject
import timber.log.Timber
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [PieceSelectFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 * Handles display and click handling of programs in the database.
 */
class PieceSelectFragment : DatabaseAccessFragment(), WorksListAdapterOnClickHandler, LoaderManager.LoaderCallbacks<Cursor?> {

    private var mDeleteCancelMenuItem: MenuItem? = null
    private var mCurrentComposer: String? = null
    private lateinit var mAdapter: WorksListAdapter
    private var mDeleteFlag = false
    private lateinit var mActivity: LoadNewProgramActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
        mActivity = activity as LoadNewProgramActivity
    }

    private var _binding: ProgramSelectFragmentBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = ProgramSelectFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.selectProgramInclude.selectComposerButton.setOnClickListener { selectComposer() }
        binding.composersNameLabel.setOnClickListener { selectComposer() }

        val manager = LinearLayoutManager(this.activity)
        binding.selectProgramInclude.pieceListRecyclerView.layoutManager = manager
        mAdapter = WorksListAdapter(requireContext(), this)
        binding.selectProgramInclude.pieceListRecyclerView.adapter = mAdapter

        mCurrentComposer = mActivity.mCurrentComposer
        Timber.d("onCreate() Composer: %s", mCurrentComposer)
        if (mActivity.useFirebase) {
            mActivity.title = getString(R.string.select_piece_by)
        } else {
            mActivity.title = getString(R.string.select_a_piece)
            makeComposerRelatedViewsInvisible()
        }
        if (mActivity.useFirebase && mCurrentComposer == null) {
            selectComposer()
        } else {
            composerSelected()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Timber.d("onCreateOptionsMenu")
        inflater.inflate(R.menu.delete_menu_item, menu)
        super.onCreateOptionsMenu(menu, inflater)
        mDeleteCancelMenuItem = menu.findItem(R.id.delete_program_menu_item)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("Fragment menu option")
        return if (item.itemId == R.id.delete_program_menu_item) {
            if (!mDeleteFlag) {
                toastDeleteInstructions()
                prepareProgramDelete()
            } else {
                cleanUpProgramDelete()
            }
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun toastDeleteInstructions() {
        Toast.makeText(mActivity, R.string.select_to_delete, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("AlwaysShowAction")
    private fun prepareProgramDelete() {
        mDeleteFlag = true
        mDeleteCancelMenuItem!!.setTitle(R.string.cancel_delete)
        mDeleteCancelMenuItem!!.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
    }

    private fun cleanUpProgramDelete() {
        mDeleteFlag = false
        mDeleteCancelMenuItem!!.setTitle(R.string.delete_program)
        mDeleteCancelMenuItem!!.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        mAdapter.notifyDataSetChanged()
        progressSpinner(false)
    }

    fun selectComposer() {
        val fragment = ComposerSelectFragment.newInstance()

//        android.transition.ChangeBounds changeBounds = (android.transition.ChangeBounds) TransitionInflater.from(mActivity).inflateTransition(R.transition.change_bounds);
//        fragment.setSharedElementEnterTransition(changeBounds);
        activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container, fragment)
                ?.addToBackStack(null) //                .addSharedElement(mComposersNameView, getString(R.string.transition_composer_name_view))
                ?.commit()
    }

    override fun onClick(pieceId: String?, title: String?) {
        Timber.d("ProgramSelect onClick() pieceId: %s", pieceId)
        progressSpinner(true)
        if (!mDeleteFlag) {
            mActivity.setProgramResult(pieceId)
            mActivity.finish()
        } else {
            dialogDeleteConfirmation(pieceId, title)
        }
    }

    private fun dialogDeleteConfirmation(pieceId: String?, title: String?) {
        val dialog = AlertDialog.Builder(mActivity)
        dialog.setCancelable(false)
                .setTitle(R.string.delete_program_dialog_title)
                .setMessage(getString(R.string.delete_confirmation_question, title))
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                    if (mActivity.useFirebase) {
                        checkFirebaseAuthorizationToDelete(pieceId, title)
                    } else {
                        deletePieceFromSql(pieceId, title)
                    }
                }
                .setNegativeButton(R.string.cancel) { dialogInt: DialogInterface?, which: Int -> cleanUpProgramDelete() }
        dialog.create().show()
    }

    private fun deletePieceFromSql(id: String?, title: String?) {
        Toast.makeText(mActivity, R.string.delete_from_sql_toast, Toast.LENGTH_SHORT).show()
        val idInt: Int? = try {
            id?.toInt()
        } catch (numE: NumberFormatException) {
            Timber.d(getString(R.string.incorrect_format_database_id))
            return
        }
        DeleteFromSqlTask(idInt, title).execute()
        cleanUpProgramDelete()
    }

    private fun checkFirebaseAuthorizationToDelete(id: String?, title: String?) {
        val userId = firebaseAuthId
        if (id != null) {
            FirebaseDatabase.getInstance().reference.child("pieces").child(id)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            Timber.d("userId: %s", userId)
                            Timber.d("creatorId: %s", dataSnapshot.child("creatorId"))
                            if (dataSnapshot.child("creatorId").value == userId) {
                                completeAuthorizedFirebaseDelete(id, title)
                            } else {
                                Toast.makeText(mActivity, R.string.not_authorized_to_delete_toast,
                                        Toast.LENGTH_SHORT).show()
                                cleanUpProgramDelete()
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
        }
    }

    private val firebaseAuthId: String
        get() {
            val auth = FirebaseAuth.getInstance()
            return auth.currentUser!!.uid
        }

    private fun completeAuthorizedFirebaseDelete(id: String?, title: String?) {
        Toast.makeText(mActivity, R.string.delete_from_firebase_toast, Toast.LENGTH_SHORT).show()
        //TODO delete from Firebase....
        // delete from composers
        if (title != null && id != null) {
            FirebaseDatabase.getInstance().reference.child("composers").child(mCurrentComposer!!)
                    .child(title).removeValue { _: DatabaseError?, _: DatabaseReference? -> Timber.d("%s deleted from cloud database...", title) }
            // delete from pieces
            FirebaseDatabase.getInstance().reference.child("pieces").child(id).removeValue { _: DatabaseError?, _: DatabaseReference? ->
                cleanUpProgramDelete()
                selectComposer()
            }
        }
    }

    private fun composerSelected() {
        progressSpinner(true)
        Timber.d("composerSelected() - %s", mCurrentComposer)
        if (mActivity!!.useFirebase) {
            Timber.d("Checking Firebase for composer %s", mCurrentComposer)
            FirebaseDatabase.getInstance().reference.child("composers").child(mCurrentComposer!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val pieceList = dataSnapshot.children
                            val list = ArrayList<TitleKeyObject>()
                            for (snap in pieceList) {
                                list.add(TitleKeyObject(snap.key!!, snap.value.toString()))
                            }
                            mAdapter.setTitles(list)
                            binding.composersNameLabel.text = mCurrentComposer
                            progressSpinner(false)
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
        } else {
            Timber.d("Checking SQL for pieces ")
            LoaderManager.getInstance(mActivity!!).initLoader(ID_PROGRAM_LOADER, null, this)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        return if (id == ID_PROGRAM_LOADER) {
            val queryUri = ProgramDatabaseSchema.MetProgram.CONTENT_URI
            Timber.d("onCreateLoader() queryUri: %s", queryUri)
            val sortOrder = ProgramDatabaseSchema.MetProgram.COLUMN_TITLE + " ASC"
            CursorLoader(mActivity!!,
                    queryUri,
                    null,
                    null,
                    null,
                    sortOrder)
        } else {
            throw RuntimeException("Loader Not Implemented: $id")
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor?>, data: Cursor?) {
        Timber.d("onLoadFinished - cursor data ready")
        progressSpinner(false)
        when {
            data == null -> {
                Timber.d("data == null")
                binding.selectProgramInclude.errorView.visibility = View.VISIBLE
                updateEmptyProgramList()
            }
            data.count == 0 -> {
                Timber.d("data.getCount() == 0")
                binding.selectProgramInclude.errorView.visibility = View.VISIBLE
                updateEmptyProgramList()
            }
            else -> {
                binding.selectProgramInclude.errorView.visibility = View.GONE
                mAdapter.newCursor(data)
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor?>) {
        mAdapter.newCursor(null)
    }

    override fun updateData() {
        Timber.d("updateData()")
        if (mActivity.useFirebase) {
            makeComposerRelatedViewsVisible()
            selectComposer()
        } else {
            mCurrentComposer = null
            makeComposerRelatedViewsInvisible()
            composerSelected()
        }
    }

    private fun makeComposerRelatedViewsVisible() {
        Timber.d("showing views")
        binding.selectProgramInclude.selectComposerButton.visibility = View.VISIBLE
        binding.composersNameLabel.text = mCurrentComposer
        if (mDeleteCancelMenuItem != null) mDeleteCancelMenuItem!!.isEnabled = true
        binding.localDatabaseLabel.visibility = View.INVISIBLE
        mActivity.title = getString(R.string.select_piece_by)
    }

    private fun makeComposerRelatedViewsInvisible() {
        Timber.d("removing views")
        binding.composersNameLabel.visibility = View.INVISIBLE
        binding.localDatabaseLabel.visibility = View.VISIBLE
        binding.selectProgramInclude.selectComposerButton.visibility = View.GONE
        mActivity.title = getString(R.string.select_a_piece)
    }

    private fun updateEmptyProgramList() {
        binding.selectProgramInclude.errorView.text = getString(R.string.no_programs_currently_in_database)
        binding.selectProgramInclude.errorView.contentDescription = getString(R.string.no_programs_currently_in_database)
        if (mDeleteCancelMenuItem != null) mDeleteCancelMenuItem!!.isEnabled = false
    }

    private fun progressSpinner(on: Boolean) {
        if (on) {
            binding.composersNameLabel.visibility = View.INVISIBLE
            binding.selectProgramInclude.programSelectProgressBar.visibility = View.VISIBLE
        } else {
            binding.composersNameLabel.visibility = View.VISIBLE
            binding.selectProgramInclude.programSelectProgressBar.visibility = View.INVISIBLE
        }
    }

    internal inner class DeleteFromSqlTask(private val _id: Int?, private val mTitle: String?) : AsyncTask<Void?, Void?, Void?>() {
        private val dialog = ProgressDialog(mActivity)
        override fun doInBackground(vararg params: Void?): Void? {
            val uri = ProgramDatabaseSchema.MetProgram.CONTENT_URI
            val whereClause = "_id=?"
            val args = arrayOf(_id?.toString())
            mActivity.contentResolver.delete(uri, whereClause, args)
            return null
        }

        override fun onPreExecute() {
            dialog.setMessage(getString(R.string.deleting_title, mTitle))
            dialog.show()
        }

        override fun onPostExecute(aVoid: Void?) {
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }
    }

    companion object {
        private const val ID_PROGRAM_LOADER = 432
        fun newInstance(): Fragment {
            return PieceSelectFragment()
        }
    }
}