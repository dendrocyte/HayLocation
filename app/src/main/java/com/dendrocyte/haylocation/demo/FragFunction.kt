package com.dendrocyte.haylocation.demo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dendrocyte.haylocation.module.man.UtilDelegator
import com.dendrocyte.haylocation.module.pin.util.LocationUpdateUtil
import cus.yiling.location.databinding.FragFunctionBinding

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
public class FragFunction : Fragment() {
    /**
     * declare delegator before onCreate()
     * let ActivityResultLauncher register first
     */
    private val delegator = UtilDelegator(this)
    val TAG = this::class.java.simpleName
    private var _binding: FragFunctionBinding? = null
    private val binding : FragFunctionBinding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragFunctionBinding.inflate(inflater, container,false).run {
            _binding = this
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }


    override fun onStart() {
        super.onStart()
        with(delegator){
            method = LocationUpdateUtil.LocationMethod.GetLastLocation
            locationSuccessObserver = { location ->
                binding.tVresult.text = "(${location.longitude}, ${location.latitude})"
            }
            locationErrObserver = { e ->
                Log.e(TAG, "LocationError: $e")
                binding.tVresult.text = "Loading Failed"
            }
            attach(requireContext()).configure().start()
        }

    }

    override fun onStop() {
        super.onStop()
        delegator.release()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
