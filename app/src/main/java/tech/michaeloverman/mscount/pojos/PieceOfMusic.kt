/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.pojos

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import tech.michaeloverman.mscount.utils.Utilities.Companion.appendCountoff
import timber.log.Timber
import java.io.Serializable
import java.util.*

/**
 * Class to hold a program for the programmable metronome.
 *
 * Created by Michael on 2/20/2017.
 */
class PieceOfMusic : Serializable {
    var title: String? = null
    var author: String? = null
    private lateinit var mBeats: MutableList<Int>

    //        int[] allDownBeats = new int[downBeats.length + 1];
//        allDownBeats[0] = mCountOffMeasureLength;
//        System.arraycopy(downBeats, 0, allDownBeats, 1, downBeats.length);
//
//        mDownBeats = new ArrayList<>();
//        for (int i = 0; i < allDownBeats.length; i++) {
//            mDownBeats.add(allDownBeats[i]);
//        }
    lateinit var downBeats: MutableList<Int>
    var subdivision = 0
    private lateinit var mCountOff: IntArray
    private var mCountOffSubdivision = 0

    // remove suppression once thoroughly implemented
    private var mCountOffMeasureLength = 0
    var defaultTempo = 0
    var tempoMultiplier = 0.0
    var baselineNoteValue = 0
    var actualDisplayNoteValue = 0
        private set
    var measureCountOffset = 0
    private var mRawData: MutableList<DataEntry>? = null
    var firebaseId: String? = null
    var creatorId: String? = null

    constructor(title: String?) {
        Timber.d("PieceOfMusic constructor()")
        this.title = title
    }

    constructor() {}

    var countOffSubdivision: Int
        get() = if (mCountOffSubdivision == 0) subdivision else mCountOffSubdivision
        set(countOffSubdivision) {
            mCountOffSubdivision = countOffSubdivision
        }
    var displayNoteValue: Int
        get() = if (actualDisplayNoteValue == 0) baselineNoteValue else actualDisplayNoteValue
        set(displayNoteValue) {
            actualDisplayNoteValue = displayNoteValue
        }
    val beats: List<Int>
        get() = mBeats

    /**
     * Accepts array of the length of each beat, by number of primary subdivisions,
     * combines that with a generated count off measure, and saves the entire array.
     *
     * @param beats a List of all beat lengths
     */
    fun setBeats(beats: MutableList<Int>) {
//        int[] countoff = buildCountoff(mSubdivision);
//        int[] allBeats = combine(countoff, beats);
//        mBeats = new ArrayList<>();
//        for(int i = 0; i < allBeats.length; i++) {
//            mBeats.add(allBeats[i]);
//        }
//        printArray(mBeats);
        mBeats = beats
    }

    fun setRawData(data: MutableList<DataEntry>?) {
        mRawData = data
    }

    val rawData: MutableList<DataEntry>?
        get() = mRawData

    fun rawDataAsString(): String {
        if (mRawData == null) constructRawData()
        val rawString = StringBuilder()
        for (d in mRawData!!) {
            rawString.append(d)
            rawString.append("\n")
        }
        return rawString.toString()
    }

    /**
     * Uses the 'length' of first beat to generate count off measure
     */
    private fun buildCountoff() {
        mCountOffMeasureLength = COUNTOFF_LENGTH + mCountOffSubdivision - 1
        mCountOff = IntArray(mCountOffMeasureLength)
        var i: Int
        i = 0
        while (i < mCountOff.size) {
            if (i != COUNTOFF_LENGTH - 2) {
                mCountOff[i++] = subdivision
            } else {
                for (j in 0 until mCountOffSubdivision) {
                    mCountOff[i++] = subdivision / mCountOffSubdivision
                }
            }
        }
        appendCountoff(mCountOff, mBeats!!, downBeats)
    }

    fun countOffArray(): IntArray {
        return mCountOff
    }

    fun constructRawData() {
        if (mRawData != null) {
            return
        }
        mRawData = ArrayList()
        var currentBeatCount = downBeats!![0]
        for (i in 1 until downBeats!!.size) {
            mRawData?.add(DataEntry(i, true))
            for (j in 0 until downBeats!![i]) {
                if (currentBeatCount >= mBeats!!.size) {
                    mBeats!!.add(subdivision)
                }
                mRawData?.add(DataEntry(mBeats!![currentBeatCount++]!!, false))
            }
        }
        resaveToFirebase()
    }

    private fun resaveToFirebase() {
        Timber.d("SAVING FOR NEW RAW DATA!!! THIS SHOULD NOT BE HAPPENING ANYMORE")
        val mDatabase = FirebaseDatabase.getInstance()
        val mPiecesDatabaseReference = mDatabase.reference

        // Look for piece first, and if exists, get that key to update; otherwise push() to create
        // new key for new piece.
        mPiecesDatabaseReference.child("composers").child(author!!).child(title!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val key: String? = if (dataSnapshot.exists()) {
                            // update
                            dataSnapshot.value.toString()
                        } else {
                            // push to create
                            mPiecesDatabaseReference.child("pieces").push().key
                        }
                        val updates: MutableMap<String, Any?> = HashMap()
                        updates["/pieces/$key"] = this@PieceOfMusic
                        updates["/composers/$author/$title"] = key
                        mPiecesDatabaseReference.updateChildren(updates)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }

    class Builder {
        var title: String? = null
        var author: String? = null
        lateinit var beats: MutableList<Int>
        lateinit var downBeats: MutableList<Int>
        lateinit var rawData: MutableList<DataEntry>
        var subdivision = 0
        var countOffSubdivision = 0
        var defaultTempo = 0
        var tempoMultiplier = 0.0
        var baselineNoteValue = 0
        var displayNoteValue = 0
        var measureCountOffset = 0
        var firebaseId: String? = null
        var creatorId: String? = null
        fun title(title: String?): Builder {
            this.title = title
            return this
        }

        fun author(author: String?): Builder {
            this.author = author
            return this
        }

        fun beats(beats: MutableList<Int>): Builder {
            this.beats = beats
            return this
        }

        //        public Builder beats(int[] beats) {
        //            return beats(Utilities.arrayToIntegerList(beats));
        //        }
        fun downBeats(downBeats: MutableList<Int>): Builder {
            this.downBeats = downBeats
            return this
        }

        //        public Builder downBeats(int[] downBeats) {
        //            return downBeats(Utilities.arrayToIntegerList(downBeats));
        //        }
        fun subdivision(sub: Int): Builder {
            subdivision = sub
            return this
        }

        fun countOffSubdivision(coSub: Int): Builder {
            countOffSubdivision = coSub
            return this
        }

        fun defaultTempo(tempo: Int): Builder {
            defaultTempo = tempo
            return this
        }

        fun tempoMultiplier(mult: Double): Builder {
            tempoMultiplier = mult
            return this
        }

        fun baselineNoteValue(value: Int): Builder {
            baselineNoteValue = value
            return this
        }

        fun displayNoteValue(value: Int): Builder {
            displayNoteValue = value
            return this
        }

        fun firstMeasureNumber(offset: Int): Builder {
            measureCountOffset = offset - 1
            return this
        }

        fun dataEntries(data: MutableList<DataEntry>): Builder {
            rawData = data
            beats = ArrayList()
            downBeats = ArrayList()
            val start = if (data[0].isBarline) 1 else 0
            var beatsPerMeasureCount = 0
            for (i in start until data.size) {
                if (data[i].isBarline) {
                    downBeats?.add(beatsPerMeasureCount)
                    beatsPerMeasureCount = 0
                } else {
                    beats?.add(data[i].data)
                    beatsPerMeasureCount++
                }
            }
            return this
        }

        fun dataEntries(data: String): Builder {
            val entries = data.split("\\n").toTypedArray()
            val list: MutableList<DataEntry> = ArrayList()
            for (entry1 in entries) {
                val entry = entry1.split(";").toTypedArray()
                list.add(DataEntry(entry[0].toInt(), java.lang.Boolean.parseBoolean(entry[1])))
            }
            return dataEntries(list)
        }

        fun hasData(): Boolean {
            return rawData != null
        }

        fun firebaseId(id: String?): Builder {
            firebaseId = id
            return this
        }

        fun creatorId(id: String?): Builder {
            creatorId = id
            return this
        }

        fun build(): PieceOfMusic {
            return PieceOfMusic(this)
        }
    }

    private constructor(builder: Builder) {
        title = builder.title
        author = builder.author
        mBeats = builder.beats
        downBeats = builder.downBeats
        subdivision = builder.subdivision
        mCountOffSubdivision = builder.countOffSubdivision
        measureCountOffset = builder.measureCountOffset
        defaultTempo = if (builder.defaultTempo == 0) DEFAULT_DEFAULT_TEMPO else builder.defaultTempo
        tempoMultiplier = if (builder.tempoMultiplier == 0.0) 1.0 else builder.tempoMultiplier
        baselineNoteValue = if (builder.baselineNoteValue == 0) QUARTER else builder.baselineNoteValue
        actualDisplayNoteValue = builder.displayNoteValue
        mRawData = builder.rawData
        firebaseId = builder.firebaseId
        creatorId = builder.creatorId
        buildCountoff()
    }

    companion object {
        private const val COUNTOFF_LENGTH = 4
        const val SIXTEENTH = 1
        const val DOTTED_SIXTEENTH = 5
        const val EIGHTH = 2
        const val DOTTED_EIGHTH = 3
        const val QUARTER = 4
        const val DOTTED_QUARTER = 6
        const val HALF = 8
        const val DOTTED_HALF = 12
        const val WHOLE = 16
        private const val DEFAULT_DEFAULT_TEMPO = 120
    }
}