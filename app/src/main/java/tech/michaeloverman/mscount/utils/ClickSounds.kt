package tech.michaeloverman.mscount.utils

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioManager
import android.media.SoundPool
import android.os.AsyncTask
import tech.michaeloverman.mscount.pojos.Click
import timber.log.Timber
import java.io.IOException
import java.util.*

/**
 * This class holds the click sounds for the SoundPool. It loads the sounds in an asyncTask
 * when first initialized.
 *
 * Created by Michael on 5/12/2017.
 */
object ClickSounds {
    private const val SOUNDS_FOLDER = "sample_sounds"
    private var mAssets: AssetManager? = null
    private val mClicks: MutableList<Click> = ArrayList()
    private var mSoundPool: SoundPool? = null
    fun loadSounds(context: Context) {
        if (mSoundPool != null) return
        mSoundPool = SoundPool(5, AudioManager.STREAM_MUSIC, 0)
        mAssets = context.assets
        LoadSoundsTask().execute()
    }

    val clicks: List<Click>
        get() = mClicks

    fun getSoundPool(context: Context): SoundPool? {
        if (mSoundPool == null) {
            loadSounds(context)
        }
        return mSoundPool
    }

    private class LoadSoundsTask : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {
            val soundNames: Array<String>?
            try {
                soundNames = mAssets!!.list(SOUNDS_FOLDER)
                if (soundNames != null) Timber.d("Found %s sounds", soundNames.size)
            } catch (ioe: IOException) {
                Timber.d("Could not list assets: %s", ioe.localizedMessage)
                return null
            }
            for (filename in soundNames!!) {
                try {
                    val assetPath = SOUNDS_FOLDER + "/" + filename
                    val click = Click(assetPath)
                    load(click)
                    mClicks.add(click)
                    Timber.d("  Loaded: %s", filename)
                } catch (ioe: IOException) {
                    Timber.d("Could not load sound %s: %s", filename, ioe.localizedMessage)
                    return null
                }
            }
            return null
        }

        @Throws(IOException::class)
        private fun load(click: Click) {
            val afd = mAssets!!.openFd(click.assetPath)
            val soundId = mSoundPool!!.load(afd, 1)
            click.soundId = soundId
        }
    }
}