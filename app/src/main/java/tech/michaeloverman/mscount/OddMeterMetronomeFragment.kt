/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount

import android.content.BroadcastReceiver
import android.os.Bundle
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
    private var mMetronome: Metronome? = null
    private var mMetronomeRunning = false
    private var mBPM = 0f
    private var mMultiplier = 0

    //    private WearNotification mWearNotification;
    private var mBroadcastReceiver: BroadcastReceiver? = null

    //    private boolean mHasWearDevice;
    @BindView(R.id.oddmeter_start_stop_fab)
    var mStartStopFab: FloatingActionButton? = null

    @BindView(R.id.oddmeter_tempo_view)
    var mTempoSetting: TextView? = null

    @BindView(R.id.include_subdivisions_checkBox)
    var mSubdivisionsCheckbox: CheckBox? = null

    @BindView(R.id.extra_subdivision_buttons)
    var mOtherButtons: LinearLayout? = null

    @BindView(R.id.pulse_multiplier_view)
    var mPulseMultiplierView: TextView? = null

    //    @BindView(R.id.odd_adView) AdView mAdView;
    private var mMultiplierSelected = false
    private var mSubdivisionsList: MutableList<Int>? = null
    private var mSubdivisionViews: MutableList<View>? = null

    @BindView(R.id.subdivision_layout)
    var mSubdivisionLayout: FlexboxLayout? = null

    //    private LinearLayout mSubdivisionLayout;
    @BindView(R.id.help_overlay)
    var mInstructionsLayout: FrameLayout? = null
    private var mDetector: GestureDetectorCompat? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
        mMetronome = Metronome(requireActivity())
        mMetronome!!.setMetronomeStartStopListener(this)

//        mHasWearDevice = PrefUtils.wearPresent(getContext());
//        if(mHasWearDevice) {
//            createAndRegisterBroadcastReceiver();
//        }
        mDetector = GestureDetectorCompat(context, MetronomeGestureListener())
        mSubdivisionsList = ArrayList()
        mSubdivisionViews = ArrayList()
    }

    //    private void updateWearNotif() {
    //        if (mHasWearDevice) {
    //            mWearNotification = new WearNotification(getContext(),
    //                    getString(R.string.unformatted_bpm, (int) mBPM), getSubdivisionsListAsString());
    //            mWearNotification.sendStartStop();
    //        }
    //    }
    //    private String getSubdivisionsListAsString() {
    //        StringBuilder sb = new StringBuilder();
    //        for(Integer i : mSubdivisionsList) {
    //            sb.append(i).append("   ");
    //        }
    //        return sb.toString();
    //    }
//    private fun createAndRegisterBroadcastReceiver() {
//        if (mBroadcastReceiver == null) {
//            mBroadcastReceiver = MetronomeBroadcastReceiver(this)
//        }
//        val filter = IntentFilter(Metronome.ACTION_METRONOME_START_STOP)
//        requireActivity().registerReceiver(mBroadcastReceiver, filter)
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.oddmeter_metronome_layout, container, false)
        ButterKnife.bind(this, view)

//        AdRequest.Builder adRequest = new AdRequest.Builder();
//        if (BuildConfig.DEBUG) {
//            adRequest.addTestDevice(getString(R.string.test_device_code));
//        }
//        mAdView.loadAd(adRequest.build());

        // use the "naked" listener to catch ACTION_UP (release) to reset tempo
        // defer to GestureDetector to handle scrolling/changing tempo
        mTempoSetting!!.setOnTouchListener { v: View?, event: MotionEvent ->
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
        val subdivCheck = prefs.getBoolean(PREF_KEY_INCLUDE_SUBDIVISIONS, false)
        mSubdivisionsCheckbox!!.isChecked = subdivCheck
        val listlength = prefs.getInt(PREF_LIST_LENGTH, 0)
        for (i in 0 until listlength) {
            val subdiv = prefs.getInt(PREF_KEY_LIST + i, 2)
            mSubdivisionsList!!.add(subdiv)
            mSubdivisionViews!!.add(getNewSubdivisionView(subdiv))
        }
        mInstructionsLayout!!.isSoundEffectsEnabled = false
        updateTempoDisplay()
        return view
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("menu item caught: %s", item.title)
        return if (item.itemId == R.id.help_menu_item) {
            makeInstructionsVisible()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun makeInstructionsVisible() {
        mInstructionsLayout!!.visibility = View.VISIBLE
    }

    @OnClick(R.id.help_cancel_button)
    fun makeInstructionsInvisible() {
        mInstructionsLayout!!.visibility = View.INVISIBLE
    }

    @OnClick(R.id.one_subs_button, R.id.two_subs_button, R.id.three_subs_button, R.id.four_subs_button, R.id.five_subs_button, R.id.six_subs_button, R.id.seven_subs_button, R.id.eight_subs_button, R.id.nine_subs_button, R.id.ten_subs_button)
    fun addSubdivision(button: TextView) {
        var wasRunning = false
        if (mMetronomeRunning) {
            metronomeStartStop()
            wasRunning = true
        }
        val beat = button.text.toString().toInt()
        if (mMultiplierSelected) {
            mMultiplier = beat
            mPulseMultiplierView!!.text = getString(R.string.pulse_equals, beat)
            multiplierSelected()
            return
        }
        mSubdivisionsList!!.add(beat)
        mSubdivisionViews!!.add(getNewSubdivisionView(beat))
        if (mOtherButtons!!.isShown) mOtherButtons!!.visibility = View.GONE
        if (wasRunning) metronomeStartStop()
    }

    @OnClick(R.id.other_subs_button)
    fun addUnusualSubdivision() {
        Timber.d("add a different length of subdivision")
        if (mOtherButtons!!.isShown) {
            mOtherButtons!!.visibility = View.GONE
        } else {
            mOtherButtons!!.visibility = View.VISIBLE
        }
    }

    @OnClick(R.id.delete_button)
    fun deleteSubdivision() {
        Timber.d("remove a subdivision")
        if (mSubdivisionsList!!.size == 0) return
        var wasRunning = false
        if (mMetronomeRunning) {
            wasRunning = true
            metronomeStartStop()
        }
        mSubdivisionsList!!.removeAt(mSubdivisionsList!!.size - 1)
        mSubdivisionLayout!!.removeView(mSubdivisionViews!![mSubdivisionViews!!.size - 1])
        mSubdivisionViews!!.removeAt(mSubdivisionViews!!.size - 1)
        if (wasRunning && mSubdivisionsList!!.size > 0) metronomeStartStop()
    }

    @OnClick(R.id.pulse_multiplier_view)
    fun multiplierSelected() {
        if (mMultiplierSelected) {
            mMultiplierSelected = false
            mPulseMultiplierView!!.background = ContextCompat.getDrawable(requireActivity(), R.drawable.roundcorner_light)
            if (mOtherButtons!!.isShown) mOtherButtons!!.visibility = View.GONE
        } else {
            mPulseMultiplierView!!.background = ContextCompat.getDrawable(requireActivity(), R.drawable.roundcorner_accent)
            mMultiplierSelected = true
        }
    }

    @OnClick(R.id.help_overlay)
    fun ignoreClicks() {
        // catch and ignore click on the help screen, so other buttons aren't functional0
    }

    override fun onPause() {
        if (mMetronomeRunning) {
            metronomeStartStop()
        }
        //        if (mWearNotification != null) {
//            mWearNotification.cancel();
//        }
//        requireActivity().unregisterReceiver(mBroadcastReceiver)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
//        createAndRegisterBroadcastReceiver()
        //        updateWearNotif();
    }

    override fun onDestroy() {
        val prefs = PreferenceManager
                .getDefaultSharedPreferences(context).edit()
        prefs.putFloat(PREF_KEY_BPM, mBPM)
        prefs.putInt(PREF_KEY_MULTIPLIER, mMultiplier)
        prefs.putInt(PREF_LIST_LENGTH, mSubdivisionsList!!.size)
        prefs.putBoolean(PREF_KEY_INCLUDE_SUBDIVISIONS, mSubdivisionsCheckbox!!.isChecked)
        for (i in mSubdivisionsList!!.indices) {
            prefs.remove(PREF_KEY_LIST + i)
            prefs.putInt(PREF_KEY_LIST + i, mSubdivisionsList!![i])
        }
        prefs.apply()
        super.onDestroy()
    }

    @OnClick(R.id.include_subdivisions_checkBox)
    fun subdivisionsOnOff() {
        if (mMetronome!!.isRunning) {
            metronomeStartStop()
            metronomeStartStop()
        }
    }

    @OnClick(R.id.oddmeter_start_stop_fab)
    override fun metronomeStartStop() {
        Timber.d("Loop length: " + mSubdivisionsList!!.size + ", view size: "
                + mSubdivisionViews!!.size)
        if (mMetronomeRunning) {
            mMetronome!!.stop()
            mMetronomeRunning = false
            mStartStopFab!!.setImageResource(android.R.drawable.ic_media_play)
        } else {
            if (mSubdivisionsList!!.size == 0) {
                Toast.makeText(context, R.string.need_subdivs_to_click_subdivs,
                        Toast.LENGTH_SHORT).show()
                return
            }
            mMetronomeRunning = true
            mStartStopFab!!.setImageResource(android.R.drawable.ic_media_pause)
            mMetronome!!.play(mBPM.toInt() * mMultiplier, mSubdivisionsList!!,
                    mSubdivisionsCheckbox!!.isChecked)
        }
        //        if (mHasWearDevice) mWearNotification.sendStartStop();
    }

    private fun changeTempo(tempoChange: Float) {
        mBPM += tempoChange
        if (mBPM > MAX_TEMPO_BPM) mBPM = MAX_TEMPO_BPM else if (mBPM < MIN_TEMPO_BPM) mBPM = MIN_TEMPO_BPM
        updateTempoDisplay()
    }

    private fun updateTempoDisplay() {
        mTempoSetting?.text = mBPM.toInt().toString()
        mPulseMultiplierView!!.text = getString(R.string.pulse_equals, mMultiplier)
        //        updateWearNotif();
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
        mSubdivisionLayout!!.addView(view)
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