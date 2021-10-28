/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.utils

/**
 * Any class which instantiates a Metronome, should implement this interface, in order for
 * the Metronome to communicate back to the instantiating class.
 *
 * Created by Michael on 2/28/2017.
 */
interface MetronomeStartStopListener {
    fun metronomeStartStop()
}