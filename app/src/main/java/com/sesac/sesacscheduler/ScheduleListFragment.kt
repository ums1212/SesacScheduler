package com.sesac.sesacscheduler

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sesac.sesacscheduler.databinding.FragmentScheduleListBinding

class ScheduleListFragment : BaseFragment<FragmentScheduleListBinding>(FragmentScheduleListBinding::inflate) {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_schedule_list, container, false)
    }

}