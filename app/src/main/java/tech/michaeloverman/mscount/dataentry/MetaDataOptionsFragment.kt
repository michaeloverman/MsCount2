package tech.michaeloverman.mscount.dataentry

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.meta_data_options_layout.*
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.pojos.PieceOfMusic
import timber.log.Timber

/**
 * Fragment which handles UI and logic surrounding optional metadata variables of program
 * creation/editing.
 *
 * TODO: double check that it actually loads correct values when editing an existing work
 *
 * Created by Michael on 5/7/2017.
 */
class MetaDataOptionsFragment : Fragment() {
    private lateinit var mContext: Context
    private var mBuilder = PieceOfMusic.Builder()
    private var mBaselineRhythm = 0

    private lateinit var mDisplayValueAdapter: NoteValueAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.meta_data_options_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        options_cancel_button.setOnClickListener { parentFragmentManager.popBackStackImmediate() }
        save_options_button.setOnClickListener { save() }
        val manager: RecyclerView.LayoutManager = LinearLayoutManager(mContext,
            LinearLayoutManager.HORIZONTAL, false)
        display_rhythmic_value_recycler.layoutManager = manager
        mDisplayValueAdapter = NoteValueAdapter(mContext,
            resources.obtainTypedArray(R.array.note_values),
            resources.getStringArray(R.array.note_value_content_descriptions))
        display_rhythmic_value_recycler.adapter = mDisplayValueAdapter
        mDisplayValueAdapter.setSelectedPosition(mBaselineRhythm)

        // Remove soft keyboard when focus on recycler
        display_rhythmic_value_recycler.onFocusChangeListener =
            View.OnFocusChangeListener { v: View, hasFocus: Boolean ->
                if (hasFocus) {
                    val imm = v.context
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
    }

    fun save() {
        var temp = measure_offset_entry.text.toString()
        if (temp != "") {
            val offset = temp.toInt()
            mBuilder.firstMeasureNumber(offset)
        }
        temp = tempo_multiplier_entry.text.toString()
        if (temp != "") {
            val multiplier = temp.toFloat()
            mBuilder.tempoMultiplier(multiplier.toDouble())
            Timber.d("mBuilder should have 0.5 multiplier...")
        }
        val display = mDisplayValueAdapter.selectedRhythm
        mBuilder.displayNoteValue(display)
        parentFragmentManager.popBackStackImmediate()
    }

    companion object {
        fun newInstance(context: Context, builder: PieceOfMusic.Builder, baselineRhythm: Int): Fragment {
            val fragment = MetaDataOptionsFragment()
            fragment.mContext = context
            fragment.mBuilder = builder
            fragment.mBaselineRhythm = baselineRhythm
            return fragment
        }
    }
}