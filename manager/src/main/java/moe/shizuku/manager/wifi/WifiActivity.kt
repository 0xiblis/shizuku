package moe.shizuku.manager.wifi

import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.utils.ShizukuStateMachine

class WifiActivity : AppBarActivity() {

    private lateinit var adapter: WifiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.wifi_title)

        if (!ShizukuStateMachine.isRunning()) {
            Toast.makeText(
                this,
                R.string.wifi_shizuku_not_running,
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        setContentView(R.layout.wifi_activity)

        val list = findViewById<RecyclerView>(R.id.list)

        adapter = WifiAdapter(this)

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        loadNetworks()
    }

    private fun loadNetworks() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val networks = WifiManager.getSavedNetworks()

                val items = networks
                    .mapNotNull { config ->
                        val ssid = config.SSID
                            ?.removePrefix("\"")
                            ?.removeSuffix("\"")
                            ?.trim()

                        if (ssid.isNullOrEmpty()) {
                            null
                        } else {
                            WifiAdapter.Item(
                                ssid = ssid,
                                password = config.preSharedKey
                                    ?.removePrefix("\"")
                                    ?.removeSuffix("\""),
                                security = getSecurity(config)
                            )
                        }
                    }
                    .groupBy { it.ssid }
                    .map { (_, configs) ->
                        configs.firstOrNull {
                            !it.password.isNullOrEmpty()
                        } ?: configs.first()
                    }
                    .sortedBy { it.ssid.lowercase() }

                withContext(Dispatchers.Main) {
                    adapter.setData(items)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@WifiActivity,
                        e.message ?: "Failed to retrieve WiFi networks",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun getSecurity(
        config: android.net.wifi.WifiConfiguration
    ): String {
        val keyManagement = config.allowedKeyManagement

        return when {
            keyManagement.get(
                android.net.wifi.WifiConfiguration.KeyMgmt.SAE
            ) -> "SAE"

            keyManagement.get(
                android.net.wifi.WifiConfiguration.KeyMgmt.WPA_PSK
            ) -> "WPA_PSK"

            keyManagement.get(
                android.net.wifi.WifiConfiguration.KeyMgmt.OWE
            ) -> "OWE"

            keyManagement.get(
                android.net.wifi.WifiConfiguration.KeyMgmt.WPA_EAP
            ) -> "WPA_EAP"

            keyManagement.get(
                android.net.wifi.WifiConfiguration.KeyMgmt.WPA2_PSK
            ) -> "WPA2_PSK"

            else -> "OPEN"
        }
    }
}
