package com.dendrocyte.haylocation.module.customView

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.location.Address
import android.location.Location
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.dendrocyte.haylocation.R
import com.dendrocyte.haylocation.module.pin.core.Callback
import com.dendrocyte.haylocation.module.pin.util.GPSResolvableApiLifecycleObserver
import com.dendrocyte.haylocation.module.pin.util.GpsUtil
import com.dendrocyte.haylocation.module.pin.util.LocationUpdateUtil
import com.dendrocyte.haylocation.module.pin.util.PermissionLifecycleObserver
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationRequest

/**
 * Created by luyiling on 2019/4/4
 * @feature register / add / remove observer, actionHandler
 * @feature use customize btn/ textview to show the location information,
 * @feature inclusive of google location module, permission check and gps check, reverse geocoding
 */
/**
 * Created by luyiling on 2024/2/23
 * Modified by
 *
 * TODO:
 * Description:這是一個view，自己去索取位置並呈現
 *
 * @feature
 * @params
 */
class UtilView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    private val TAG = UtilView::class.java.simpleName
    private var permissionLifecycleObserver : PermissionLifecycleObserver
    private var gpsLifecycleObserver : GPSResolvableApiLifecycleObserver
    private var locationLifecycleOwner : LocationUpdateLifecycleObserver
    lateinit var gpsUtil: GpsUtil
    var locationUtil : LocationUpdateUtil
    private var method : LocationUpdateUtil.LocationMethod


    //Activity Lifecycle Owner: findViewTreeLifecycleOwner = Null
    //Activity Lifecycle Owner: context as? LifecycleOwner exist, state = INITIALIZED
    init {

        //1.Get activity's lifecycle owner
        val owner: LifecycleOwner? = context as? LifecycleOwner

        //2.Add activity/fragment observer
        permissionLifecycleObserver = PermissionLifecycleObserver(
            object: Callback {
                override fun onGranted() {
                    gpsUtil.exeGPSOn()
                }
                override fun onDenied(denyPermission: Map<String, Boolean>) {
                    Log.e(TAG, "Permission Denied")
                    doPermissionFail()
                }
            }
        )
        gpsLifecycleObserver = GPSResolvableApiLifecycleObserver{ on ->
            if (on) doNext() /*do the key things*/
            else doGPSFail()
        }
        locationLifecycleOwner = LocationUpdateLifecycleObserver()
        owner?.lifecycle?.addObserver(permissionLifecycleObserver)
        owner?.lifecycle?.addObserver(gpsLifecycleObserver)
        owner?.lifecycle?.addObserver(locationLifecycleOwner)


        //3. Grep config from xml
        /**
         * -locationSettingRequest (optional)
         * -locationRequest (optional)
         * -method (must-have)
         */
        val defStyleAttr = android.R.attr.textViewStyle
        val defStyleRes = 0
        var locationReq : LocationRequest = GpsUtil.MY_DEFAULT_LOCATION_REQ
        var settingReq : LocationSettingsRequest? = null
        with(context.theme.obtainStyledAttributes(
            attrs, R.styleable.UtilBtn, defStyleAttr, defStyleRes
        )){
            //Save type array for check by Inspection tool
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Stores debugging information about attributes.
                // This should be called in a constructor by every custom View that uses a custom styleable.
                // If the custom view does not call it,
                // then the custom attributes used by this view will not be visible in layout inspection tools.
                saveAttributeDataForStyleable(
                    context, R.styleable.UtilBtn,attrs,
                    this, defStyleAttr,
                    defStyleRes
                )
            }

            //Grep locationRequest
            if (hasValue(R.styleable.UtilBtn_intervalMillis)){
                val default: Long = 60 * 60 * 1000 //google's default. refer to LocationRequest
                val builder = LocationRequest.Builder(
                    try{
                        getString(R.styleable.UtilBtn_intervalMillis)
                            ?.toLong()
                            //?.coerceAtLeast(0) // 若是負值以0計 //google will check
                            ?.run {
                                if(this@run == 0L) return@run default
                                else this
                            }
                            ?: default
                    }catch (e: NumberFormatException){
                        default
                    }
                )
                if (hasValue(R.styleable.UtilBtn_priority))
                    builder.setPriority(
                        when(getInt(R.styleable.UtilBtn_priority, 0)){
                            1 -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                            2 -> LocationRequest.PRIORITY_LOW_POWER
                            //google's default. refer to LocationRequest
                            //0 ->
                            else -> LocationRequest.PRIORITY_HIGH_ACCURACY
                        }
                    )
                if (hasValue(R.styleable.UtilBtn_granularity)){
                    builder.setGranularity(
                        when(getInt(R.styleable.UtilBtn_granularity, 0)){
                            1 -> Granularity.GRANULARITY_COARSE
                            2 -> Granularity.GRANULARITY_FINE
                            //google's default. refer to LocationRequest
                            //0 ->
                            else -> Granularity.GRANULARITY_PERMISSION_LEVEL
                        }
                    )
                }
                if (hasValue(R.styleable.UtilBtn_durationMillis)){
                    val default : Long = Long.MAX_VALUE //google's default. refer to LocationRequest
                    builder.setDurationMillis(
                        try{
                            getString(R.styleable.UtilBtn_durationMillis)
                                ?.toLong()
                                //?.coerceAtLeast(0) // 若是負值以0計 //google will check
                                ?.run {
                                    if(this@run == 0L) return@run default
                                    else this
                                }
                                ?: default
                        }catch (e: NumberFormatException){
                            default
                        }
                    )
                }
                if (hasValue(R.styleable.UtilBtn_maxUpdates)){
                    builder.setMaxUpdates(
                        getInt(
                            R.styleable.UtilBtn_maxUpdates,
                            Integer.MAX_VALUE //google's default. refer to LocationRequest
                        ).coerceAtLeast(1) // 若是0,負值以1計
                    )
                }
                if (hasValue(R.styleable.UtilBtn_minUpdateIntervalMillis)){
                    val default: Long = 60 * 10 * 1000 //google's default. refer to LocationRequest
                    builder.setMinUpdateIntervalMillis(
                        try{
                            getString(R.styleable.UtilBtn_minUpdateIntervalMillis)
                                ?.toLong()
                                //?.coerceAtLeast(0L) // 若是負值以0計 //google will check
                                ?.run {
                                    if(this@run == 0L) return@run default
                                    else this
                                }
                                ?: default
                        }catch (e: NumberFormatException){
                            default
                        }
                    )
                }
                if (hasValue(R.styleable.UtilBtn_maxUpdateDelayMillis)){
                    val default = 0L //google's default. refer to LocationRequest
                    builder.setMaxUpdateDelayMillis(
                        try{
                            getString(R.styleable.UtilBtn_maxUpdateDelayMillis)
                                ?.toLong()
                            //?.coerceAtLeast(0L) // 若是負值以0計 //google will check
                                ?: default
                        }catch (e: NumberFormatException){
                            default
                        }
                    )
                }
                if (hasValue(R.styleable.UtilBtn_maxUpdateAgeMillis)){
                    val default = -1L //google's default. refer to LocationRequest
                    builder.setMaxUpdateAgeMillis(
                        try{
                            getString(R.styleable.UtilBtn_maxUpdateAgeMillis)
                                ?.toLong()
                                //?.coerceAtLeast(-1L) // 若是小於-1以-1計  //google will check
                                ?: default
                        }catch (e: NumberFormatException){
                            default
                        }
                    )
                }
                if (hasValue(R.styleable.UtilBtn_waitForAccurateLocation)){
                    builder.setWaitForAccurateLocation(
                        getBoolean(
                            R.styleable.UtilBtn_waitForAccurateLocation,
                            false//google's default. refer to LocationRequest
                        )
                    )
                }
                if (hasValue(R.styleable.UtilBtn_minUpdateDistanceMeters)) {
                    builder.setMinUpdateDistanceMeters(
                        getFloat(
                            R.styleable.UtilBtn_minUpdateDistanceMeters,
                            0f //google's default. refer to LocationRequest
                        )
                    )
                }
                locationReq = builder.build()
            }

            //Grep locationSettingRequest
            if (hasValue(R.styleable.UtilBtn_alwaysShow)){
                settingReq = LocationSettingsRequest.Builder()
                    .addLocationRequest(locationReq)
                    .setAlwaysShow(
                        getBoolean(
                            R.styleable.UtilBtn_alwaysShow,
                            false //google's default. refer to LocationSettingRequest
                        )
                    )
                    .build()
            }

            //Grep locationMethod, this is must-have
            //if not set on xml, feed default
            //default: GetLastLocation(0)
            method = LocationUpdateUtil.LocationMethod.getId(
                getInt(R.styleable.UtilBtn_locationMethod, 0)
            )

            //Recycle type array
            recycle()
        }



        //4. Declare Util Instance
        /**
         * 再之後gpsUtil生成後，再把observer 給util, 這樣就不用一定要在xml做宣告了
         * 之所以要xml做宣告是為了registerActivityResult, 要在onCreate()就宣告, 此時正是view剛生成時
         */
        gpsUtil = GpsUtil.Builder(context)
            .addObserver(gpsLifecycleObserver)
            .also {
                /* //custom locationSettingRequest yourself from 3.
                * .addSettingRequest()
                */
                if(settingReq != null)
                    it.addSettingRequest(settingReq!!)
                /* //custom locationRequest yourself from 3.
                 * .addLocationRequest()
                */
                if(locationReq != GpsUtil.MY_DEFAULT_LOCATION_REQ)
                    it.addLocationRequest(locationReq)
            }
            .build()
        locationUtil = LocationUpdateUtil(context.applicationContext)

    }




    /**
     * `internal` modifier does not only use in the same module,
     * this is not for end user to modify
     *
     * @purpose get setting from xml to know use which func of locationUtil
     * note: key point
     */
    internal fun requestLocation(){
        when(method){
            LocationUpdateUtil.LocationMethod.GetLocationUpdates -> {
                locationUtil.getLocationUpdates(
                    gpsUtil.locateRequest!!,
                    { location : Location ->
                        Log.d(TAG, "=> $location")
                        text = "(${location.longitude}, ${location.latitude})"
                    }, { e ->

                    },
                    false
                )
            }
            LocationUpdateUtil.LocationMethod.GetAddressUpdates -> {
                locationUtil.getAddressUpdates(
                    gpsUtil.locateRequest!!,
                    { address : Address ->
                        Log.d(TAG, "=> $address")
                        text = "${address.adminArea}"
                    }, { e ->

                    },
                    false
                )
            }
            LocationUpdateUtil.LocationMethod.GetLastAddress -> {
                locationUtil.getLastAddress(
                    true, //因為GPSOn 才做doNext()
                    { address : Address ->
                        Log.d(TAG, "=> $address")
                        text = "${address.adminArea}"
                    }, { e ->

                    }
                )
            }
            //LocationUpdateUtil.LocationMethod.GetLastLocation
            else -> {
                locationUtil.getLastLocation(
                    true, //因為GPSOn 才做doNext()
                    { location : Location ->
                        Log.d(TAG, "=> $location")
                        text = "(${location.longitude}, ${location.latitude})"
                    }, { e ->

                    }
                )
            }
        }
    }


    /*為了拿到activity的context */
    fun getActivity(cont: Context?): Activity? {
        if (cont == null) return null
        else if (cont is Activity) return cont
        else if (cont is ContextWrapper) return getActivity(cont.baseContext)
        return null
    }




    //********************************//
    //********* key point ************//
    //********************************//

    /**
     * 可以override
     */
    fun start() = permissionLifecycleObserver.askLocation(context)

    /**
     * 可以override
     * User want to customize doNext(), it shall use super.doNext() as well
     */
    fun doNext() = requestLocation()

    /**
     * 可以override
     */
    fun doGPSFail() { text = "loading failed" }

    /**
     * 可以override
     */
    fun doPermissionFail() { text = "loading failed" }
    //********************************//
    //********************************//
    //********************************//




    //Activity Lifecycle Owner: findViewTreeLifecycleOwner exist, state = RESUMED
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "onAttachedToWindow")
        /**
         * 需讓LifecycleOwner在state=CREATED,
         * 註冊PermissionUtil.ActivityResultContracts
         * 註冊GPSUtil.ActivityResultContracts
         */
        //4. note: 正式啟動！
        start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        locationUtil.detach()
    }



    inner class LocationUpdateLifecycleObserver: DefaultLifecycleObserver {
        private val _TAG = LocationUpdateLifecycleObserver::class.java.simpleName
        private var permitRestart = false
        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)
            Log.d(_TAG, "onCreate")
        }

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            Log.d(_TAG, "onStart")
            if (permitRestart) {
                /**
                 * 應該要從permission 從頭再來檢查一次，
                 * 因為permission和GPS都可以在被使用者關掉,
                 * 所以並非從doNext() 而是start()
                 */
                start()
                permitRestart = false
            }
        }
        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            Log.d(_TAG, "onStop")
            permitRestart = true
            locationUtil.detach()
        }
    }

}
