/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils

import android.content.Context
import android.media.SoundPool
import android.os.CountDownTimer
import android.widget.Toast
import tech.michaeloverman.mscount.pojos.PieceOfMusic
import tech.michaeloverman.mscount.utils.ClickSounds.getSoundPool
import tech.michaeloverman.mscount.utils.PrefUtils.getDownBeatClickId
import tech.michaeloverman.mscount.utils.PrefUtils.getInnerBeatClickId
import tech.michaeloverman.mscount.utils.PrefUtils.getSubdivisionBeatClickId
import tech.michaeloverman.mscount.utils.Utilities.Companion.integerListToArray
import timber.log.Timber

/**
 * The meat and potatoes and asparagus of the app. This runs the met. Accepts the details
 * needed to click: tempo, odd-meter groupings, program, whatever.
 *
 * Created by Michael on 10/6/2016.
 */
class Metronome(private val mContext: Context) {
    /* Sounds and Such */
    private val mSoundPool: SoundPool? = getSoundPool(mContext)
    private var mDownBeatClickId = 0
    private var mInnerBeatClickId = 0
    private var mSubdivisionBeatClickId = 0
    private val mClickVolumes: FloatArray

    /* Timer, clicker variables */
    private var mTimer: CountDownTimer? = null
    private var mDelay: Long = 0
    var isRunning: Boolean
        private set

    // TODO remove suppression once properly implemented
    private var mListener: MetronomeStartStopListener? = null
    private var mPListener: ProgrammedMetronomeListener? = null

    fun setMetronomeStartStopListener(ml: MetronomeStartStopListener?) {
        mListener = ml
    }

    fun setProgrammedMetronomeListener(pl: ProgrammedMetronomeListener?) {
        mPListener = pl
    }

    /**
     * Simple metronome click: takes either int or float tempo marking, and number of
     * beats per measure, calculates delay between clicks in millis,
     * starts simple timer, and clicks at defined intervals.
     */
    fun play(tempo: Int, beats: Int) {
        if (tempo == 0) {
            Toast.makeText(mContext, "Tempo cannot be 0", Toast.LENGTH_SHORT).show()
            Timber.d("Tempo is 0 - how?")
            return
        }
        clicksFromSharedPrefs
        mDelay = (60000 / tempo).toLong()
        if (beats == 1) {
            startClicking()
        } else {
            playMeasures(beats)
        }
    }

    fun play(tempo: Float, beats: Int) {
        if (tempo == 0.0f) {
            Toast.makeText(mContext, "Tempo cannot be 0", Toast.LENGTH_SHORT).show()
            Timber.d("Tempo is 0 - how?")
            return
        }
        clicksFromSharedPrefs
        val delay = 60000f / tempo
        mDelay = delay.toLong()// as Int.toLong()
        if (beats == 1) {
            startClicking()
        } else {
            playMeasures(beats)
        }
    }

    /**
     * Simple, single click metronome start
     */
    private fun startClicking() {
        Timber.d("int delay: %s", mDelay)
        mTimer = object : CountDownTimer(TWENTY_MINUTES, mDelay) {
            override fun onTick(millisUntilFinished: Long) {
                mSoundPool!!.play(mDownBeatClickId, mClickVolumes[0], mClickVolumes[0], 1, 0, 1.0f)
            }

            override fun onFinish() {
                isRunning = false
                mListener!!.metronomeStartStop() // Do something to change UI on listener end...
            }
        }
        isRunning = true
        mTimer?.start()
    }

    /**
     * Click with number of beats metronome start
     * @param numBeats - number of beats in 'measure'
     */
    private fun playMeasures(numBeats: Int) {
//        logSubdivisionVolumes(subs);
        mTimer = object : CountDownTimer(TWENTY_MINUTES, mDelay) {
            var subCount = 0
            override fun onTick(millisUntilFinished: Long) {
                if (subCount == 0) {
                    mSoundPool!!.play(mDownBeatClickId, mClickVolumes[subCount], mClickVolumes[subCount], 1, 0, 1.0f)
                } else {
                    mSoundPool!!.play(mInnerBeatClickId, mClickVolumes[subCount], mClickVolumes[subCount], 1, 0, 1.0f)
                }
                if (++subCount == numBeats) subCount = 0
            }

            override fun onFinish() {
                isRunning = false
                mListener!!.metronomeStartStop()
            }
        }
        isRunning = true
        mTimer?.start()
    }
    //
    //    private void logSubdivisionVolumes(int subs) {
    //        StringBuilder sb = new StringBuilder("Subdivision Volumes: ");
    //        for(int i = 0; i < subs; i++) {
    //            sb.append(mClickVolumes[i]).append(", ");
    //        }
    //        Timber.d(sb.toString());
    //    }
    /**
     * Programmed click, accepts a PieceOfMusic to define changing click patterns, and a
     * tempo marking.
     * @param p - the piece/program
     * @param tempo1 - tempo of playback
     */
    fun play(p: PieceOfMusic, tempo1: Int) {
        var tempo = tempo1
        if (tempo == 0) {
            Timber.d("Big problem - tempo == 0 - how did that happen?")
            Toast.makeText(mContext, "Tempo cannot be 0", Toast.LENGTH_SHORT).show()
            return
        }
        clicksFromSharedPrefs
        Timber.d("metronome play()")
        if (p.tempoMultiplier != 0.0) {
            Timber.d("tempo multiplier!! %s", p.tempoMultiplier)
            tempo = (tempo * p.tempoMultiplier).toInt()
        }
        mDelay = (60000 / p.subdivision / tempo).toLong()
        //        Timber.d(p.toString());
        val beats = integerListToArray(p.beats)
        val downBeats = integerListToArray(p.downBeats)
        val countOffSubs = p.countOffSubdivision
        val measureNumberOffset = p.measureCountOffset
        mTimer = object : CountDownTimer(TWENTY_MINUTES, mDelay) {
            var nextClick = 0 // number of subdivisions in 'this' beat, before next click
            var count = 0 // count of subdivisions since last click
            var beatPointer = 0 // pointer to move through beats array
            var beatsPerMeasureCount = 0 // count of beats since last downbeat
            var measurePointer = 0 //pointer to move through downbeats array
            override fun onTick(millisUntilFinished: Long) {
                if (count == nextClick) {
                    if (beatsPerMeasureCount == 0) { // It's a downbeat!
                        mSoundPool!!.play(mDownBeatClickId, 1.0f, 1.0f, 1, 0, 1.0f)
                        // start counting until next downbeat
                        beatsPerMeasureCount = downBeats[measurePointer]
                        mPListener!!.metronomeMeasureNumber((measureNumberOffset + measurePointer++).toString() + "")
                    } else { // inner beat
                        mSoundPool!!.play(mInnerBeatClickId, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                    // if we've reached the end of the piece, stop the metronome.
                    if (beatPointer == beats.size - 1) {
                        stop()
                        mPListener!!.metronomeStopAndShowAd()
                    }
                    nextClick += beats[beatPointer++] // set the subdivision counter for next beat
                    beatsPerMeasureCount-- // count down one beat in the measure
                    if (measurePointer == 1) {
                        if (beatPointer >= 3 + countOffSubs) {
                            mPListener!!.metronomeMeasureNumber("GO")
                        } else if (beatPointer >= 3) {
                            mPListener!!.metronomeMeasureNumber("READY")
                        } else {
                            mPListener!!.metronomeMeasureNumber(beatPointer.toString() + "")
                        }
                    }
                }
                count++ // count one subdivision gone by...
            }

            override fun onFinish() {
                cancel()
                isRunning = false
            }
        }
        isRunning = true
        mTimer?.start()
    }

    /**
     * Method accepts tempo and groupings, loops through groupings
     * @param tempo - tempo of the playback
     * @param groupings - listing of each click length
     * @param includeSubs - whether or not to include the internal subdivision clicks
     */
    fun play(tempo: Int, groupings: List<Int>, includeSubs: Boolean) {
        clicksFromSharedPrefs
        Timber.d("play an odd-meter loop")
        val beats = integerListToArray(groupings)
        Timber.d("beat loop length: %s", beats.size)
        mDelay = (60000 / tempo).toLong()
        mTimer = object : CountDownTimer(TWENTY_MINUTES, mDelay) {
            var nextClick = 0 // number of subdivisions in 'this' beat, before next click
            var count = 0 // count of subdivisions since last click
            var beatPointer = 0 // pointer to move through beats array
            override fun onTick(millisUntilFinished: Long) {
                if (count == nextClick) {
                    if (beatPointer == beats.size || beatPointer == 0) { // It's a downbeat!
                        mSoundPool!!.play(mDownBeatClickId, 1.0f, 1.0f, 1, 0, 1.0f)
                    } else { // inner beat
                        mSoundPool!!.play(mInnerBeatClickId, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                    // if we've reached the end of the array, loop back to beginning.
                    if (beatPointer == beats.size) {
                        beatPointer = 0
                    }
                    nextClick += beats[beatPointer++] // set the subdivision counter for next beat
                } else if (includeSubs) {
                    mSoundPool!!.play(mSubdivisionBeatClickId, 1.0f, 1.0f, 1, 0, 1.0f)
                }
                count++ // count one subdivision gone by...
            }

            override fun onFinish() {
                cancel()
                isRunning = false
                mListener!!.metronomeStartStop()
            }
        }
        isRunning = true
        mTimer?.start()
    }

    fun stop() {
//        if (mTimer == null) return
//        else { mTimer.cancel()
//        isRunning = false }
        mTimer?.let {
            it.cancel()
            isRunning = false
        }
    }

    fun setClickVolumes(vols: IntArray) {
        val num = if (vols.size > MAX_SUBDIVISIONS) MAX_SUBDIVISIONS else vols.size
        for (i in 0 until num) {
            mClickVolumes[i] = vols[i] / 10.0f
        }
    }

    private val clicksFromSharedPrefs: Unit
        get() {
            mDownBeatClickId = getDownBeatClickId(mContext)
            mInnerBeatClickId = getInnerBeatClickId(mContext)
            mSubdivisionBeatClickId = getSubdivisionBeatClickId(mContext)
        }

    companion object {
        const val ACTION_METRONOME_START_STOP = "metronome_start_stop_action"
        private const val TWENTY_MINUTES = (60000 * 20).toLong()
        const val MAX_SUBDIVISIONS = 10
        const val MAX_TEMPO = 400
        const val MIN_TEMPO = 15
    }

    //    private static final Metronome instance = new Metronome();
    //
    //    public static Metronome getInstance() {
    //        return instance;
    //    }
    init {
        isRunning = false
        mClickVolumes = FloatArray(MAX_SUBDIVISIONS)
        for (i in 0 until MAX_SUBDIVISIONS) {
            mClickVolumes[i] = 1.0f
        }
    }
}