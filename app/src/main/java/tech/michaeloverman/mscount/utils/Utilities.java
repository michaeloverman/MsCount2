/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils;

import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

import tech.michaeloverman.mscount.database.ProgramDatabaseSchema;
import tech.michaeloverman.mscount.pojos.PieceOfMusic;
import timber.log.Timber;

/**
 * Various utility methods - rather generic in what they do, though used in some fairly
 * specific manners throughout program.
 *
 * Created by Michael on 2/26/2017.
 */

public class Utilities {

    // Suppress default constructor for noninstantiability
    private Utilities() {
        throw new AssertionError();
    }
    /**
     * Used to turn beat length data, in List, from database, into int[] for actual use by metronome.
     * @param integerList List of integers
     * @return array of ints
     */
    public static int[] integerListToArray(List<Integer> integerList) {
//        if(integerList == null) return null;
        int[] ints = new int[integerList.size()];
        for(int i = 0; i < integerList.size(); i++) {
            ints[i] = integerList.get(i);
        }
        return ints;
    }

//    /**
//     * Used by hard coded data entry. Should probably be removed prior to actual release.
//     * @param ints
//     * @return
//     */
//    public static List<Integer> arrayToIntegerList(int[] ints) {
//        List<Integer> list = new ArrayList<>();
//        for(int i = 0 ; i < ints.length; i++) {
//            list.add(ints[i]);
//        }
//        return list;
//    }

// --Commented out by Inspection START (5/13/2017 11:36 AM):
//    public static void printArray(int[] array) {
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < array.length; i++) {
//            sb.append(array[i]).append(", ");
//        }
//        Timber.d(sb.toString());
//    }
// --Commented out by Inspection STOP (5/13/2017 11:36 AM)

    @SuppressWarnings("unused")
    public static int[] combine(int[] countoff, int[] beats) {
        Timber.d("combine() arrays...");
        int[] combination = new int[countoff.length + beats.length];
        System.arraycopy(countoff, 0, combination, 0, countoff.length);
        System.arraycopy(beats, 0, combination, countoff.length, beats.length);
        return combination;
    }

    public static void appendCountoff(int[] countoff, List<Integer> beats, List<Integer> downBeats) {
        for(int i = countoff.length - 1; i >= 0; i--) {
            beats.add(0, countoff[i]);
        }
        downBeats.add(0, countoff.length);
//        return compound;
    }

    @SuppressWarnings("unused")
    public static List<Integer> createBeatList(int[] downbeats, int subd) {
        List<Integer> beats = new ArrayList<>();
        for (int downbeat : downbeats) {
            for (int j = 0; j < downbeat; j++) {
                beats.add(subd);
            }
        }
        return beats;
    }

    public static ContentValues getContentValuesFromPiece(PieceOfMusic piece) {
        ContentValues values = new ContentValues();
        values.put(ProgramDatabaseSchema.MetProgram.COLUMN_COMPOSER, piece.getAuthor());
        values.put(ProgramDatabaseSchema.MetProgram.COLUMN_TITLE, piece.getTitle());
        values.put(ProgramDatabaseSchema.MetProgram.COLUMN_PRIMARY_SUBDIVISIONS, piece.getSubdivision());
        values.put(ProgramDatabaseSchema.MetProgram.COLUMN_COUNTOFF_SUBDIVISIONS, piece.getCountOffSubdivision());
        values.put(ProgramDatabaseSchema.MetProgram.COLUMN_DEFAULT_TEMPO, piece.getDefaultTempo());
        values.put(ProgramDatabaseSchema.MetProgram.COLUMN_DEFAULT_RHYTHM, piece.getBaselineNoteValue());
        values.put(ProgramDatabaseSchema.MetProgram.COLUMN_TEMPO_MULTIPLIER, piece.getTempoMultiplier());
        values.put(ProgramDatabaseSchema.MetProgram.COLUMN_MEASURE_COUNT_OFFSET, piece.getMeasureCountOffset());
        values.put(ProgramDatabaseSchema.MetProgram.COLUMN_DATA_ARRAY, piece.rawDataAsString());
        values.put(ProgramDatabaseSchema.MetProgram.COLUMN_FIREBASE_ID, piece.getFirebaseId());
        values.put(ProgramDatabaseSchema.MetProgram.COLUMN_CREATOR_ID, piece.getCreatorId());
        values.put(ProgramDatabaseSchema.MetProgram.COLUMN_DISPLAY_RHYTHM, piece.getActualDisplayNoteValue());

        return values;
    }
}
