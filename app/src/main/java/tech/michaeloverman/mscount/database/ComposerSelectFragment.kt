/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.database.ComposerSelectFragment.ComposerListAdapter.ComposerViewHolder
import tech.michaeloverman.mscount.databinding.SelectComposerLayoutBinding
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

    private lateinit var mAdapter: ComposerListAdapter
    private lateinit var mActivity: LoadNewProgramActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate()")
        mActivity = activity as LoadNewProgramActivity
        Timber.d("useFirebase = ${mActivity.useFirebase}")
        retainInstance = true
        setHasOptionsMenu(true)
    }

    private var _binding: SelectComposerLayoutBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Timber.d("onCreateView()")
        _binding = SelectComposerLayoutBinding.inflate(inflater, container, false)
        mActivity.title = getString(R.string.select_a_composer)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val manager = LinearLayoutManager(mActivity)
        binding.composerRecyclerView.layoutManager = manager
        mAdapter = ComposerListAdapter(this.context)
        binding.composerRecyclerView.adapter = mAdapter
        loadComposers()
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
                        mAdapter.setComposers(list)
                        progressSpinner(false)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }

    override fun updateData() {
        Timber.d("updateData()")
        loadComposers()
    }

    /**
     * Adapter class to handle recycler view, listing composer names
     */
    internal inner class ComposerListAdapter (private val mContext: Context?) : RecyclerView.Adapter<ComposerViewHolder>() {
        private var composers: List<String?> = emptyList()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposerViewHolder {
            val item = LayoutInflater.from(mContext).inflate(R.layout.list_item_composer, parent, false)
            return ComposerViewHolder(item)
        }

        override fun onBindViewHolder(holder: ComposerViewHolder, position: Int) {
            holder.composerName.text = composers[position]
        }

        override fun getItemCount(): Int {
            return composers.size
        }

        fun setComposers(list: List<String?>) {
            composers = list
            notifyDataSetChanged()
        }

        internal inner class ComposerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var composerName: TextView = itemView.findViewById(R.id.composer_name_tv)
            override fun onClick(v: View) {
                val position = adapterPosition

                // send selected composer name to PreprogrammedMetronomeFragment
                mActivity.mCurrentComposer = composers[position]

                // close this fragment and return
                fragmentManager!!.popBackStackImmediate()
            }

            init {
                itemView.setOnClickListener(this)
            }
        }
    }

    private fun progressSpinner(on: Boolean) {
        if (on) {
            binding.composerSelectProgressBar.visibility = View.VISIBLE
        } else {
            binding.composerSelectProgressBar.visibility = View.INVISIBLE
        }
    }

    companion object {
        fun newInstance(): Fragment {
            Timber.d("newInstance()")
            return ComposerSelectFragment()
        }
    }
}