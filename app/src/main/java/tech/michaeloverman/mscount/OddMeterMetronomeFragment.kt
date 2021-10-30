/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount

import android.os.Bundle
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.odd_metronome_instructions.*
import kotlinx.android.synthetic.main.oddmeter_metronome_layout.*
import tech.michaeloverman.mscount.utils.Metronome
import tech.michaeloverman.mscount.utils.MetronomeStartStopListener
import timber.log.Timber
import java.util.*
import kotlin.math.abs

/**
 * Odd-Meter metronome fragment. Handles UI, input, etc
 * Created by Michael on 3/14/2017.
 */
class OddMeterMetronomeFragment : Fragment(), MetronomeStartStopListener {
    private lateinit var mMetronome: Metronome
    private var mMetronomeRunning = false
    private var mBPM = 0f
    private var mMultiplier = 0

    private var mMultiplierSelected = false
    private var mSubdivisionsList: MutableList<Int>? = null
    private var mSubdivisionViews: MutableList<View>? = null
    private var subdivCheck: Boolean = false

    private var mDetector: GestureDetectorCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
        mMetronome = Metronome(requireContext())
        mMetronome.setMetronomeStartStopListener(this)

        mDetector = GestureDetectorCompat(context, MetronomeGestureListener())
        mSubdivisionsList = ArrayList()
        mSubdivisionViews = ArrayList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.oddmeter_metronome_layout, container, false)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        help_cancel_button.setOnClickListener { help_overlay.visibility = View.INVISIBLE }

        one_subs_button.setOnClickListener { addSubdivision(one_subs_button.text.toString()) }
        two_subs_button.setOnClickListener { addSubdivision(two_subs_button.text.toString()) }
        three_subs_button.setOnClickListener { addSubdivision(three_subs_button.text.toString()) }
        four_subs_button.setOnClickListener { addSubdivision(four_subs_button.text.toString()) }
        five_subs_button.setOnClickListener { addSubdivision(five_subs_button.text.toString()) }
        six_subs_button.setOnClickListener { addSubdivision(six_subs_button.text.toString()) }
        seven_subs_button.setOnClickListener { addSubdivision(seven_subs_button.text.toString()) }
        eight_subs_button.setOnClickListener { addSubdivision(eight_subs_button.text.toString()) }
        nine_subs_button.setOnClickListener { addSubdivision(nine_subs_button.text.toString()) }
        ten_subs_button.setOnClickListener { addSubdivision(ten_subs_button.text.toString()) }
        other_subs_button.setOnClickListener {
            extra_subdivision_buttons.visibility = if (extra_subdivision_buttons.isShown) View.GONE
            else View.VISIBLE
        }
        delete_button.setOnClickListener { deleteSubdivision() }

        pulse_multiplier_view.setOnClickListener { multiplierSelected() }
        include_subdivisions_checkBox.setOnClickListener { subdivisionsOnOff() }
        include_subdivisions_checkBox.isChecked = subdivCheck

        oddmeter_start_stop_fab.setOnClickListener { metronomeStartStop() }

        // use the "naked" listener to catch ACTION_UP (release) to reset tempo
        // defer to GestureDetector to handle scrolling/changing tempo
        oddmeter_tempo_view.setOnTouchListener { _: View?, event: MotionEvent ->
            val action = event.action
            if (action == MotionEvent.ACTION_UP) {
                if (mMetronomeRunning) {
                    // stop the met
                    metronomeStartStop()
                    // restart at new tempo
                    metronomeStartStop()
                }
            } else {
                mDetector!!.onTouchEvent(event)
            }
            true
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        mBPM = prefs.getFloat(PREF_KEY_BPM, 120f)
        mMultiplier = prefs.getInt(PREF_KEY_MULTIPLIER, 2)
        subdivCheck = prefs.getBoolean(PREF_KEY_INCLUDE_SUBDIVISIONS, false)
        val listlength = prefs.getInt(PREF_LIST_LENGTH, 0)
        for (i in 0 until listlength) {
            val subdiv = prefs.getInt(PREF_KEY_LIST + i, 2)
            mSubdivisionsList!!.add(subdiv)
            mSubdivisionViews!!.add(getNewSubdivisionView(subdiv))
        }
        help_overlay.isSoundEffectsEnabled = false
        updateTempoDisplay()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("menu item caught: %s", item.title)
        return if (item.itemId == R.id.help_menu_item) {
            help_overlay.visibility = View.VISIBLE
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun addSubdivision(button: String) {
        var wasRunning = false
        if (mMetronomeRunning) {
            metronomeStartStop()
            wasRunning = true
        }
        val beat = button.toInt()
        if (mMultiplierSelected) {
            mMultiplier = beat
            pulse_multiplier_view.text = getString(R.string.pulse_equals, beat)
            multiplierSelected()
            return
        }
        mSubdivisionsList!!.add(beat)
        mSubdivisionViews!!.add(getNewSubdivisionView(beat))
        if (extra_subdivision_buttons.isShown) extra_subdivision_buttons.visibility = View.GONE
        if (wasRunning) metronomeStartStop()
    }

    private fun deleteSubdivision() {
        Timber.d("remove a subdivision")
        if (mSubdivisionsList!!.size == 0) return
        var wasRunning = false
        if (mMetronomeRunning) {
            wasRunning = true
            metronomeStartStop()
        }
        mSubdivisionsList!!.removeAt(mSubdivisionsList!!.size - 1)
        subdivision_layout.removeView(mSubdivisionViews!![mSubdivisionViews!!.size - 1])
        mSubdivisionViews!!.removeAt(mSubdivisionViews!!.size - 1)
        if (wasRunning && mSubdivisionsList!!.size > 0) metronomeStartStop()
    }

    private fun multiplierSelected() {
        if (mMultiplierSelected) {
            mMultiplierSelected = false
            pulse_multiplier_view.background = ContextCompat.getDrawable(requireActivity(), R.drawable.roundcorner_light)
            if (extra_subdivision_buttons.isShown) extra_subdivision_buttons.visibility = View.GONE
        } else {
            pulse_multiplier_view.background = ContextCompat.getDrawable(requireActivity(), R.drawable.roundcorner_accent)
            mMultiplierSelected = true
        }
    }

    override fun onPause() {
        if (mMetronomeRunning) {
            metronomeStartStop()
        }
        super.onPause()
    }

    override fun onDestroy() {
        val prefs = PreferenceManager
                .getDefaultSharedPreferences(context).edit()
        prefs.putFloat(PREF_KEY_BPM, mBPM)
        prefs.putInt(PREF_KEY_MULTIPLIER, mMultiplier)
        prefs.putInt(PREF_LIST_LENGTH, mSubdivisionsList!!.size)
        prefs.putBoolean(PREF_KEY_INCLUDE_SUBDIVISIONS, subdivCheck)
        for (i in mSubdivisionsList!!.indices) {
            prefs.remove(PREF_KEY_LIST + i)
            prefs.putInt(PREF_KEY_LIST + i, mSubdivisionsList!![i])
        }
        prefs.apply()
        super.onDestroy()
    }

    private fun subdivisionsOnOff() {
        subdivCheck = include_subdivisions_checkBox.isChecked
        if (mMetronome.isRunning) {
            metronomeStartStop()
            metronomeStartStop()
        }
    }

    override fun metronomeStartStop() {
        Timber.d("Loop length: " + mSubdivisionsList!!.size + ", view size: "
                + mSubdivisionViews!!.size)
        if (mMetronomeRunning) {
            mMetronome.stop()
            mMetronomeRunning = false
            oddmeter_start_stop_fab.setImageResource(android.R.drawable.ic_media_play)
        } else {
            if (mSubdivisionsList!!.size == 0) {
                Toast.makeText(context, R.string.need_subdivs_to_click_subdivs,
                        Toast.LENGTH_SHORT).show()
                return
            }
            mMetronomeRunning = true
            oddmeter_start_stop_fab.setImageResource(android.R.drawable.ic_media_pause)
            mMetronome.play(mBPM.toInt() * mMultiplier, mSubdivisionsList!!,
                subdivCheck)
        }
    }

    private fun changeTempo(tempoChange: Float) {
        mBPM += tempoChange
        if (mBPM > MAX_TEMPO_BPM) mBPM = MAX_TEMPO_BPM else if (mBPM < MIN_TEMPO_BPM) mBPM = MIN_TEMPO_BPM
        updateTempoDisplay()
    }

    private fun updateTempoDisplay() {
        oddmeter_tempo_view.text = mBPM.toInt().toString()
        pulse_multiplier_view.text = getString(R.string.pulse_equals, mMultiplier)
    }

    private fun getNewSubdivisionView(value: Int): View {
        val view = TextView(context)
        view.text = value.toString()
        view.textSize = SUBDIVISION_DISPLAY_SIZE
        view.background = ContextCompat.getDrawable(requireActivity(),
                R.drawable.roundcorner_parchment)
        view.setPadding(MARGIN, 0, MARGIN, 0)
        val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(MARGIN, MARGIN, MARGIN, MARGIN)
        view.layoutParams = params
        subdivision_layout.addView(view)
        return view
    }

    internal inner class MetronomeGestureListener : SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (abs(distanceY) > MINIMUM_Y_FOR_FAST_CHANGE) {
                changeTempo(distanceY / 10)
            } else {
                changeTempo(-distanceX / 100)
            }
            return true
        }
    }

    companion object {
        private const val MAX_TEMPO_BPM = Metronome.MAX_TEMPO.toFloat()
        private const val MIN_TEMPO_BPM = Metronome.MIN_TEMPO.toFloat()
        private const val SUBDIVISION_DISPLAY_SIZE = 40f
        private const val MARGIN = 8
        private const val MINIMUM_Y_FOR_FAST_CHANGE = 10f
        private const val PREF_KEY_BPM = "pref_key_oddmeter_bpm"
        private const val PREF_LIST_LENGTH = "pref_key_oddmeter_subdivision_list"
        private const val PREF_KEY_LIST = "oddmeter_subdivision_"
        private const val PREF_KEY_MULTIPLIER = "oddmeter_multiplier"
        private const val PREF_KEY_INCLUDE_SUBDIVISIONS = "include_subdivisions_checkbox"
        fun newInstance(): Fragment {
            return OddMeterMetronomeFragment()
        }
    }
}