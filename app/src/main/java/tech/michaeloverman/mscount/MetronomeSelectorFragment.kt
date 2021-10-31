/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import tech.michaeloverman.mscount.databinding.MetSelectorFragmentBinding
import tech.michaeloverman.mscount.programmed.ProgrammedMetronomeFragment

/**
 * Fragment handles selection of & transition to the different metronomes.
 * Created by Michael on 2/24/2017.
 */
class MetronomeSelectorFragment : Fragment() {
    var mBigRoundButton: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
//        val navHostFragment = supportFragmentManager.findFragmentById(R.id.ms_count_navigation) as NavHostFragment
//        val navController = navHostFragment.navController
    }

    private var _binding: MetSelectorFragmentBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MetSelectorFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBigRoundButton = binding.bigRoundButton
        binding.normalMetronomeButton.setOnClickListener {
            navigateToNewFragment(NormalMetronomeFragment(), "normal")
        }
        binding.preprogrammedMetronomeButton.setOnClickListener {
            navigateToNewFragment(ProgrammedMetronomeFragment(), "program")
        }
        binding.oddMeterMetronomeButton.setOnClickListener {
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
        fun newInstance(): Fragment {
            return MetronomeSelectorFragment()
        }
    }

//    private fun navigateToProgram() {
//        val intent = Intent(context, ProgrammedMetronomeActivity::class.java)
//        val sharedView: View? = mBigRoundButton
//        val transitionName = getString(R.string.round_button_transition)
//        val transitionOptions = ActivityOptionsCompat
//            .makeSceneTransitionAnimation(requireActivity(), sharedView!!, transitionName)
//        startActivity(intent, transitionOptions.toBundle())
//    }
}