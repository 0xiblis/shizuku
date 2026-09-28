package moe.shizuku.manager.wifi

import android.annotation.SuppressLint
import android.content.AttributionSource
import android.net.wifi.WifiConfiguration
import android.os.Build
import android.os.Bundle
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object WifiManager {

    @SuppressLint("PrivateApi")
    fun getSavedNetworks(): List<WifiConfiguration> {
        val base = Class.forName("android.net.wifi.IWifiManager")
        val stub = Class.forName("android.net.wifi.IWifiManager\$Stub")

        val asInterface = stub.getMethod(
            "asInterface",
            android.os.IBinder::class.java
        )

        val iwm = asInterface.invoke(
            null,
            ShizukuBinderWrapper(
                SystemServiceHelper.getSystemService("wifi")
            )
        )

        val packageName = when (Shizuku.getUid()) {
            0 -> "android"
            1000 -> "android"
            2000 -> "com.android.shell"
            else -> throw IllegalArgumentException(
                "Unknown Shizuku uid ${Shizuku.getUid()}"
            )
        }

        val privilegedConfigs = if (
            Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2
        ) {
            val method = base.getMethod(
                "getPrivilegedConfiguredNetworks",
                String::class.java,
                String::class.java,
                Bundle::class.java
            )

            val attribution = AttributionSource::class.java
                .getConstructor(
                    Int::class.java,
                    String::class.java,
                    String::class.java,
                    Set::class.java,
                    AttributionSource::class.java
                )
                .newInstance(
                    Shizuku.getUid(),
                    packageName,
                    null,
                    null,
                    null
                )

            val extras = Bundle().apply {
                putParcelable(
                    "EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE",
                    attribution
                )
            }

            method.invoke(
                iwm,
                packageName,
                null,
                extras
            )
        } else {
            try {
                val method = base.getMethod(
                    "getPrivilegedConfiguredNetworks",
                    String::class.java,
                    String::class.java
                )

                method.invoke(
                    iwm,
                    packageName,
                    null
                )
            } catch (_: NoSuchMethodException) {
                val method = base.getMethod(
                    "getPrivilegedConfiguredNetworks",
                    String::class.java,
                    String::class.java,
                    Bundle::class.java
                )

                method.invoke(
                    iwm,
                    packageName,
                    null,
                    null
                )
            }
        }

        @Suppress("UNCHECKED_CAST")
        return privilegedConfigs?.let {
            it::class.java
                .getMethod("getList")
                .invoke(it) as List<WifiConfiguration>
        } ?: emptyList()
    }
}
