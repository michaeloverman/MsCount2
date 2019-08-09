/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.pojos;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.michaeloverman.mscount.utils.Utilities;
import timber.log.Timber;

/**
 * Class to hold a program for the programmable metronome.
 *
 * Created by Michael on 2/20/2017.
 */

public class PieceOfMusic implements Serializable {

    private static final int COUNTOFF_LENGTH = 4;
    public static final int SIXTEENTH = 1;
    public static final int DOTTED_SIXTEENTH = 5;
    public static final int EIGHTH = 2;
    public static final int DOTTED_EIGHTH = 3;
    public static final int QUARTER = 4;
    public static final int DOTTED_QUARTER = 6;
    public static final int HALF = 8;
    public static final int DOTTED_HALF = 12;
    public static final int WHOLE = 16;
    private static final int DEFAULT_DEFAULT_TEMPO = 120;

    private String mTitle;
    private String mAuthor;
    private List<Integer> mBeats;
    private List<Integer> mDownBeats;
    private int mSubdivision;
    private int[] mCountOff;
    private int mCountOffSubdivision;
    @SuppressWarnings("FieldCanBeLocal") // remove suppression once thoroughly implemented
    private int mCountOffMeasureLength;
    private int mDefaultTempo;
    private double mTempoMultiplier;
    private int mBaselineNoteValue;
    private int mDisplayNoteValue;
    private int mMeasureCountOffset;
    private List<DataEntry> mRawData;
    private String mFirebaseId;
    private String mCreatorId;

    public PieceOfMusic(String title) {
        Timber.d("PieceOfMusic constructor()");
        mTitle = title;
    }

    public PieceOfMusic() {
    }

    public String getCreatorId() {
        return mCreatorId;
    }

    public void setCreatorId(String id) {
        mCreatorId = id;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String author) {
        mAuthor = author;
    }

    public int getSubdivision() {
        return mSubdivision;
    }

    public void setSubdivision(int subdivision) {
        mSubdivision = subdivision;
    }

    public int getCountOffSubdivision() {
        return mCountOffSubdivision == 0 ? mSubdivision : mCountOffSubdivision;
    }

    public void setCountOffSubdivision(int countOffSubdivision) {
        mCountOffSubdivision = countOffSubdivision;
    }

    public int getDefaultTempo() {
        return mDefaultTempo;
    }

    public void setDefaultTempo(int defaultTempo) {
        mDefaultTempo = defaultTempo;
    }

    public double getTempoMultiplier() {
        return mTempoMultiplier;
    }

    public void setTempoMultiplier(double tempoMultiplier) {
        mTempoMultiplier = tempoMultiplier;
    }

    public int getBaselineNoteValue() {
        return mBaselineNoteValue;
    }

    public void setBaselineNoteValue(int baselineNoteValue) {
        mBaselineNoteValue = baselineNoteValue;
    }

    public int getDisplayNoteValue() {
        return mDisplayNoteValue == 0 ? mBaselineNoteValue : mDisplayNoteValue;
    }

    public int getActualDisplayNoteValue() {
        return mDisplayNoteValue;
    }

    public void setDisplayNoteValue(int displayNoteValue) {
        mDisplayNoteValue = displayNoteValue;
    }

    public int getMeasureCountOffset() {
        return mMeasureCountOffset;
    }

    public void setMeasureCountOffset(int measureCountOffset) {
        mMeasureCountOffset = measureCountOffset;
    }

    public List<Integer> getBeats() {
        return mBeats;
    }

    public String getFirebaseId() {
        return mFirebaseId;
    }

    public void setFirebaseId(String id) {
        mFirebaseId = id;
    }

    /**
     * Accepts array of the length of each beat, by number of primary subdivisions,
     * combines that with a generated count off measure, and saves the entire array.
     *
     * @param beats a List of all beat lengths
     */
    public void setBeats(List<Integer> beats) {
//        int[] countoff = buildCountoff(mSubdivision);
//        int[] allBeats = combine(countoff, beats);
//        mBeats = new ArrayList<>();
//        for(int i = 0; i < allBeats.length; i++) {
//            mBeats.add(allBeats[i]);
//        }
//        printArray(mBeats);
        mBeats = beats;
    }

    public void setRawData(List<DataEntry> data) {

        mRawData = data;
    }

    public List<DataEntry> getRawData() {
        return mRawData;
    }

    public String rawDataAsString() {
        if (mRawData == null) constructRawData();
        StringBuilder rawString = new StringBuilder();
        for (DataEntry d : mRawData) {
            rawString.append(d);
            rawString.append("\n");
        }
        return rawString.toString();
    }

    /**
     * Uses the 'length' of first beat to generate count off measure
     */
    private void buildCountoff() {
        mCountOffMeasureLength = COUNTOFF_LENGTH + mCountOffSubdivision - 1;
        mCountOff = new int[mCountOffMeasureLength];
        int i;
        for (i = 0; i < mCountOff.length; ) {
            if (i != COUNTOFF_LENGTH - 2) {
                mCountOff[i++] = mSubdivision;
            } else {
                for (int j = 0; j < mCountOffSubdivision; j++) {
                    mCountOff[i++] = mSubdivision / mCountOffSubdivision;
                }
            }
        }
        Utilities.appendCountoff(mCountOff, mBeats, mDownBeats);
    }

    @SuppressWarnings("unused")
    public int[] countOffArray() {
        return mCountOff;
    }


    public List<Integer> getDownBeats() {
        return mDownBeats;
    }

    @SuppressWarnings("unused")
    public void setDownBeats(List<Integer> downBeats) {
//        int[] allDownBeats = new int[downBeats.length + 1];
//        allDownBeats[0] = mCountOffMeasureLength;
//        System.arraycopy(downBeats, 0, allDownBeats, 1, downBeats.length);
//
//        mDownBeats = new ArrayList<>();
//        for (int i = 0; i < allDownBeats.length; i++) {
//            mDownBeats.add(allDownBeats[i]);
//        }
        mDownBeats = downBeats;
    }

    public void constructRawData() {
        if (mRawData != null) {
            return;
        }
        mRawData = new ArrayList<>();
        int currentBeatCount = mDownBeats.get(0);
        for (int i = 1; i < mDownBeats.size(); i++) {
            mRawData.add(new DataEntry(i, true));
            for (int j = 0; j < mDownBeats.get(i); j++) {
                if (currentBeatCount >= mBeats.size()) {
                    mBeats.add(mSubdivision);
                }
                mRawData.add(new DataEntry(mBeats.get(currentBeatCount++), false));
            }
        }

        resaveToFirebase();
    }

    private void resaveToFirebase() {
        Timber.d("SAVING FOR NEW RAW DATA!!! THIS SHOULD NOT BE HAPPENING ANYMORE");
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference mPiecesDatabaseReference = mDatabase.getReference();

        // Look for piece first, and if exists, get that key to update; otherwise push() to create
        // new key for new piece.
        mPiecesDatabaseReference.child("composers").child(this.getAuthor()).child(this.getTitle())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                        String key;
                        if (dataSnapshot.exists()) {
                            // update
                            key = dataSnapshot.getValue().toString();
                        } else {
                            // push to create
                            key = mPiecesDatabaseReference.child("pieces").push().getKey();
                        }

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("/pieces/" + key, PieceOfMusic.this);
                        updates.put("/composers/" + getAuthor() + "/" + getTitle(), key);
                        mPiecesDatabaseReference.updateChildren(updates);

                    }

                    @Override
                    public void onCancelled(@NotNull DatabaseError databaseError) {

                    }
                });

    }

    public static class Builder {
        private String title;
        private String author;
        private List<Integer> beats;
        private List<Integer> downBeats;
        private List<DataEntry> rawData;
        private int subdivision;
        private int countOffSubdivision;
        private int defaultTempo;
        private double tempoMultiplier;
        private int baselineNoteValue;
        private int displayNoteValue;
        private int measureCountOffset;
        private String firebaseId;
        private String creatorId;

        public Builder() {

        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder beats(List<Integer> beats) {
            this.beats = beats;
            return this;
        }

        //        public Builder beats(int[] beats) {
//            return beats(Utilities.arrayToIntegerList(beats));
//        }
        public Builder downBeats(List<Integer> downBeats) {
            this.downBeats = downBeats;
            return this;
        }

        //        public Builder downBeats(int[] downBeats) {
//            return downBeats(Utilities.arrayToIntegerList(downBeats));
//        }
        public Builder subdivision(int sub) {
            this.subdivision = sub;
            return this;
        }

        public Builder countOffSubdivision(int coSub) {
            this.countOffSubdivision = coSub;
            return this;
        }

        public Builder defaultTempo(int tempo) {
            this.defaultTempo = tempo;
            return this;
        }

        public Builder tempoMultiplier(double mult) {
            this.tempoMultiplier = mult;
            return this;
        }

        public Builder baselineNoteValue(int value) {
            this.baselineNoteValue = value;
            return this;
        }

        public Builder displayNoteValue(int value) {
            this.displayNoteValue = value;
            return this;
        }

        public Builder firstMeasureNumber(int offset) {
            this.measureCountOffset = offset - 1;
            return this;
        }

        public Builder dataEntries(List<DataEntry> data) {
            this.rawData = data;
            this.beats = new ArrayList<>();
            this.downBeats = new ArrayList<>();

            int start = data.get(0).isBarline() ? 1 : 0;

            int beatsPerMeasureCount = 0;

            for (int i = start; i < data.size(); i++) {

                if (data.get(i).isBarline()) {
                    this.downBeats.add(beatsPerMeasureCount);
                    beatsPerMeasureCount = 0;
                } else {
                    this.beats.add(data.get(i).getData());
                    beatsPerMeasureCount++;
                }
            }

            return this;
        }

        public Builder dataEntries(String data) {
            String[] entries = data.split("\\n");
            List<DataEntry> list = new ArrayList<>();
            for (String entry1 : entries) {
                String[] entry = entry1.split(";");
                list.add(new DataEntry(Integer.parseInt(entry[0]), Boolean.parseBoolean(entry[1])));
            }
            return dataEntries(list);
        }

        public boolean hasData() {
            return !(this.rawData == null);
        }

        public Builder firebaseId(String id) {
            this.firebaseId = id;
            return this;
        }

        public Builder creatorId(String id) {
            this.creatorId = id;
            return this;
        }

        public PieceOfMusic build() {
            return new PieceOfMusic(this);
        }
    }

    private PieceOfMusic(Builder builder) {
        mTitle = builder.title;
        mAuthor = builder.author;
        mBeats = builder.beats;
        mDownBeats = builder.downBeats;
        mSubdivision = builder.subdivision;
        mCountOffSubdivision = builder.countOffSubdivision;
        mMeasureCountOffset = builder.measureCountOffset;
        mDefaultTempo = builder.defaultTempo == 0 ? DEFAULT_DEFAULT_TEMPO : builder.defaultTempo;
        mTempoMultiplier = builder.tempoMultiplier == 0.0 ? 1.0 : builder.tempoMultiplier;
        mBaselineNoteValue = builder.baselineNoteValue == 0 ? QUARTER : builder.baselineNoteValue;
        mDisplayNoteValue = builder.displayNoteValue;
        mRawData = builder.rawData;
        mFirebaseId = builder.firebaseId;
        mCreatorId = builder.creatorId;

        buildCountoff();
    }
}
