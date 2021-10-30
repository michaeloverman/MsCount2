/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database

import android.content.Context
import android.database.Cursor
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_work.view.*
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.database.WorksListAdapter.WorksViewHolder
import tech.michaeloverman.mscount.pojos.TitleKeyObject
import tech.michaeloverman.mscount.utils.AlphanumComparator
import timber.log.Timber
import java.util.*

/**
 * Adapter for supplying recycler view with proper data.
 * Created by Michael on 2/25/2017.
 */
class WorksListAdapter     //        mTitles = titles;
(private val mContext: Context, private val mClickHandler: WorksListAdapterOnClickHandler) : RecyclerView.Adapter<WorksViewHolder>() {
    private var mTitles: List<TitleKeyObject> = emptyList()

    interface WorksListAdapterOnClickHandler {
        fun onClick(key: String?, title: String?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorksViewHolder {
//        Timber.d("onCreateViewHolder()");
        val item = LayoutInflater.from(mContext).inflate(R.layout.list_item_work, parent, false)
        return WorksViewHolder(item)
    }

    override fun onBindViewHolder(holder: WorksViewHolder, position: Int) {
//        Timber.d("onBindViewHolder()");
        holder.title.text = mTitles[position].title
        holder.title.setHorizontallyScrolling(true)
        holder.title.ellipsize = TextUtils.TruncateAt.MIDDLE
        //        ViewCompat.setTransitionName(holder.title, "titleViewTrans" + position);
    }

    override fun getItemCount(): Int {
        return mTitles.size
    }

    fun setTitles(titles: List<TitleKeyObject>) {
        mTitles = titles
        Collections.sort(mTitles, AlphanumComparator())
        Timber.d("setTitles() - %s titles...", mTitles.size)
        notifyDataSetChanged()
    }

    fun newCursor(data: Cursor?) {
        val titles: MutableList<TitleKeyObject> = ArrayList()
        if (data == null) {
            Timber.d("Null cursor...")
            mTitles = titles
            return
        }
        Timber.d("newCursor() data.getCount() == %s", data.count)
        try {
            data.moveToFirst()
            while (!data.isAfterLast) {
                titles.add(TitleKeyObject(
                        data.getString(ProgramDatabaseSchema.MetProgram.POSITION_TITLE),
                        data.getString(ProgramDatabaseSchema.MetProgram.POSITION_ID)))
                data.moveToNext()
            }
        } catch (exception: Exception) {
            Timber.d("Problem here: check this out...")
        }
        setTitles(titles)
    }

    inner class WorksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var title: TextView = itemView.work_title
        override fun onClick(v: View) {
            Timber.d("WorksViewHolder onClick()")
            val position = adapterPosition
            val key = mTitles[position].key
            val title = mTitles[position].title
            mClickHandler.onClick(key, title)
        }

        init {
            //            Timber.d("WorksViewHolder constructor()");
            itemView.setOnClickListener(this)
        }
    }
}