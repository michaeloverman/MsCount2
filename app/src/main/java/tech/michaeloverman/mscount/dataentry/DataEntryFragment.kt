/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.dataentry

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.perf.metrics.AddTrace
import kotlinx.android.synthetic.main.data_input_instructions.*
import kotlinx.android.synthetic.main.data_input_layout.*
import kotlinx.android.synthetic.main.data_input_single_entry_layout.view.*
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.dataentry.DataEntryFragment.DataListAdapter.DataViewHolder
import tech.michaeloverman.mscount.pojos.DataEntry
import tech.michaeloverman.mscount.pojos.PieceOfMusic
import tech.michaeloverman.mscount.utils.PrefUtils
import timber.log.Timber
import java.util.*

/**
 * This fragment handles the UI and the logic for entering program data in the Programmed
 * Metronome activity.
 *
 * Created by Michael on 3/12/2017.
 */
class DataEntryFragment : Fragment() {
    private var mBuilder: PieceOfMusic.Builder? = null

    private var mTitle: String? = null
    private var mDataList: MutableList<DataEntry> = emptyList<DataEntry>().toMutableList()
    private var mDataMultipliedBy = 1.0f
    private var mDataMultipliedListener: DataMultipliedListener? = null
    private var mMeasureNumber = 0
    private var mAdapter: DataListAdapter = DataListAdapter()
    private var mDataItemSelected = false

    interface DataMultipliedListener {
        fun dataValuesMultipliedBy(multiplier: Float)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
        mMeasureNumber = 0

        // set up measure numbers - add opening barline, if necessary
        if (mDataList.size > 0) {
            mMeasureNumber = mDataList[mDataList.size - 1].data
        } else {
            mDataList.add(DataEntry(++mMeasureNumber, BARLINE))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.data_input_layout, container, false)
//        mBarlineButton.a
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repeat_sign.setOnClickListener { repeatSignClicked() }
        one.setOnClickListener { dataEntered(one) }
        two.setOnClickListener { dataEntered(two) }
        three.setOnClickListener { dataEntered(three) }
        four.setOnClickListener { dataEntered(four) }
        five.setOnClickListener { dataEntered(five) }
        six.setOnClickListener { dataEntered(six) }
        seven.setOnClickListener { dataEntered(seven) }
        eight.setOnClickListener { dataEntered(eight) }
        nine.setOnClickListener { dataEntered(nine) }
        ten.setOnClickListener { dataEntered(ten) }
        twelve.setOnClickListener { dataEntered(twelve) }
        other.setOnClickListener { dataEntered(other) }
        barline.setOnClickListener { dataEntered(barline) }

        data_back_button.setOnClickListener { back() }
        data_save_button.setOnClickListener { saveData() }
        data_delete_button.setOnClickListener { delete() }

        help_cancel_button.setOnClickListener { instructionsCancelled() }

        data_title_view.text = mTitle
        val manager = LinearLayoutManager(activity,
            LinearLayoutManager.HORIZONTAL, false)
        entered_data_recycler_view.layoutManager = manager
//        mAdapter = DataListAdapter()
        entered_data_recycler_view.adapter = mAdapter
        mAdapter.notifyDataSetChanged()
        entered_data_recycler_view.scrollToPosition(mDataList.size - 1)

        if (!PrefUtils.initialHelpShown(context, PrefUtils.PREF_DATA_HELP)) {
            help_overlay.visibility = View.VISIBLE
            PrefUtils.helpScreenShown(context, PrefUtils.PREF_DATA_HELP)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.data_entry_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.double_data_values -> multiplyValues(2)
            R.id.halve_data_values -> divideValues(2)
            R.id.triple_data_values -> multiplyValues(3)
            R.id.third_data_values -> divideValues(3)
            R.id.help_menu_item -> {
                makeInstructionsVisible()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
        //        mBuilder.tempoMultiplier(mDataMultipliedBy);
        return true
    }

    private fun multiplyValues(multiplier: Int) {
        for (entry in mDataList!!) {
            if (!entry.isBarline) {
                entry.data = entry.data * multiplier
            }
        }
        mAdapter.notifyDataSetChanged()
        mDataMultipliedBy *= multiplier.toFloat()
    }

    private fun divideValues(divider: Int) {
        val tempList: MutableList<DataEntry> = ArrayList()
        for (entry in mDataList!!) {
            if (!entry.isBarline && entry.data % divider != 0) {
                cantDivideError()
                return
            } else {
                tempList.add(DataEntry(entry.data / divider, entry.isBarline))
            }
        }
        mDataList = tempList
        mAdapter.notifyDataSetChanged()
        mDataMultipliedBy /= divider.toFloat()
    }

    private fun cantDivideError() {
        Toast.makeText(context, R.string.cant_evenly_divide, Toast.LENGTH_SHORT).show()
    }

    private fun makeInstructionsVisible() {
        help_overlay.visibility = View.VISIBLE
    }

    private fun instructionsCancelled() {
        help_overlay.visibility = View.INVISIBLE
    }

    /**
     * Delete the last item, or the selected item
     */
    fun delete() {
        var barline = false
        var resetMeasureNumbers = false
        val itemToDelete = if (mDataItemSelected) mAdapter.selectedPosition else mDataList.size - 1
        if (itemToDelete == 0) return
        if (mDataList[itemToDelete].isBarline) {
            barline = true
            resetMeasureNumbers = mDataItemSelected
        }
        mDataList.removeAt(itemToDelete)
        if (mAdapter.selectedPosition >= mDataList.size) {
            mAdapter.selectedPosition = -1
            mDataItemSelected = false
        }
        if (barline) mMeasureNumber--
        if (resetMeasureNumbers) resetMeasureNumbers()
        mAdapter.notifyDataSetChanged()
    }

    private fun resetMeasureNumbers() {
        mMeasureNumber = 0
        for (i in mDataList.indices) {
            if (mDataList[i].isBarline) {
                mDataList[i].data = ++mMeasureNumber
            }
        }
    }

    /**
     * returns data to previous fragment, which allows for saving to Firebase, at least at this
     * point it does...
     */
    @AddTrace(name = "DataEntryFrag.saveData")
    fun saveData() {
        Timber.d("saveData()")

        // add barline to end, if not already there
        if (!mDataList[mDataList.size - 1].isBarline) {
            mDataList.add(DataEntry(++mMeasureNumber, true))
        }
        mBuilder!!.dataEntries(mDataList)
        mDataMultipliedListener!!.dataValuesMultipliedBy(mDataMultipliedBy)
        parentFragmentManager.popBackStackImmediate()
    }

    /**
     * returns to previous fragment WITHOUT saving the data
     */
    fun back() {
        if (mDataList.size == 0) {
            parentFragmentManager.popBackStackImmediate()
        } else {
            losingDataAlertDialog()
        }
    }

    private fun losingDataAlertDialog() {
        AlertDialog.Builder(context)
                .setTitle(R.string.erase_data)
                .setMessage(R.string.delete_for_sure_question)
                .setPositiveButton(R.string.leave
                ) { _: DialogInterface?, _: Int -> parentFragmentManager.popBackStackImmediate() }
                .setNegativeButton(android.R.string.cancel
                ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .show()
    }

    private fun dataEntered(view: TextView) {
        when (val value = view.text.toString()) {
            "|" -> if (!mDataItemSelected && mDataList[mDataList.size - 1].isBarline) return else addDataEntry(null, BARLINE)
            "?" -> integerDialogResponse
            else -> addDataEntry(value, BEAT)
        }
    }

    private fun addDataEntry(valueString: String?, isBarline: Boolean) {
        Timber.d("addDataEntry: %s, beatOrBarline: %s", valueString, isBarline)
        val value: Int = if (isBarline) {
            ++mMeasureNumber
        } else {
            valueString!!.toInt()
        }
        if (mDataItemSelected) {
            mDataList.add(mAdapter.selectedPosition++, DataEntry(value, isBarline))
            if (isBarline) {
                resetMeasureNumbers()
            }
        } else {
            mDataList.add(DataEntry(value, isBarline))
        }
        mAdapter.notifyDataSetChanged()
        if (mDataItemSelected) {
            entered_data_recycler_view.scrollToPosition(mAdapter.selectedPosition)
        } else {
            entered_data_recycler_view.scrollToPosition(mDataList.size - 1)
        }
    }

    // force keyboard to show automatically
    private val integerDialogResponse: Unit
        get() {
            val view = View.inflate(context, R.layout.get_integer_dialog_layout, null)
            val editText = view.findViewById<EditText>(R.id.get_integer_edittext)
            val dialog = AlertDialog.Builder(context)
                    .setTitle(R.string.enter_subdivision_value)
                    .setView(view)
                    .setPositiveButton(android.R.string.ok
                    ) { dialogInt: DialogInterface, which: Int ->
                        val entry = editText.text.toString()
                        if (entry.isEmpty()) {
                            dialogInt.cancel()
                        } else {
                            addDataEntry(editText.text.toString(), BEAT)
                        }
                    }
                    .setNegativeButton(android.R.string.cancel
                    ) { dialogInt: DialogInterface, which: Int -> dialogInt.cancel() }
                    .create()
            dialog.show()
            // force keyboard to show automatically
            dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }

    /**
     * Proceeds up the data list until finding the previous barline, copies from there back to the
     * end again.
     */
    private fun repeatSignClicked() {
        var lastIndex = mDataList.size - 1
        if (lastIndex == 0) return

        // if it's not a barline at the end, add it....
        if (!mDataList[lastIndex].isBarline) {
            mDataList.add(DataEntry(++mMeasureNumber, BARLINE))
            lastIndex++
        }
        // reverse up the list looking for the previous barline
        var i: Int = lastIndex - 1
        while (i >= 0) {
            if (mDataList[i].isBarline) break
            i--
        }
        // follow back to the end, copying beats
        ++i
        while (i < lastIndex) {
            mDataList.add(DataEntry(mDataList[i].data, BEAT))
            i++
        }

        // add barline at end
        mDataList.add(DataEntry(++mMeasureNumber, BARLINE))
        mAdapter.notifyDataSetChanged()
        entered_data_recycler_view.scrollToPosition(mDataList.size - 1)
    }

    /**
     * Adapter for displaying data as it is entered in the recycler view. Also keeps track
     * if an item is selected for editing.
     */
    inner class DataListAdapter : RecyclerView.Adapter<DataViewHolder>() {
        var selectedPosition = -1
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
            val layoutId: Int = when (viewType) {
                Companion.VIEW_TYPE_BARLINE -> R.layout.data_input_barline_layout
                Companion.VIEW_TYPE_BEAT -> R.layout.data_input_single_entry_layout
                else -> throw IllegalArgumentException("Invalid view type, value of $viewType")
            }
            val item = LayoutInflater.from(context).inflate(layoutId, parent, false)
            return DataViewHolder(item)
        }

        override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
            val data = mDataList[position].data
            holder.dataEntry.text = data.toString()
            if (mDataList[position].isBarline) {
                holder.itemView.contentDescription = getString(
                        R.string.barline_content_description, data)
            }
            if (position == selectedPosition) {
                holder.itemView.background = ContextCompat.getDrawable(activity!!,
                        R.drawable.roundcorner_accent)
            } else {
                holder.itemView.background = ContextCompat.getDrawable(activity!!,
                        R.drawable.roundcorner_parchment)
            }

            // select/deselect data items for edit
            holder.itemView.setOnClickListener { v: View? ->
                // Update views
                notifyItemChanged(selectedPosition)
                if (selectedPosition == holder.adapterPosition) {
                    selectedPosition = -1
                    mDataItemSelected = false
                } else {
                    selectedPosition = holder.adapterPosition
                    mDataItemSelected = true
                }
                notifyItemChanged(selectedPosition)
            }
        }

        override fun getItemCount(): Int {
            return mDataList.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (mDataList[position].isBarline) {
                Companion.VIEW_TYPE_BARLINE
            } else {
                Companion.VIEW_TYPE_BEAT
            }
        }

        inner class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var dataEntry: TextView = itemView.data_entry

        }
    }

    companion object {
        private const val VIEW_TYPE_BARLINE = 0
        private const val VIEW_TYPE_BEAT = 1
        private const val BARLINE = true
        private const val BEAT = false
        fun newInstance(title: String?, builder: PieceOfMusic.Builder?,
                        dml: DataMultipliedListener?): Fragment {
            return newInstance(title, builder, ArrayList(), dml)
        }

        fun newInstance(title: String?, builder: PieceOfMusic.Builder?,
                        data: MutableList<DataEntry>, dml: DataMultipliedListener?): Fragment {
            val fragment = DataEntryFragment()
            fragment.mTitle = title
            fragment.mBuilder = builder
            fragment.mDataList = data
            fragment.mDataMultipliedListener = dml
            return fragment
        }
    }
}