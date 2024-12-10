package com.dendrocyte.haylocation.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cus.yiling.location.databinding.FragBlankBinding

/**
 * Created by luyiling on 2024/3/11
 * Modified by
 *
 * TODO:
 * Description:
 *
 * @params
 * @params
 */
/**
 * Created by luyiling on 2019/3/31
 *
 *
 * TODO:
 * 測試
 * activity 動態加入location btn ＋ activity 繼承包裝的activity 是可以收到 activity Result
 * fragment 動態加入location btn ＋ activity 繼承包裝的activity 是可以收到 activity Result
 * <IMPORTANT></IMPORTANT>
 */
public class FragBlank : Fragment() {

    val TAG = this::class.java.simpleName
    private var _binding: FragBlankBinding? = null
    private val binding : FragBlankBinding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragBlankBinding.inflate(inflater, container,false).run {
            _binding = this
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
