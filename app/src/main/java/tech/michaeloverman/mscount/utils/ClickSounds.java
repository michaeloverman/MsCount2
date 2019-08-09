package tech.michaeloverman.mscount.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tech.michaeloverman.mscount.pojos.Click;
import timber.log.Timber;

/**
 * This class holds the click sounds for the SoundPool. It loads the sounds in an asyncTask
 * when first initialized.
 *
 * Created by Michael on 5/12/2017.
 */

public class ClickSounds {

    private static final String SOUNDS_FOLDER = "sample_sounds";

    private static AssetManager mAssets;
    private static final List<Click> mClicks = new ArrayList<>();
    private static SoundPool mSoundPool;

    public static void loadSounds(Context context) {
        if(mSoundPool != null) return;
        //noinspection deprecation  // TODO fix this properly with SoundPool.Builder
        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        mAssets = context.getAssets();
        new LoadSoundsTask().execute();
    }

    public static List<Click> getClicks() {
        return mClicks;
    }

    public static SoundPool getSoundPool(Context context) {
        if(mSoundPool == null) {
            loadSounds(context);
        }
        return mSoundPool;
    }

    private static class LoadSoundsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            String[] soundNames;
            try {
                soundNames = mAssets.list(SOUNDS_FOLDER);
                if(soundNames != null) Timber.d("Found %s sounds", soundNames.length);
            } catch (IOException ioe) {
                Timber.d("Could not list assets: %s", ioe.getLocalizedMessage());
                return null;
            }
            for (String filename : soundNames) {
                try {
                    String assetPath = SOUNDS_FOLDER + "/" + filename;
                    Click click = new Click(assetPath);
                    load(click);
                    mClicks.add(click);
                    Timber.d("  Loaded: %s", filename);
                } catch (IOException ioe) {
                    Timber.d("Could not load sound %s: %s", filename, ioe.getLocalizedMessage());
                    return null;
                }
            }
            return null;
        }

        private void load(Click click) throws IOException {
            AssetFileDescriptor afd = mAssets.openFd(click.getAssetPath());
            int soundId = mSoundPool.load(afd, 1);
            click.setSoundId(soundId);
        }
    }

}
