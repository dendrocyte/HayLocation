package com.dendrocyte.haylocation.module.pin

import android.app.Service
import android.content.Intent
import android.location.Address
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.ResultReceiver
import android.text.TextUtils
import android.util.Log
import androidx.core.os.bundleOf
import com.dendrocyte.haylocation.Constants
import com.dendrocyte.haylocation.R
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.Locale


/**
 * Created by luyiling on 2024/12/10
 * Geocoder 太不穩定，改用半付費的 Google Maps Geocoding API
 * @refer https://developers.google.com/maps/documentation/geocoding/requests-geocoding?hl=zh-tw
 * @refer https://developers.google.com/maps/documentation/geocoding/requests-geocoding?hl=zh-tw#Types
 * @refer https://developers.google.com/maps/documentation/geocoding/requests-reverse-geocoding?hl=zh-tw
 */
/**
 * Created by luyiling on 2024/12/9
 * 錯誤訊息：Service not Available
 * 可能原因：
 * 1. 设备无网络连接
 * Geocoder 服务依赖网络连接来查询地理编码数据。
 * 如果设备当前无网络连接或网络不稳定。
 * 2. Google Play 服务不可用
 * Geocoder 通常依赖 Google Play 服务来处理请求。
 * 如果设备上没有安装 Google Play 服务，或者服务被禁用。
 * 3. Geocoder 服务暂时不可用 --> （常遇到）
 * 即使网络连接正常，也可能因为服务端的问题（如 Google 的地理编码服务中断或超时）导致此错误。
 * 4. 设备或环境限制
 * 某些设备可能没有启用或支持 Geocoder 服务，
 * 尤其是在中国大陆等地区，Google 的服务可能被屏蔽，导致无法访问。
 * 5. 地理坐标无效
 * 如果提供给 Geocoder 的经纬度超出了合法范围，
 * 例如纬度小于 -90 或大于 90，或者经度小于 -180 或大于 180，会导致服务抛出异常。
 */
/**
 * Created by luyiling on 2024/2/26
 * Modified by
 * 是否要改成reverse geocoding 比較reliable
 * 因為google 包沒有下載，要重開機才有用能用
 * https://developers.google.com/maps/documentation/geocoding/requests-reverse-geocoding?hl=zh-tw#reverse-requests
 *
 * TODO:
 * Description:
 *
 * @params
 * @params
 */
/**
 * Created by luyiling on 2019/4/6
 *
 *
 * TODO:
 * The getFromLocation() method provided by the Geocoder class accepts a latitude and longitude
 * and returns a list of addresses.
 * The method is synchronous and may take a long time to do its work,
 * so you should not call it from the main, user interface (UI) thread of your app.
 */
class ParseLocationService : Service() {
    private val TAG = ParseLocationService::class.java.simpleName
    private lateinit var handlerThread : HandlerThread
    private lateinit var handler : Handler

    /*reverse geocoding*/
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Get the location passed to this service through an extra.
        val location: Location = intent.getParcelableExtra(
            Constants.LOCATION_DATA_EXTRA
        ) ?: throw Exception("Miss param: Location")
        val receiver : ResultReceiver = intent.getParcelableExtra(
            Constants.RECEIVER
        ) ?: throw Exception("Miss param: ResultReceiver")

        var addresses: List<Address>? = null
        val client = OkHttpClient()
        var errorMessage = ""

        try {
            handlerThread = HandlerThread("Get address")
            handlerThread.start()
            handler = Handler(handlerThread.looper)
            handler.post{
                // reverse geocoding
                val url = "https://us-central1-haylocation-96cfd.cloudfunctions.net/" +
                        "reverseGeocode"+
                        "?lat=${location.latitude}&lng=${location.longitude}"
                val request = Request.Builder()
                    .url(url)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Reverse geocoding failed. Unexpected code $response")
                    val responseData = response.body?.string() ?: ""
                    // convert AddressResult to Address
                    val model = Gson().fromJson(
                        responseData, AddressModel::class.java
                    )
                    /**
                     * "status" 欄位可能包含下列值：
                     *
                     * -"OK" 表示沒有發生任何錯誤。地址剖析成功，且系統傳回至少一組地理編碼。
                     * -"ZERO_RESULTS" 表示地理編碼成功，但沒有傳回任何結果。如果地理編碼器收到不存在的 address，就有可能發生這種情況。
                     * -"OVER_DAILY_LIMIT" 表示下列任一項目：
                     * API 金鑰遺失或無效。
                     * 您的帳戶尚未啟用結帳功能。
                     * 超過自行設定的用量上限。
                     * 您提供的付款方式已失效 (例如信用卡已過期)。
                     * 請參閱 Google 地圖常見問題，瞭解如何修正這個問題。
                     *
                     * -"OVER_QUERY_LIMIT" 表示您已超過配額。
                     * -"REQUEST_DENIED" 表示您的要求遭拒。
                     * -"INVALID_REQUEST" 通常表示缺少查詢內容 (address、components 或 latlng)。
                     * -"UNKNOWN_ERROR" 表示伺服器發生錯誤，因此無法處理要求。如果再試一次，要求可能會成功。
                     *
                     */
                    if(model.status != "OK") throw Exception("Reverse geocoding failed. Unexpected code $responseData")
                    // 一般來說，系統只會針對地址查詢傳回 "results" 陣列中的一個項目
                    // 但如果地址查詢含糊不清，地理編碼器可能會傳回多個結果。
                    addresses = model.results.map { toAddress(it) }
                    Log.i(TAG, getString(R.string.address_found))
                }
            }

            while(addresses == null){
                Thread.sleep(1000)
            }

            handler.removeCallbacksAndMessages(null)
            handlerThread.quit()

        } catch (e: NullPointerException) { // location is null.
            errorMessage = getString(R.string.no_location_data_provided)
            Log.e(TAG, errorMessage, e)
        } catch (e: IOException) { // Catch network or other I/O problems.
            errorMessage = getString(R.string.service_not_available)
            Log.e(TAG, errorMessage, e)
        } catch (e: IllegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used)
            Log.e(
                TAG, errorMessage + ". " +
                        "Latitude = " + location!!.latitude +
                        ", Longitude = " +
                        location.longitude, e
            )
        } catch (e: Exception){
            errorMessage = getString(R.string.no_address_found)
            Log.e(TAG, errorMessage, e)
        }


        // Deliver to receiver
        val addr : Address? = addresses?.get(0)
        var msg : String = addr?.adminArea ?: errorMessage
        var code : Int =
            if (addr == null) Constants.FAILURE_RESULT
            else Constants.SUCCESS_RESULT
        receiver.send(code, bundleOf(
            Constants.RESULT_DATA_MSG to msg,
            Constants.RESULT_DATA_OBJ to addr
        ))
        return super.onStartCommand(intent, flags, startId)
    }

    override fun stopService(name: Intent?): Boolean {
        if (::handlerThread.isInitialized)
            handlerThread.quit()
        if (::handler.isInitialized)
            handler.removeCallbacksAndMessages(null)
        return super.stopService(name)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // {
    //   "results" : [
    //      {
    //         "address_components" : [
    //            {
    //               "long_name" : "277",
    //               "short_name" : "277",
    //               "types" : [ "street_number" ]
    //            },
    //            {
    //               "long_name" : "Bedford Avenue",
    //               "short_name" : "Bedford Ave",
    //               "types" : [ "route" ]
    //            },
    //            {
    //               "long_name" : "Williamsburg",
    //               "short_name" : "Williamsburg",
    //               "types" : [ "neighborhood", "political" ]
    //            },
    //            {
    //               "long_name" : "Brooklyn",
    //               "short_name" : "Brooklyn",
    //               "types" : [ "sublocality", "political" ]
    //            },
    //            {
    //               "long_name" : "Kings",
    //               "short_name" : "Kings",
    //               "types" : [ "administrative_area_level_2", "political" ]
    //            },
    //            {
    //               "long_name" : "New York",
    //               "short_name" : "NY",
    //               "types" : [ "administrative_area_level_1", "political" ]
    //            },
    //            {
    //               "long_name" : "United States",
    //               "short_name" : "US",
    //               "types" : [ "country", "political" ]
    //            },
    //            {
    //               "long_name" : "11211",
    //               "short_name" : "11211",
    //               "types" : [ "postal_code" ]
    //            }
    //         ],
    //         "formatted_address" : "277 Bedford Avenue, Brooklyn, NY 11211, USA",
    //         "geometry" : {
    //            "location" : {
    //               "lat" : 40.714232,
    //               "lng" : -73.9612889
    //            },
    //            "location_type" : "ROOFTOP",
    //            "viewport" : {
    //               "northeast" : {
    //                  "lat" : 40.7155809802915,
    //                  "lng" : -73.9599399197085
    //               },
    //               "southwest" : {
    //                  "lat" : 40.7128830197085,
    //                  "lng" : -73.96263788029151
    //               }
    //            }
    //         },
    //         "place_id" : "ChIJd8BlQ2BZwokRAFUEcm_qrcA",
    //         "types" : [ "street_address" ]
    //      },
    //
    //  ... Additional <code>results[]</code> ...
    private fun toAddress(model: Result) : Address{
        println("result=${model}")
        val address = Address(Locale.getDefault())
        for (data in model.addressComponents){
            if (data.types.contains("postal_code"))
                address.postalCode = data.longName
            if (data.types.contains("country"))
                address.countryName = data.longName
            if (data.types.contains("administrative_area_level_1"))
                address.adminArea = data.longName //city
            if (data.types.contains("sublocality"))
                address.subLocality = data.longName
            if (data.types.contains("administrative_area_level_2"))
                address.subAdminArea = data.longName //dist
            if (data.types.contains("route"))
                address.thoroughfare = data.longName
            if (data.types.contains("street_number"))
                address.subThoroughfare = data.longName
        }
        address.latitude = model.geometry.location.lat
        address.longitude = model.geometry.location.lng
        // whole address = address.getAddressLine(0)
        return address
    }
}

