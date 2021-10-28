/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.database.ComposerSelectFragment.ComposerListAdapter.ComposerViewHolder
import timber.log.Timber
import java.util.*

/**
 * This fragment gets the complete list of composer names from Firebase database, and
 * lists them. When one is selected, the name is returned to the PreprogrammedMetronomeFragment
 * for piece selection. The ComposerCallback interface is defined here, for implementation
 * by PreprogrammedMetronomeFragment, in order to communicate the selection back.
 *
 * Created by Michael on 2/26/2017.
 */
class ComposerSelectFragment : DatabaseAccessFragment() {
    //    private static final int NO_DATA_ERROR_CODE = 42;
    @BindView(R.id.composer_recycler_view)
    var mRecyclerView: RecyclerView? = null

    @BindView(R.id.composer_select_progress_bar)
    var mProgressBar: ProgressBar? = null

    @BindView(R.id.empty_data_view)
    var mErrorView: TextView? = null
    private var mAdapter: ComposerListAdapter? = null
    private var mActivity: LoadNewProgramActivity? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")
        mActivity = activity as LoadNewProgramActivity?
        Timber.d("useFirebase = %s", mActivity!!.useFirebase)
        retainInstance = true
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Timber.d("onCreateView()")
        val view = inflater.inflate(R.layout.select_composer_layout, container, false)
        ButterKnife.bind(this, view)
        mActivity!!.title = getString(R.string.select_a_composer)
        val manager = LinearLayoutManager(mActivity)
        mRecyclerView!!.layoutManager = manager
        mAdapter = ComposerListAdapter(this.context)
        mRecyclerView!!.adapter = mAdapter
        loadComposers()
        return view
    }

    /**
     * Contact Firebase Database, get all the composer's names, attach to adapter for
     * recycler viewing
     */
    private fun loadComposers() {
        Timber.d("loadComposers()")
        progressSpinner(true)
        FirebaseDatabase.getInstance().reference.child("composers")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val iterable = dataSnapshot.children
                        val list: MutableList<String> = ArrayList()
                        for (snap in iterable) {
                            snap.key?.let { list.add(it) }
                        }
                        list.sort()
                        mAdapter!!.setComposers(list)
                        progressSpinner(false)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }

    public override fun updateData() {
        Timber.d("updateData()")
        loadComposers()
    }

    /**
     * Adapter class to handle recycler view, listing composer names
     */
    internal inner class ComposerListAdapter     //            Timber.d("ComposerListAdapter constructor");
    (val mContext: Context?) : RecyclerView.Adapter<ComposerViewHolder>() {
        private var composers: List<String?>? = null
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposerViewHolder {
//            Timber.d("onCreateViewHolder");
            val item = LayoutInflater.from(mContext).inflate(R.layout.list_item_composer, parent, false)
            return ComposerViewHolder(item)
        }

        override fun onBindViewHolder(holder: ComposerViewHolder, position: Int) {
//            Timber.d("onBindViewHolder()");
            holder.composerName!!.text = composers!![position]
        }

        override fun getItemCount(): Int {
            return if (composers == null) 0 else composers!!.size
        }

        fun setComposers(list: List<String?>?) {
            composers = list
            notifyDataSetChanged()
        }

        internal inner class ComposerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            @BindView(R.id.composer_name_tv)
            var composerName: TextView? = null
            override fun onClick(v: View) {
                val position = adapterPosition

                // send selected composer name to PreprogrammedMetronomeFragment
                mActivity!!.mCurrentComposer = composers!![position]

                // close this fragment and return
                fragmentManager!!.popBackStackImmediate()
            }

            init {
                //                Timber.d("ComposerViewHolder constructor");
                ButterKnife.bind(this, itemView)
                itemView.setOnClickListener(this)
            }
        }
    }

    private fun progressSpinner(on: Boolean) {
        if (on) {
            mProgressBar!!.visibility = View.VISIBLE
        } else {
            mProgressBar!!.visibility = View.INVISIBLE
        }
    }

    companion object {
        fun newInstance(): Fragment {
            Timber.d("newInstance()")
            return ComposerSelectFragment()
        }
    }
}