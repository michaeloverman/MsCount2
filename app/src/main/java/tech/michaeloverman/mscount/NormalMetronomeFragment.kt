/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import tech.michaeloverman.mscount.databinding.NormalMetronomeFragmentBinding
import tech.michaeloverman.mscount.utils.Metronome
import tech.michaeloverman.mscount.utils.MetronomeStartStopListener
import tech.michaeloverman.mscount.utils.PrefUtils
import timber.log.Timber
import java.util.*
import kotlin.math.abs

/**
 * Normal metronome. Handle UI, input, etc. Requests clicks from a Metronome.
 *
 * Created by Michael on 2/24/2017.
 */
class NormalMetronomeFragment : Fragment(), MetronomeStartStopListener {
    private lateinit var mMetronome: Metronome
    private var mMetronomeRunning = false

    private lateinit var mSubdivisionIndicators: Array<FloatingActionButton?>
    private lateinit var mSubdivisionFabColors: IntArray

    //    Animation fadingFabAnim, unFadingFabAnim;
    private var expandingAddFabAnim: Animation? = null
    private var expandingSubFabAnim: Animation? = null
    private var collapsingAddFabAnim: Animation? = null
    private var collapsingSubFabAnim: Animation? = null

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
        mMetronome = Metronome(requireContext())
        mMetronome.setMetronomeStartStopListener(this)

        mDetector = GestureDetectorCompat(this.context, MetronomeGestureListener() )

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

        setHasOptionsMenu(true)
    }

//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//        inflater.inflate(R.menu.normal_menu, menu)
//    }

    private var _binding: NormalMetronomeFragmentBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = NormalMetronomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.normalStartStopFab.setOnClickListener { metronomeStartStop() }
        binding.tempoDownButton.setOnClickListener { changeTempo(-0.1f) }
        binding.tempoUpButton.setOnClickListener { changeTempo(0.1f) }
        binding.addSubdivisionsFab.setOnClickListener { addASubdivision() }
        binding.expandedAddSubdivisionsFab.setOnClickListener { addASubdivision() }
        binding.expandedSubtractSubdivisionsFab.setOnClickListener { subtractASubdivision() }
        binding.wholeNumbers.setOnClickListener { onWholeRadioButtonClicked() }
        binding.decimals.setOnClickListener { onDecimalRadioButtonClicked() }

        binding.overlayInclude.helpCancelButton.setOnClickListener { binding.helpOverlay.visibility = View.INVISIBLE }
        binding.helpOverlay.setOnClickListener { ignoreClicks() }

        // use the "naked" listener to catch ACTION_UP (release) for resetting tempo
        // otherwise defer to GestureDetector to handle scrolling
        binding.currentTempo.setOnTouchListener { v: View?, event: MotionEvent ->
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
        collapsingAddFabAnim?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                binding.addSubdivisionsFab.show()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        mSubdivisionIndicators = arrayOf(binding.subdivisionIndicator1, binding.subdivisionIndicator2,
            binding.subdivisionIndicator3, binding.subdivisionIndicator4, binding.subdivisionIndicator5,
            binding.subdivisionIndicator6, binding.subdivisionIndicator7, binding.subdivisionIndicator8,
            binding.subdivisionIndicator9, binding.subdivisionIndicator10)
        addSubdivisionVolumeChangeListeners()
        if (mNumSubdivisions > 1) {
            expandFabs()
            (1 until mNumSubdivisions).forEach { i ->
                mSubdivisionIndicators[i]?.show()
            }
        }

        if (!mWholeNumbersSelected) binding.decimals.isChecked = true

        binding.helpOverlay.isSoundEffectsEnabled = false

        updateDisplay()

        if (!PrefUtils.initialHelpShown(context, PrefUtils.PREF_NORMAL_HELP)) {
            binding.helpOverlay.visibility = View.VISIBLE
            PrefUtils.helpScreenShown(context, PrefUtils.PREF_NORMAL_HELP)
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
        prefs.putInt(PREF_KEY_SUBDIVISIONS, mNumSubdivisions)
        prefs.putBoolean(PREF_WHOLE_NUMBERS, mWholeNumbersSelected)
        prefs.apply()

        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("menu item caught: %s", item.title)
        return if (item.itemId == R.id.help_menu_item) {
            binding.helpOverlay.visibility = View.VISIBLE
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun addASubdivision() {
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

    private fun subtractASubdivision() {
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
        binding.expandedSubtractSubdivisionsFab.show()
        binding.expandedAddSubdivisionsFab.show()
        binding.addSubdivisionsFab.hide()
        binding.expandedAddSubdivisionsFab.startAnimation(expandingAddFabAnim)
        binding.expandedSubtractSubdivisionsFab.startAnimation(expandingSubFabAnim)
    }

    private fun collapseFabs() {
        binding.expandedAddSubdivisionsFab.startAnimation(collapsingAddFabAnim)
        binding.expandedSubtractSubdivisionsFab.startAnimation(collapsingSubFabAnim)
        binding.expandedSubtractSubdivisionsFab.hide()
        binding.expandedAddSubdivisionsFab.hide()
    }

    override fun metronomeStartStop() {
        if (mMetronomeRunning) {
            mMetronome.stop()
            mMetronomeRunning = false
            binding.normalStartStopFab.setImageResource(android.R.drawable.ic_media_play)
        } else {
            mMetronomeRunning = true
            if (mWholeNumbersSelected) {
                mMetronome.play(mBPM.toInt(), mNumSubdivisions)
            } else {
                mMetronome.play(mBPM, mNumSubdivisions)
            }
            binding.normalStartStopFab.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    private fun onDecimalRadioButtonClicked() {
        if (binding.decimals.isChecked) {
            mWholeNumbersSelected = false
            binding.tempoDownButton.visibility = View.VISIBLE
            binding.tempoUpButton.visibility = View.VISIBLE
        }
        updateDisplay()
    }

    private fun onWholeRadioButtonClicked() {
        if (binding.wholeNumbers.isChecked) {
            mWholeNumbersSelected = true
            binding.tempoDownButton.visibility = View.GONE
            binding.tempoUpButton.visibility = View.GONE
        }
        updateDisplay()
    }

    private fun ignoreClicks() {
        // catch and ignore click on the help screen, so other buttons aren't functional
    }

    private fun changeTempo(tempoChange: Float) {
        mBPM += tempoChange
        if (mBPM > MAX_TEMPO_BPM_INT) mBPM = MAX_TEMPO_BPM_FLOAT else if (mBPM < MIN_TEMPO_BPM_INT) mBPM = MIN_TEMPO_BPM_FLOAT
        updateDisplay()
    }

    private fun updateDisplay() {
        if (mWholeNumbersSelected) {
            binding.currentTempo.text = mBPM.toInt().toString()
        } else {
            binding.currentTempo.text = ((mBPM * 10).toInt().toFloat() / 10).toString()
        }
    }

    private fun addSubdivisionVolumeChangeListeners() {
//        mSubdivisionDetector = new GestureDetectorCompat[MAX_SUBDIVISIONS];
        (0 until MAX_SUBDIVISIONS).forEach { i ->
            //            mSubdivisionDetector[subdivisionID] = new GestureDetectorCompat(this.getContext(),
//                    new SubdivisionGestureListener(subdivisionID));
            mSubdivisionIndicators[i]?.setOnTouchListener(object : OnTouchListener {
                var firstY = 0f
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
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
        binding.currentTempo.text = getString(R.string.vol_abbrev_colon, mSubdivisionVolumes[subdiv])
    }

    private fun changeSubdivisionVolume(id: Int, volumeChange: Float) {
        mSubdivisionFloatVolumes[id] += volumeChange
        if (mSubdivisionFloatVolumes[id] > MAX_FLOAT_VOLUME) mSubdivisionFloatVolumes[id] = MAX_FLOAT_VOLUME else if (mSubdivisionFloatVolumes[id] < MIN_FLOAT_VOLUME) mSubdivisionFloatVolumes[id] = MIN_FLOAT_VOLUME
        Timber.d("float volume measured: %f", mSubdivisionFloatVolumes[id])
        mSubdivisionVolumes[id] = (mSubdivisionFloatVolumes[id] / FLOAT_VOLUME_DIVIDER).toInt()
        binding.currentTempo.text = getString(R.string.vol_abbrev_colon, mSubdivisionVolumes[id])
        setFabAppearance(mSubdivisionIndicators[id], mSubdivisionVolumes[id])
        mMetronome.setClickVolumes(mSubdivisionVolumes)
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