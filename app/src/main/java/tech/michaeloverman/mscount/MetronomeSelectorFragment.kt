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
import kotlinx.android.synthetic.main.met_selector_fragment.*
import tech.michaeloverman.mscount.programmed.ProgrammedMetronomeActivity

/**
 * Fragment handles selection of & transition to the different metronomes.
 * Created by Michael on 2/24/2017.
 */
class MetronomeSelectorFragment : Fragment() {
    var mBigRoundButton: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
//        val navHostFragment = supportFragmentManager.findFragmentById(R.id.ms_count_navigation) as NavHostFragment
//        val navController = navHostFragment.navController
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.met_selector_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBigRoundButton = big_round_button
        normal_metronome_button.setOnClickListener {
            navigateToNewFragment(NormalMetronomeFragment(), "normal")
        }
        preprogrammed_metronome_button.setOnClickListener {
            navigateToProgram()
        }
        odd_meter_metronome_button.setOnClickListener {
            navigateToNewFragment(OddMeterMetronomeFragment(), "odd")
        }
    }

    private fun navigateToNewFragment(frag: Fragment, fragName: String) {
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.fragment_container, frag)
            ?.addToBackStack(fragName)
            ?.commit()
    }

    companion object {
        private val TAG = MetronomeSelectorFragment::class.java.simpleName
        fun newInstance(): Fragment {
            return MetronomeSelectorFragment()
        }
    }

    private fun navigateToProgram() {
        val intent = Intent(context, ProgrammedMetronomeActivity::class.java)
        val sharedView: View? = mBigRoundButton
        val transitionName = getString(R.string.round_button_transition)
        val transitionOptions = ActivityOptionsCompat
            .makeSceneTransitionAnimation(requireActivity(), sharedView!!, transitionName)
        startActivity(intent, transitionOptions.toBundle())
    }
}