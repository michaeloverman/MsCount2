/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import tech.michaeloverman.mscount.utils.Metronome
import tech.michaeloverman.mscount.utils.MetronomeStartStopListener
import timber.log.Timber
import java.util.*
import kotlin.math.abs

/**
 * Normal metronome. Handle UI, input, etc. Requests clicks from a Metronome.
 *
 * Created by Michael on 2/24/2017.
 */
class NormalMetronomeFragment : Fragment(), MetronomeStartStopListener {
    private var mMetronome: Metronome? = null
    private var mMetronomeRunning = false

//    //	private WearNotification mWearNotification;
//    private var mBroadcastReceiver: BroadcastReceiver? = null

    //	private boolean mHasWearDevice;
	@BindView(R.id.normal_start_stop_fab)
    var mStartStopFab: FloatingActionButton? = null

	@BindView(R.id.current_tempo)
    var mTempoSetting: TextView? = null

	@BindView(R.id.tempo_down_button)
    var mTempoDownButton: ImageButton? = null

	@BindView(R.id.tempo_up_button)
    var mTempoUpButton: ImageButton? = null

	@BindView(R.id.add_subdivisions_fab)
    var mAddSubdivisionFAB: FloatingActionButton? = null

	@BindView(R.id.expanded_add_subdivisions_fab)
    var mExpandedAddSubFab: FloatingActionButton? = null

	@BindView(R.id.expanded_subtract_subdivisions_fab)
    var mSubtractSubFab: FloatingActionButton? = null

	@BindView(R.id.subdivision_indicator1)
    var sub1: FloatingActionButton? = null

	@BindView(R.id.subdivision_indicator2)
    var sub2: FloatingActionButton? = null

	@BindView(R.id.subdivision_indicator3)
    var sub3: FloatingActionButton? = null

	@BindView(R.id.subdivision_indicator4)
    var sub4: FloatingActionButton? = null

	@BindView(R.id.subdivision_indicator5)
    var sub5: FloatingActionButton? = null

	@BindView(R.id.subdivision_indicator6)
    var sub6: FloatingActionButton? = null

	@BindView(R.id.subdivision_indicator7)
    var sub7: FloatingActionButton? = null

	@BindView(R.id.subdivision_indicator8)
    var sub8: FloatingActionButton? = null

	@BindView(R.id.subdivision_indicator9)
    var sub9: FloatingActionButton? = null

	@BindView(R.id.subdivision_indicator10)
    var sub10: FloatingActionButton? = null

    //    @BindView(R.id.normal_adView) AdView mAdView;
	@BindView(R.id.help_overlay)
    var mInstructionsLayout: FrameLayout? = null
    private lateinit var mSubdivisionIndicators: Array<FloatingActionButton?>
    private lateinit var mSubdivisionFabColors: IntArray
    private var expandingAddFabAnim: Animation? = null
    private var expandingSubFabAnim: Animation? = null
    private var collapsingAddFabAnim: Animation? = null
    private var collapsingSubFabAnim: Animation? = null

    //    Animation fadingFabAnim, unFadingFabAnim;
    private var mBPM = 0f
    private var mWholeNumbersSelected = true
    private var mNumSubdivisions = 0
    private lateinit var mSubdivisionFloatVolumes: FloatArray
    private lateinit var mSubdivisionVolumes: IntArray
    private var mDetector: GestureDetectorCompat? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
        mMetronome = context?.let { Metronome(it) }
        mMetronome!!.setMetronomeStartStopListener(this)

//		mHasWearDevice = PrefUtils.wearPresent(getContext());
//		if (mHasWearDevice) {
//			createAndRegisterBroadcastReceiver();
//		}
        mDetector = GestureDetectorCompat(this.context, MetronomeGestureListener())
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        mNumSubdivisions = pref.getInt(PREF_KEY_SUBDIVISIONS, 1)
        mBPM = pref.getFloat(PREF_KEY_BPM, 123.4f)
        mWholeNumbersSelected = pref.getBoolean(PREF_WHOLE_NUMBERS, true)
        mSubdivisionVolumes = IntArray(MAX_SUBDIVISIONS)
        mSubdivisionFloatVolumes = FloatArray(MAX_SUBDIVISIONS)
        for (i in 0 until MAX_SUBDIVISIONS) {
            mSubdivisionVolumes[i] = 10
            mSubdivisionFloatVolumes[i] = MAX_FLOAT_VOLUME
        }
        mSubdivisionFabColors = requireContext().resources.getIntArray(R.array.subdivision_colors)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.normal_metronome_fragment, container, false)
        ButterKnife.bind(this, view)

        //        AdRequest adRequest = new AdRequest.Builder().build();
//        AdRequest.Builder adRequest = new AdRequest.Builder();
//        if (BuildConfig.DEBUG) {
//            adRequest.addTestDevice(getString(R.string.test_device_code));
//        }
//        mAdView.loadAd(adRequest.build());

        // use the "naked" listener to catch ACTION_UP (release) for resetting tempo
        // otherwise defer to GestureDetector to handle scrolling
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
        expandingAddFabAnim = AnimationUtils.loadAnimation(context, R.anim.expanding_add_fab)
        expandingSubFabAnim = AnimationUtils.loadAnimation(context, R.anim.expanding_sub_fab)
        collapsingSubFabAnim = AnimationUtils.loadAnimation(context, R.anim.collapsing_sub_fab)
        collapsingAddFabAnim = AnimationUtils.loadAnimation(context, R.anim.collapsing_add_fab)
        collapsingAddFabAnim?.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                mAddSubdivisionFAB!!.show()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        mSubdivisionIndicators = arrayOf(sub1, sub2, sub3, sub4, sub5, sub6, sub7, sub8, sub9, sub10)
        addSubdivisionVolumeChangeListeners()
        if (mNumSubdivisions > 1) {
            expandFabs()
            for (i in 1 until mNumSubdivisions) {
                mSubdivisionIndicators[i]!!.show()
            }
        }
        if (!mWholeNumbersSelected) {
            val b = view.findViewById<RadioButton>(R.id.decimals)
            b.isChecked = true
        }
        mInstructionsLayout!!.isSoundEffectsEnabled = false
        updateDisplay()
        return view
    }

    override fun onPause() {
//        if (mAdView != null) {
//            mAdView.pause();
//        }
        if (mMetronomeRunning) {
            metronomeStartStop()
        }

//		if (mWearNotification != null) {
//			mWearNotification.cancel();
//		}
//        requireActivity().unregisterReceiver(mBroadcastReceiver)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        //        if (mAdView != null) {
//            mAdView.resume();
//        }
//        createAndRegisterBroadcastReceiver()
        //		updateWearNotif();
    }

    override fun onDestroy() {
        val prefs = PreferenceManager
                .getDefaultSharedPreferences(context).edit()
        prefs.putFloat(PREF_KEY_BPM, mBPM)
        prefs.putInt(PREF_KEY_SUBDIVISIONS, mNumSubdivisions)
        prefs.putBoolean(PREF_WHOLE_NUMBERS, mWholeNumbersSelected)
        prefs.apply()

//        if (mAdView != null) {
//            mAdView.destroy();
//        }
        super.onDestroy()
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
    fun instructionsCancelled() {
        mInstructionsLayout!!.visibility = View.INVISIBLE
    }

    //	private void updateWearNotif() {
    //		if (mHasWearDevice) {
    //			mWearNotification = new WearNotification(getContext(),
    //					getString(R.string.app_name), getString(R.string.unformatted_bpm, (int) mBPM));
    //			mWearNotification.sendStartStop();
    //		}
    //	}
//    private fun createAndRegisterBroadcastReceiver() {
//        if (mBroadcastReceiver == null) {
//            mBroadcastReceiver = MetronomeBroadcastReceiver(this)
//        }
//        val filter = IntentFilter(Metronome.ACTION_METRONOME_START_STOP)
//        requireActivity().registerReceiver(mBroadcastReceiver, filter)
//    }

    @OnClick(R.id.add_subdivisions_fab, R.id.expanded_add_subdivisions_fab)
    fun addASubdivision() {
        var restart = false
        if (mMetronomeRunning) {
            metronomeStartStop()
            restart = true
        }
        if (mNumSubdivisions == 1) {
            expandFabs()
        } else if (mNumSubdivisions == MAX_SUBDIVISIONS) {
            Toast.makeText(activity, R.string.too_many_subdivisions,
                    Toast.LENGTH_SHORT).show()
            return
        }
        mSubdivisionIndicators[mNumSubdivisions]!!.show()
        mNumSubdivisions++
        if (restart) metronomeStartStop()
    }

    @OnClick(R.id.expanded_subtract_subdivisions_fab)
    fun subtractASubdivision() {
        val bundle = Bundle()
        bundle.putString("time", Calendar.getInstance().toString())
        bundle.putString("presubtract_subdivision_count", ":$mNumSubdivisions")
        mFirebaseAnalytics!!.logEvent("subtractASubdivision", bundle)
        if (mNumSubdivisions == 1) return
        var restart = false
        if (mMetronomeRunning) {
            metronomeStartStop()
            restart = true
        }
        mNumSubdivisions--
        mSubdivisionIndicators[mNumSubdivisions]!!.hide()
        if (mNumSubdivisions == 1) {
            collapseFabs()
        }
        if (restart) metronomeStartStop()
    }

    private fun expandFabs() {
        mSubtractSubFab!!.show()
        mExpandedAddSubFab!!.show()
        mAddSubdivisionFAB!!.hide()
        mExpandedAddSubFab!!.startAnimation(expandingAddFabAnim)
        mSubtractSubFab!!.startAnimation(expandingSubFabAnim)
    }

    private fun collapseFabs() {
        mExpandedAddSubFab!!.startAnimation(collapsingAddFabAnim)
        mSubtractSubFab!!.startAnimation(collapsingSubFabAnim)
        mSubtractSubFab!!.hide()
        mExpandedAddSubFab!!.hide()
    }

    @OnClick(R.id.normal_start_stop_fab)
    override fun metronomeStartStop() {
        if (mMetronomeRunning) {
            mMetronome!!.stop()
            mMetronomeRunning = false
            mStartStopFab!!.setImageResource(android.R.drawable.ic_media_play)
        } else {
            mMetronomeRunning = true
            if (mWholeNumbersSelected) {
                mMetronome!!.play(mBPM.toInt(), mNumSubdivisions)
            } else {
                mMetronome!!.play(mBPM, mNumSubdivisions)
            }
            mStartStopFab!!.setImageResource(android.R.drawable.ic_media_pause)
        }
        //		if (mHasWearDevice) mWearNotification.sendStartStop();
    }

    @OnClick(R.id.whole_numbers, R.id.decimals)
    fun onRadioButtonClicked(view: View) {
        val checked = (view as RadioButton).isChecked
        when (view.getId()) {
            R.id.whole_numbers -> if (checked) {
                mWholeNumbersSelected = true
                mTempoDownButton!!.visibility = View.GONE
                mTempoUpButton!!.visibility = View.GONE
            }
            R.id.decimals -> if (checked) {
                mWholeNumbersSelected = false
                mTempoDownButton!!.visibility = View.VISIBLE
                mTempoUpButton!!.visibility = View.VISIBLE
            }
        }
        updateDisplay()
    }

    @OnClick(R.id.tempo_down_button)
    fun onDownButtonClick() {
        changeTempo(-0.1f)
    }

    @OnClick(R.id.tempo_up_button)
    fun onUpButtonClick() {
        changeTempo(0.1f)
    }

    @OnClick(R.id.help_overlay)
    fun ignoreClicks() {
        // catch and ignore click on the help screen, so other buttons aren't functional
    }

    private fun changeTempo(tempoChange: Float) {
        mBPM += tempoChange
        if (mBPM > MAX_TEMPO_BPM_INT) mBPM = MAX_TEMPO_BPM_FLOAT else if (mBPM < MIN_TEMPO_BPM_INT) mBPM = MIN_TEMPO_BPM_FLOAT
        updateDisplay()
    }

    private fun updateDisplay() {
        if (mWholeNumbersSelected) {
            mTempoSetting?.text = mBPM.toInt().toString()
        } else {
            mTempoSetting!!.text = ((mBPM * 10).toInt().toFloat() / 10).toString()
        }
        //		updateWearNotif();
    }

    private fun addSubdivisionVolumeChangeListeners() {
//        mSubdivisionDetector = new GestureDetectorCompat[MAX_SUBDIVISIONS];
        for (i in 0 until MAX_SUBDIVISIONS) {
            //            mSubdivisionDetector[subdivisionID] = new GestureDetectorCompat(this.getContext(),
//                    new SubdivisionGestureListener(subdivisionID));
            mSubdivisionIndicators[i]!!.setOnTouchListener(object : OnTouchListener {
                var firstY = 0f
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    val action = event.action
                    when (action) {
                        MotionEvent.ACTION_DOWN -> {
                            displayVolumeSub(i)
                            firstY = event.y
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val y = event.y
                            val distanceY = y - firstY
                            firstY = y
                            changeSubdivisionVolume(i, -distanceY)
                        }
                        MotionEvent.ACTION_UP -> {
                            v.performClick()
                            updateDisplay()
                        }
                        else -> return false
                    }
                    return true
                }
            })
        }
    }

    private fun displayVolumeSub(subdiv: Int) {
        mTempoSetting!!.text = getString(R.string.vol_abbrev_colon, mSubdivisionVolumes[subdiv])
    }

    private fun changeSubdivisionVolume(id: Int, volumeChange: Float) {
        mSubdivisionFloatVolumes[id] += volumeChange
        if (mSubdivisionFloatVolumes[id] > MAX_FLOAT_VOLUME) mSubdivisionFloatVolumes[id] = MAX_FLOAT_VOLUME else if (mSubdivisionFloatVolumes[id] < MIN_FLOAT_VOLUME) mSubdivisionFloatVolumes[id] = MIN_FLOAT_VOLUME
        Timber.d("float volume measured: %f", mSubdivisionFloatVolumes[id])
        mSubdivisionVolumes[id] = (mSubdivisionFloatVolumes[id] / FLOAT_VOLUME_DIVIDER).toInt()
        mTempoSetting!!.text = getString(R.string.vol_abbrev_colon, mSubdivisionVolumes[id])
        setFabAppearance(mSubdivisionIndicators[id], mSubdivisionVolumes[id])
        mMetronome!!.setClickVolumes(mSubdivisionVolumes)
    }

    private fun setFabAppearance(fab: FloatingActionButton?, level: Int) {
        fab!!.backgroundTintList = ColorStateList.valueOf(mSubdivisionFabColors[level])
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
        private const val MAX_SUBDIVISIONS = Metronome.MAX_SUBDIVISIONS
        private const val MAX_TEMPO_BPM_INT = Metronome.MAX_TEMPO
        private const val MAX_TEMPO_BPM_FLOAT = MAX_TEMPO_BPM_INT.toFloat()
        private const val MIN_TEMPO_BPM_INT = Metronome.MIN_TEMPO
        private const val MIN_TEMPO_BPM_FLOAT = MIN_TEMPO_BPM_INT.toFloat()
        private const val MAX_FLOAT_VOLUME = 300.0f
        private const val MIN_FLOAT_VOLUME = 0.0f
        private const val FLOAT_VOLUME_DIVIDER = 30
        private const val MINIMUM_Y_FOR_FAST_CHANGE = 10f
        private const val PREF_KEY_SUBDIVISIONS = "pref_subdivisions"
        private const val PREF_KEY_BPM = "pref_bpm"
        private const val PREF_WHOLE_NUMBERS = "pref_whole_numbers"
        fun newInstance(): Fragment {
            return NormalMetronomeFragment()
        }
    }
}