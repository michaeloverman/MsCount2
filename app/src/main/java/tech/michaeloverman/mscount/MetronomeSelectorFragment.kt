/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import tech.michaeloverman.mscount.MetronomeSelectorFragment
import tech.michaeloverman.mscount.programmed.ProgrammedMetronomeActivity
import timber.log.Timber

/**
 * Fragment handles selection of & transition to the different metronomes.
 * Created by Michael on 2/24/2017.
 */
class MetronomeSelectorFragment : Fragment() {
    @JvmField
	@BindView(R.id.big_round_button)
    var mBigRoundButton: ImageView? = null

    //	@BindView(R.id.normal_metronome_button) Button mNormalMetButton;
    //	@BindView(R.id.preprogrammed_metronome_button) Button mPreprogrammedMetButton;
    private var mUnbinder: Unbinder? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.met_selector_fragment, container, false)
        mUnbinder = ButterKnife.bind(this, view)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mUnbinder!!.unbind()
    }

    @OnClick(R.id.normal_metronome_button, R.id.preprogrammed_metronome_button, R.id.odd_meter_metronome_button)
    fun buttonClicked(button: View) {
        Timber.d(TAG, "buttonClicked()")
        val intent: Intent?
        intent = when (button.id) {
            R.id.normal_metronome_button -> Intent(activity, NormalMetronomeActivity::class.java)
            R.id.odd_meter_metronome_button -> Intent(activity, OddMeterMetronomeActivity::class.java)
            R.id.preprogrammed_metronome_button -> Intent(activity, ProgrammedMetronomeActivity::class.java)
            else -> null
        }
        val sharedView: View? = mBigRoundButton
        val transitionName = getString(R.string.round_button_transition)
        val transitionOptions = ActivityOptionsCompat
                .makeSceneTransitionAnimation(requireActivity(), sharedView!!, transitionName)
        if (intent != null) {
            startActivity(intent, transitionOptions.toBundle())
        }
    }

    companion object {
        private val TAG = MetronomeSelectorFragment::class.java.simpleName
        fun newInstance(): Fragment {
            return MetronomeSelectorFragment()
        }
    }
}