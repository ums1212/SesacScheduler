package com.sesac.sesacscheduler

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import com.sesac.sesacscheduler.databinding.FragmentScheduleListBinding

class ScheduleListFragment : BaseFragment<FragmentScheduleListBinding>(FragmentScheduleListBinding::inflate) {
        override lateinit var binding: FragmentScheduleListBinding
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
        savedInstanceState: Bundle?
        ): View? {
            binding = FragmentScheduleListBinding.inflate(inflater, container, false)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            // 데이터 준비 (예시)
            val scheduleList = mutableListOf("일정1", "일정2", "일정3")

            // Adapter 생성
            val adapter = ArrayAdapter(requireContext(), R.layout.simple_list_item_1, scheduleList)

            // ListView에 Adapter 설정
            binding.scheduleList.adapter = adapter

            // 데이터 추가 버튼 클릭 이벤트 처리
            binding.buttonAdd.setOnClickListener {
                // 데이터 추가
                scheduleList.add(0, "새로운 일정")
            }
        }
    }