### HayLocation
----

This is a textView to ask locate permission, check GPS on and present location by itself

![presentation](./app/asset/glocation.gif)
<br><br>
### Use
----
This library is hosted on GitHub Packages, so you will need to generate a GitHub Personal Access Token (PAT) to download it.
<br><br>
#### I. Generate a Token:
1. Log in to your GitHub account.
2. Go to [Token Settings Page](https://github.com/settings/tokens).
3. Click **Generate new token (classic)** and select the **`read:packages`** permission.
4. Save the generated token securely.

#### II. Declare dependency
`setting.gradle`
```
pluginManagement {
    repositories {
        //Set credential at local.properties
        def localProps = new Properties()
        file("local.properties").withInputStream {
            localProps.load(it)
        }
        maven {
          url "https://maven.pkg.github.com/dendrocyte/haylocation"
          credentials {
                username = "dendrocyte"
                password = localProps.getProperty("github.haylocation.token")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        //Set credential at local.properties
        def localProps = new Properties()
        file("local.properties").withInputStream {
            localProps.load(it)
        }
        maven {
          url "https://maven.pkg.github.com/dendrocyte/haylocation"
          credentials {
                username = "dendrocyte"
                password = localProps.getProperty("github.haylocation.token")
            }
        }
    }
}
```
`local.properties`
```
github.haylocation.token=<YOUR_PERSONAL_ACCESS_TOKEN>
```
`build.gradle`
```
implementation 'com.dendrocyte:haylocation:1.0.5'
```

#### III. Add Permission on AndroidManifest first
```
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    
</manifest>    
```

#### IV. Declare this View on xml! That is it!
```
<com.dendrocyte.haylocation.module.customView.UtilBtn
        android:id="@+id/btn_cuslocation"
        android:layout_width="100dp"
        android:layout_height="50dp"        
        />
```

| attr                        | description                                                               | values                                                                                    |
|:----------------------------|:--------------------------------------------------------------------------|:------------------------------------------------------------------------------------------|
| app:locationMethod          | get location by Location / Address <br/> once / frequently                | getLastLocation (default)<br/>getLastAddress<br/>GetLocationUpdates<br/>GetAddressUpdates |
| app:intervalMillis          | configure LocationRequest (must-have)                                     | [0, Long.MAX_VALUE], 60 * 60 * 1000 (default)                                             |
| app:maxUpdates              | configure LocationRequest about maximum times to update location          | [1, Long.MAX_VALUE], Integer.MAX_VALUE (default)                                          |
| app:maxUpdateAgeMillis      | configure LocationRequest                                                 | [0, Long.MAX_VALUE], -1L (default)                                                        |
| app:minUpdateIntervalMillis | configure LocationRequest about minimum interval(ms) to update            | [0, Long.MAX_VALUE], 10 * 60 * 1000 (default)                                             |
| app:minUpdateDistanceMeters | configure LocationRequest about minimum distance(m) to update             | [0, Float.MAX_VALUE], 0f (default)                                                        |
| app:waitForAccurateLocation | configure LocationRequest                                                 | true, false (default)                                                                     |
| app:granularity             | configure LocationRequest about location range                            | permissionLevel (default)<br/>coarse<br/>fine                                             |
| app:priority                | configure LocationRequest about location accuracy                         | highAccuracy (default)<br/>balancedPowerAccuracy<br/>lowPowerAccuracy                     |                                               |
| app:durationMillis          | configure LocationRequest                                                 | Long.MAX_VALUE(default)                                                                   |
| app:maxUpdateDelayMillis    | configure LocationRequest                                                 | 0L (default)                                                                              |
| app:alwaysShow              | configure LocationSettingRequest                                          | true, false (default)                                                                     |

<br><br>
### Other declaration way
Of course, apart from the XML declaration method, there are also delegator and worker usage methods.
For more details, please refer to the [demo folder](https://github.com/dendrocyte/HayLocation/tree/main/app/src/main/java/com/dendrocyte/haylocation/demo)
<br><br>
#### (1) Delegator:
If you prefer to use only methods or want to decouple from the view, you can use the delegator approach.

```
 /**
* declare delegator before onCreate()
* let ActivityResultLauncher register first
*/
  private val delegator = UtilDelegator(this)

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
            attach(baseContext).configure().start()
        }

    }

    override fun onStop() {
        super.onStop()
        delegator.release()
    }
```

#### (2) Worker:
If you want to use WorkManager, you can adopt the worker approach.
See `*Worker.class` at demo folder
