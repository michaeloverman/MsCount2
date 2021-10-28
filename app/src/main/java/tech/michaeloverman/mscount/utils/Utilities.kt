/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils

import android.content.ContentValues
import tech.michaeloverman.mscount.database.ProgramDatabaseSchema
import tech.michaeloverman.mscount.pojos.PieceOfMusic
import timber.log.Timber
import java.util.*

/**
 * Various utility methods - rather generic in what they do, though used in some fairly
 * specific manners throughout program.
 *
 * Created by Michael on 2/26/2017.
 */
class Utilities private constructor() {
    companion object {
        /**
         * Used to turn beat length data, in List, from database, into int[] for actual use by metronome.
         * @param integerList List of integers
         * @return array of ints
         */
        fun integerListToArray(integerList: List<Int>): IntArray {
//        if(integerList == null) return null;
            val ints = IntArray(integerList.size)
            for (i in integerList.indices) {
                ints[i] = integerList[i]
            }
            return ints
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
        fun combine(countoff: IntArray, beats: IntArray): IntArray {
            Timber.d("combine() arrays...")
            val combination = IntArray(countoff.size + beats.size)
            System.arraycopy(countoff, 0, combination, 0, countoff.size)
            System.arraycopy(beats, 0, combination, countoff.size, beats.size)
            return combination
        }

        fun appendCountoff(countoff: IntArray, beats: MutableList<Int>, downBeats: MutableList<Int>) {
            for (i in countoff.indices.reversed()) {
                beats.add(0, countoff[i])
            }
            downBeats.add(0, countoff.size)
            //        return compound;
        }

        fun createBeatList(downbeats: IntArray, subd: Int): List<Int> {
            val beats: MutableList<Int> = ArrayList()
            for (downbeat in downbeats) {
                for (j in 0 until downbeat) {
                    beats.add(subd)
                }
            }
            return beats
        }

        fun getContentValuesFromPiece(piece: PieceOfMusic): ContentValues {
            val values = ContentValues()
            values.put(ProgramDatabaseSchema.MetProgram.COLUMN_COMPOSER, piece.author)
            values.put(ProgramDatabaseSchema.MetProgram.COLUMN_TITLE, piece.title)
            values.put(ProgramDatabaseSchema.MetProgram.COLUMN_PRIMARY_SUBDIVISIONS, piece.subdivision)
            values.put(ProgramDatabaseSchema.MetProgram.COLUMN_COUNTOFF_SUBDIVISIONS, piece.countOffSubdivision)
            values.put(ProgramDatabaseSchema.MetProgram.COLUMN_DEFAULT_TEMPO, piece.defaultTempo)
            values.put(ProgramDatabaseSchema.MetProgram.COLUMN_DEFAULT_RHYTHM, piece.baselineNoteValue)
            values.put(ProgramDatabaseSchema.MetProgram.COLUMN_TEMPO_MULTIPLIER, piece.tempoMultiplier)
            values.put(ProgramDatabaseSchema.MetProgram.COLUMN_MEASURE_COUNT_OFFSET, piece.measureCountOffset)
            values.put(ProgramDatabaseSchema.MetProgram.COLUMN_DATA_ARRAY, piece.rawDataAsString())
            values.put(ProgramDatabaseSchema.MetProgram.COLUMN_FIREBASE_ID, piece.firebaseId)
            values.put(ProgramDatabaseSchema.MetProgram.COLUMN_CREATOR_ID, piece.creatorId)
            values.put(ProgramDatabaseSchema.MetProgram.COLUMN_DISPLAY_RHYTHM, piece.actualDisplayNoteValue)
            return values
        }
    }
}