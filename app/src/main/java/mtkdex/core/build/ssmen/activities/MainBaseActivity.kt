package mtkdex.core.build.ssmen.activities

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.MainApplication
import com.v2ray.ang.R
import com.v2ray.ang.handler.AngConfigManager.importBatchConfig
import com.v2ray.ang.handler.AngConfigManager.importCustomizeConfig
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.V2RayServiceManager
import com.v2ray.ang.util.Utils
import com.v2ray.ang.viewmodel.MainViewModel
import com.v2ray.ang.viewmodel.SettingsViewModel
import io.michaelrocks.paranoid.Obfuscate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mtkdex.core.build.ssmen.config.ConfigDataBase
import mtkdex.core.build.ssmen.config.ConfigUtil
import mtkdex.core.build.ssmen.config.SettingsConstants
import mtkdex.core.build.ssmen.config.SettingsConstants.SERVER_POSITION
import mtkdex.core.build.ssmen.connectivity.DeviceStateReceiver
import mtkdex.core.build.ssmen.logger.hLogStatus
import mtkdex.core.build.ssmen.service.dex002
import mtkdex.core.build.ssmen.service.dex002.InjectorListener
import mtkdex.core.build.ssmen.service.dex002.MyBinder
import mtkdex.core.build.ssmen.service.dex003
import mtkdex.core.build.ssmen.service.dex003.ConnectionStats
import mtkdex.core.build.ssmen.service.dex003.EventMsg
import mtkdex.core.build.ssmen.service.dex003.LogMsg
import mtkdex.core.build.ssmen.service.dex003.ProfileList
import mtkdex.core.build.ssmen.utils.PrefUtil
import mtkdex.core.build.ssmen.utils.c_01
import mtkdex.core.build.ssmen.utils.util
import mtkdex.core.build.ssmen.view.StatisticGraphData
import mtkdex.core.build.ssmen.view.StatisticGraphData.DataTransferStats
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.Random

@Obfuscate
abstract class MainBaseActivity : AppCompatActivity(), SettingsConstants, InjectorListener,
    dex003.EventReceiver {

    private val settingsStorage by lazy {
        MMKV.mmkvWithID(
            MmkvManager.ID_SETTING,
            MMKV.MULTI_PROCESS_MODE
        )
    }
    private val mainStorage by lazy {
        MMKV.mmkvWithID(
            MmkvManager.ID_MAIN,
            MMKV.MULTI_PROCESS_MODE
        )
    }
    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    protected var pref: SharedPreferences? = null
    @JvmField protected var securePref: SharedPreferences? = null
    protected var editor: SharedPreferences.Editor? = null
    @JvmField protected var secureEditor: SharedPreferences.Editor? = null
    protected var dPrefs: SharedPreferences? = null
    protected var dEditor: SharedPreferences.Editor? = null
    private var injector: dex002? = null
    protected var config: ConfigUtil? = null
    private var mBoundService: dex003? = null
    protected var serverData: ConfigDataBase? = null
    protected var networkData: ConfigDataBase? = null
    protected var upDateBytes: DataTransferStats? = null
    val isRunning by lazy { MutableLiveData<Boolean>() }
    val updateTestResultAction by lazy { MutableLiveData<String>() }
    private var mDeviceStateReceiver: DeviceStateReceiver? = null

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            this@MainBaseActivity.mBoundService = (service as dex003.LocalBinder).service
            Log.d(TAG, "CLIBASE: onServiceConnected: " + mBoundService.toString())
            mBoundService!!.client_attach(this@MainBaseActivity)
            this@MainBaseActivity.post_bind()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.d(TAG, "CLIBASE: onServiceDisconnected")
            this@MainBaseActivity.mBoundService = null
        }
    }
    private val mInjectorConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p1: ComponentName, p2: IBinder) {
            injector = (p2 as MyBinder).service
            injector!!.setInjectorListener(this@MainBaseActivity)
        }

        override fun onServiceDisconnected(p1: ComponentName) {
            injector = null
        }
    }

    @Suppress("Deprecation")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val builder = ThreadPolicy.Builder()
        StrictMode.setThreadPolicy(builder.permitAll().build())
        pref = MainApplication.getPrivateSharedPreferences()
        securePref = mtkdex.core.build.ssmen.utils.SecurePrefUtil.getEncryptedPrefs(this)
        editor = pref!!.edit()
        secureEditor = securePref!!.edit()
        dPrefs = MainApplication.getDefaultSharedPreferences()
        dEditor = dPrefs!!.edit()
        
        migrateToSecurePrefs()

        //getSettingsStorage()
        config = ConfigUtil.getInstance(this@MainBaseActivity)
        serverData = ConfigDataBase(this@MainBaseActivity, "mServerData")
        networkData = ConfigDataBase(this@MainBaseActivity, "mNetwrokData")
        upDateBytes = StatisticGraphData.getStatisticData().dataTransferStats
        setTheme(config!!.getAppThemeDark())
        window.statusBarColor = if (config!!.appThemeUtil) ResourcesCompat.getColor(
            resources,
            R.color.windowBackground_dark,
            null
        ) else ResourcesCompat.getColor(resources, R.color.window_nav_light, null)
        window.navigationBarColor = if (config!!.appThemeUtil) ResourcesCompat.getColor(
            resources,
            R.color.windowBackground_dark,
            null
        ) else ResourcesCompat.getColor(resources, R.color.window_nav_light, null)
    }


    protected fun doBindService() {
        bindService(
            Intent(this, dex003::class.java).setAction(dex003.ACTION_BIND),
            this.mConnection,
            BIND_AUTO_CREATE
        )
        bindService(Intent(this, dex002::class.java), mInjectorConnection, BIND_AUTO_CREATE)
        register_connectivity_receiver()
    }

    protected fun doUnbindService() {
        Log.d(TAG, "CLIBASE: doUnbindService")
        if (this.mBoundService != null) {
            unbindService(this.mConnection)
            this.mBoundService = null
        }
        if (injector != null) {
            unbindService(mInjectorConnection)
            injector = null
        }
        unregister_connectivity_receiver()
    }

    override fun startOpenVPN() {
        // TODO: Implement this method
    }

    protected fun get_connection_stats(): ConnectionStats? {
        if (this.mBoundService != null) {
            return mBoundService!!._connection_stats
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("DefaultLocale")
    protected fun get_gui_version(name: String): String {
        var versionName = "1.0"
        var versionCode = 1
        return String.format("%s %s-%d", name, versionName, versionCode)
    }


    protected fun submitConnectIntent(
        profile_name: String,
        server: String?,
        vpn_proto: String?,
        ipv6: String?,
        conn_timeout: String?,
        username: String?,
        password: String?,
        cache_password: Boolean,
        pk_password: String?,
        response: String?,
        epki_alias: String?,
        compression_mode: String?,
        proxy_name: String?,
        proxy_username: String?,
        proxy_password: String?,
        proxy_allow_creds_dialog: Boolean,
        gui_version: String?
    ) {
        val prefix = dex003.INTENT_PREFIX
        val intent = Intent(this, dex003::class.java).setAction(dex003.ACTION_CONNECT)
            .putExtra("$prefix.PROFILE", profile_name)
            .putExtra("$prefix.GUI_VERSION", gui_version)
            .putExtra("$prefix.PROXY_NAME", proxy_name)
            .putExtra("$prefix.PROXY_USERNAME", proxy_username)
            .putExtra("$prefix.PROXY_PASSWORD", proxy_password)
            .putExtra("$prefix.PROXY_ALLOW_CREDS_DIALOG", proxy_allow_creds_dialog)
            .putExtra("$prefix.SERVER", server)
            .putExtra("$prefix.PROTO", vpn_proto)
            .putExtra("$prefix.IPv6", ipv6)
            .putExtra("$prefix.CONN_TIMEOUT", conn_timeout)
            .putExtra("$prefix.USERNAME", username)
            .putExtra("$prefix.PASSWORD", password)
            .putExtra("$prefix.CACHE_PASSWORD", cache_password)
            .putExtra("$prefix.PK_PASSWORD", pk_password)
            .putExtra("$prefix.RESPONSE", response).putExtra("$prefix.EPKI_ALIAS", epki_alias)
            .putExtra("$prefix.COMPRESSION_MODE", compression_mode)
        if (this.mBoundService != null) {
            mBoundService!!.client_attach(this)
        }
        startService(intent)
        Log.d(TAG, "CLI: submitConnectIntent: $profile_name")
    }

    protected fun submitDisconnectIntent() {
        if (hLogStatus.isTunnelActive()) {
            startService(
                Intent(
                    this@MainBaseActivity,
                    dex002::class.java
                ).setAction(dex002.STOP_SERVICE)
                    .putExtra("stateSTOP_SERVICE", resString(R.string.state_disconnected))
            )
        }
    }

    protected fun submitReloadProfileIntent(profile_content: String) {
        val prefix = dex003.INTENT_PREFIX
        try {
            val intent = Intent(
                this@MainBaseActivity,
                dex003::class.java
            ).setAction(dex003.ACTION_REFRESH_PROFILE).putExtra("$prefix.CONTENT", profile_content)
            
            // Only use startForegroundService if we are actually starting a VPN connection
            // For profile refreshes, startService is sufficient while app is in foreground
            startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainBaseActivity", "Failed to start service: ${e.message}")
        }
    }

    protected fun resolveExternalPkiAlias(prof: dex003.Profile?, next_action: EpkiPost) {
        if (prof == null || !prof.need_external_pki_alias()) {
            next_action.post_dispatch(null)
        } else {
            next_action.post_dispatch("DISABLE_CLIENT_CERT")
        }
    }

    protected fun addlogInfo(msg: String) {
        hLogStatus.logInfo(msg)
    }

    private fun migrateToSecurePrefs() {
        val user = pref?.getString("_screenUsername_key", "") ?: ""
        val pass = pref?.getString("_screenPassword_key", "") ?: ""
        
        if (user.isNotEmpty() || pass.isNotEmpty()) {
            secureEditor?.putString("_screenUsername_key", user)?.apply()
            secureEditor?.putString("_screenPassword_key", pass)?.apply()
            
            // Remove from plain text prefs
            editor?.remove("_screenUsername_key")?.apply()
            editor?.remove("_screenPassword_key")?.apply()
        }
    }

    protected fun getStoredUsername(): String {
        return securePref?.getString("_screenUsername_key", "") ?: ""
    }

    protected fun getStoredPassword(): String {
        return securePref?.getString("_screenPassword_key", "") ?: ""
    }

    protected open fun post_bind() {}
    override fun event(ev: EventMsg) {
        if (ev.name != null) {
            hLogStatus.logInfo("Event: ${ev.name}" + if (ev.info != null) " - ${ev.info}" else "")
        }
    }

    override fun log(lm: LogMsg) {
        if (lm.line != null) {
            hLogStatus.logInfo(lm.line)
        }
    }

    protected fun resString(res_id: Int): String {
        return resources.getString(res_id)
    }

    interface EpkiPost {
        fun post_dispatch(str: String?)
    }

    protected fun get_last_event(): EventMsg? {
        if (this.mBoundService != null) {
            return mBoundService!!._last_event
        }
        return null
    }

    protected fun get_last_event_prof_manage(): EventMsg? {
        if (this.mBoundService != null) {
            return mBoundService!!._last_event_prof_manage
        }
        return null
    }

    protected fun profile_list(): ProfileList? {
        if (this.mBoundService != null) {
            try {
                return mBoundService!!._profile_list
            } catch (_: IOException) {
            }
        }
        return null
    }

    protected fun removeServer(): Boolean {
        return try {
            mainViewModel.clearV2rayServers()
            mainViewModel.reloadServerList()
            true
        } catch (ex: Exception) {
            addlogInfo("<font color = #FF9600>error! $ex")
            false
        }
    }

    protected fun loadServerArrayDragaPosition(): Boolean {
        try {
            val j1 = JSONArray()
            val j2 = JSONArray()
            val j3 = JSONArray()
            val j4 = JSONArray()
            val j5 = JSONArray()
            val j6 = JSONArray()
            val j7 = JSONArray()
            val jarr = JSONArray(serverData!!.data)
            for (i in 0 until jarr.length()) {
                if (jarr.getJSONObject(i).getInt("serverType") == 0) {
                    j1.put(jarr.getJSONObject(i))
                }
                if (jarr.getJSONObject(i).getInt("serverType") == 4) {
                    j2.put(jarr.getJSONObject(i))
                }
                if (jarr.getJSONObject(i).getInt("serverType") == 1) {
                    j3.put(jarr.getJSONObject(i))
                }
                if (jarr.getJSONObject(i).getInt("serverType") == 2) {
                    j4.put(jarr.getJSONObject(i))
                }
                if (jarr.getJSONObject(i).getInt("serverType") == 3) {
                    if (jarr.getJSONObject(i).getInt("V2rayType") == 0) {
                        j5.put(jarr.getJSONObject(i))
                    }
                }
                if (jarr.getJSONObject(i).getInt("serverType") == 3) {
                    if (jarr.getJSONObject(i).getInt("V2rayType") == 1) {
                        j6.put(jarr.getJSONObject(i))
                    }
                }
                if (jarr.getJSONObject(i).getInt("serverType") == 3) {
                    if (jarr.getJSONObject(i).getInt("V2rayType") == 2) {
                        j7.put(jarr.getJSONObject(i))
                    }
                }
            }
            editor!!.putString(SettingsConstants.SERVER_TYPE_V2RAY, j5.toString()).apply()
            editor!!.putString(SettingsConstants.SERVER_TYPE_CDN_V2RAY, j6.toString()).apply()
            editor!!.putString(SettingsConstants.SERVER_TYPE_SSL_V2RAY, j7.toString()).apply()
            editor!!.putString(SettingsConstants.SERVER_TYPE_DNS, j4.toString()).apply()
            editor!!.putString(SettingsConstants.SERVER_TYPE_SSH, j3.toString()).apply()
            editor!!.putString(SettingsConstants.SERVER_TYPE_UDP_HYSTERIA_V1, j2.toString()).apply()
            editor!!.putString(SettingsConstants.SERVER_TYPE_OVPN, j1.toString()).apply()
            return true
        } catch (e: JSONException) {
            util.showToast("Error-2!", e.message)
            return false
        }
    }

    protected fun serverArrayDragaPosition(): JSONArray {
        try {
            val jar = JSONArray()
            if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 0) {
                val jarr1 = JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_OVPN, "[]"))
                for (i in 0 until jarr1.length()) {
                    if (jarr1.getJSONObject(i).getInt("serverType") == 0) {
                        jar.put(jarr1.getJSONObject(i))
                    }
                }
            } else if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 2) {
                val jarr2 = JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_SSH, "[]"))
                for (i in 0 until jarr2.length()) {
                    if (jarr2.getJSONObject(i).getInt("serverType") == 1) {
                        jar.put(jarr2.getJSONObject(i))
                    }
                }
            } else if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 3) {
                val jarr3 = JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_DNS, "[]"))
                for (i in 0 until jarr3.length()) {
                    if (jarr3.getJSONObject(i).getInt("serverType") == 2) {
                        jar.put(jarr3.getJSONObject(i))
                    }
                }
            } else if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 1) {
                val jarr4 =
                    JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_UDP_HYSTERIA_V1, "[]"))
                for (i in 0 until jarr4.length()) {
                    if (jarr4.getJSONObject(i).getInt("serverType") == 4) {
                        jar.put(jarr4.getJSONObject(i))
                    }
                }
            } else if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 4) {
                val jarr5 = JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_V2RAY, "[]"))
                for (i in 0 until jarr5.length()) {
                    if (jarr5.getJSONObject(i).getInt("serverType") == 3) {
                        if (jarr5.getJSONObject(i).getInt("V2rayType") == 0) {
                            jar.put(jarr5.getJSONObject(i))
                        }
                    }
                }
            } else if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 5) {
                val jarr6 =
                    JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_CDN_V2RAY, "[]"))
                for (i in 0 until jarr6.length()) {
                    if (jarr6.getJSONObject(i).getInt("serverType") == 3) {
                        if (jarr6.getJSONObject(i).getInt("V2rayType") == 1) {
                            jar.put(jarr6.getJSONObject(i))
                        }
                    }
                }
            } else if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 6) {
                val jarr7 =
                    JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_SSL_V2RAY, "[]"))
                for (i in 0 until jarr7.length()) {
                    if (jarr7.getJSONObject(i).getInt("serverType") == 3) {
                        if (jarr7.getJSONObject(i).getInt("V2rayType") == 2) {
                            jar.put(jarr7.getJSONObject(i))
                        }
                    }
                }
            }
            if (jar.length() >= 2) {
                editor!!.putBoolean("show_random_layout", true).apply()
            } else {
                editor!!.putBoolean("show_random_layout", false).apply()
            }
            return jar
        } catch (e: JSONException) {
            editor!!.putBoolean("isRandom", false).apply()
            editor!!.putBoolean("show_random_layout", false).apply()
            util.showToast("Error-3!", e.message)
        }
        return JSONArray("[]")
    }

    protected fun addOrEditedServers(): JSONArray {
        try {
            val jar = JSONArray()
            val jar0 = JSONArray()
            val jarr1 = JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_OVPN, "[]"))
            val jarr2 = JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_SSH, "[]"))
            val jarr3 = JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_DNS, "[]"))
            val jarr4 =
                JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_UDP_HYSTERIA_V1, "[]"))
            val jarr5 = JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_V2RAY, "[]"))
            val jarr6 = JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_CDN_V2RAY, "[]"))
            val jarr7 = JSONArray(pref!!.getString(SettingsConstants.SERVER_TYPE_SSL_V2RAY, "[]"))
            for (i in 0 until jarr1.length()) {
                jar.put(jarr1.getJSONObject(i))
            }
            for (i in 0 until jarr2.length()) {
                jar.put(jarr2.getJSONObject(i))
            }
            for (i in 0 until jarr3.length()) {
                jar.put(jarr3.getJSONObject(i))
            }
            for (i in 0 until jarr5.length()) {
                jar.put(jarr5.getJSONObject(i))
            }
            for (i in 0 until jarr6.length()) {
                jar.put(jarr6.getJSONObject(i))
            }
            for (i in 0 until jarr7.length()) {
                jar.put(jarr7.getJSONObject(i))
            }
            for (i in 0 until jarr4.length()) {
                jar.put(jarr4.getJSONObject(i))
            }
            for (i in 0 until jar.length()) {
                if (jar.getJSONObject(i).has("isAddOrEdited") && jar.getJSONObject(i)
                        .getBoolean("isAddOrEdited")
                ) {
                    jar0.put(jar.getJSONObject(i))
                }
            }
            return jar0
        } catch (e: JSONException) {
            util.showToast("Error-4!", e.message)
        }
        return JSONArray("[]")
    }

    protected fun addOrEditedNetwork(): JSONArray {
        try {
            val jar = JSONArray()
            val jar0 = JSONArray()
            val jarr1 = JSONArray(pref!!.getString(SettingsConstants.LOAD_UDP_TWEAKS_KEY, "[]"))
            val jarr2 = JSONArray(pref!!.getString(SettingsConstants.LOAD_DNS_TWEAKS_KEY, "[]"))
            val jarr3 = JSONArray(pref!!.getString(SettingsConstants.LOAD_V2RAY_TWEAKS_KEY, "[]"))
            val jarr5 =
                JSONArray(pref!!.getString(SettingsConstants.LOAD_V2RAY_CDN_TWEAKS_KEY, "[]"))
            val jarr6 =
                JSONArray(pref!!.getString(SettingsConstants.LOAD_V2RAY_SSL_TWEAKS_KEY, "[]"))
            val jarr4 =
                JSONArray(pref!!.getString(SettingsConstants.LOAD_OVPN_SSH_TWEAKS_KEY, "[]"))
            for (i in 0 until jarr1.length()) {
                jar.put(jarr1.getJSONObject(i))
            }
            for (i in 0 until jarr2.length()) {
                jar.put(jarr2.getJSONObject(i))
            }
            for (i in 0 until jarr3.length()) {
                jar.put(jarr3.getJSONObject(i))
            }
            for (i in 0 until jarr4.length()) {
                jar.put(jarr4.getJSONObject(i))
            }
            for (i in 0 until jarr5.length()) {
                jar.put(jarr5.getJSONObject(i))
            }
            for (i in 0 until jarr6.length()) {
                jar.put(jarr6.getJSONObject(i))
            }
            for (i in 0 until jar.length()) {
                if (jar.getJSONObject(i).has("isAddOrEdited") && jar.getJSONObject(i)
                        .getBoolean("isAddOrEdited")
                ) {
                    jar0.put(jar.getJSONObject(i))
                }
            }
            return jar0
        } catch (e: JSONException) {
            util.showToast("Error-5!", e.message)
        }
        return JSONArray("[]")
    }

    protected fun loadNetworkArrayDragaPosition(): Boolean {
        try {
            val j1 = JSONArray()
            val jarr = JSONArray(networkData!!.data)
            for (i in 0 until jarr.length()) {
                j1.put(jarr.getJSONObject(i))
            }
            editor!!.putString(SettingsConstants.LOAD_ALL_TWEAKS_KEY, j1.toString()).apply()
            return true
        } catch (e: JSONException) {
            util.showToast("Error-12!", e.message)
            return false
        }
    }


    protected fun networkArrayDragaPosition(): JSONArray {
        try {
            val jar = JSONArray()
            val jarr = JSONArray(pref!!.getString(SettingsConstants.LOAD_ALL_TWEAKS_KEY, "[]"))
            for (i in 0 until jarr.length()) {
                jar.put(jarr.getJSONObject(i))
            }
            // jar = JsonUtils.sort(jar, JsonUtils.getComparator(this, "FLAG", 1))
            return jar
        } catch (e: JSONException) {
            util.showToast("Error-13!", e.message)
        }
        return JSONArray("[]")

    }

    private fun getNetworkType(js: JSONObject): String {
        try {
            if (js.getInt("proto_spin") == 0) {
                return "HTTP"
            } else if (js.getInt("proto_spin") == 1) {
                return "UDP"
            } else if (js.getInt("proto_spin") == 2) {
                return "SLOWDNS"
            } else if (js.getInt("proto_spin") == 3) {
                return "SSL"
            } else if (js.getInt("proto_spin") == 4) {
                return "SSL+PAYLOAD"
            } else if (js.getInt("proto_spin") == 5) {
                return "SSL+PROXY"
            } else if (js.getInt("proto_spin") == 6) {
                return "V2ray/Xray"
            }
        } catch (e: Exception) {
            util.showToast("getNetworkType", e.message)
        }
        return ""
    }

    private fun getServerType(sjs: JSONObject, pjs: JSONObject): String? {
        try {
            config!!.socketTYPE = pjs.getString("server_type")
            if (pjs.getString("server_type") == "cf") {
                return sjs.getString("ServerIP")
            } else if (pjs.getString("server_type") == "ws") {
                return sjs.getString("ServerCloudFront")
            } else if (pjs.getString("server_type") == "http") {
                return sjs.getString("ServerHTTP")
            } else if (pjs.getString("server_type") == "v2ray1") {
                return sjs.getString("ServerV2ray1")
            } else if (pjs.getString("server_type") == "v2ray2") {
                return sjs.getString("ServerV2ray2")
            } else if (pjs.getString("server_type") == "v2ray3") {
                return sjs.getString("ServerV2ray3")
            } else if (pjs.getString("server_type") == "v2ray4") {
                return sjs.getString("ServerV2ray4")
            } else if (pjs.getString("server_type") == "v2ray5") {
                return sjs.getString("ServerV2ray5")
            } else if (pjs.getString("server_type") == "v2ray6") {
                return sjs.getString("ServerV2ray6")
            } else if (pjs.getString("server_type") == "v2ray7") {
                return sjs.getString("ServerV2ray7")
            }
            config!!.socketTYPE = "cf"
            util.showToast("Oppss!", "Server error")
            return null
        } catch (e: Exception) {
            util.showToast("getServerType", e.message)
        }
        return null
    }

    private fun t(): String {
        if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 0) {
            return SettingsConstants.SERVER_TYPE_OVPN
        } else if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 2) {
            return SettingsConstants.SERVER_TYPE_SSH
        } else if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 3) {
            return SettingsConstants.SERVER_TYPE_DNS
        } else if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 4) {
            return SettingsConstants.SERVER_TYPE_V2RAY
        } else if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 5) {
            return SettingsConstants.SERVER_TYPE_V2RAY
        } else if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 6) {
            return SettingsConstants.SERVER_TYPE_V2RAY
        } else if (pref!!.getInt(SettingsConstants.manual_tunnel_radio_key, 0) == 1) {
            return SettingsConstants.SERVER_TYPE_UDP_HYSTERIA_V1
        }
        return SettingsConstants.SERVER_TYPE_OVPN
    }

    protected fun reLoad_Configs(): Boolean {
        try {
            val mRandomServerIndex: Int
            val jarr1 = serverArrayDragaPosition()
            val jarr2 = networkArrayDragaPosition()
            if (jarr1.length() == 0 || jarr2.length() == 0) {
                editor!!.putBoolean("isRandom", false).apply()
                return false
            }
            mRandomServerIndex = if (pref!!.getBoolean("isRandom", false)) {
                Random().nextInt(jarr1.length())
            } else {
                pref!!.getInt(SettingsConstants.SERVER_POSITION, 0)
            }
            val s_js = jarr1.getJSONObject(mRandomServerIndex)
            val p_js = jarr2.getJSONObject(pref!!.getInt(SettingsConstants.NETWORK_POSITION, 0))
            val serType = t()
            val netType = getNetworkType(p_js)
            config!!.serverType = serType
            val mHost = getServerType(s_js, p_js)
            config!!.setIsQueryMode(false)
            //editor!!.putString("Network_info", "").apply()
            editor!!.putString(
                "Server_message",
                if (s_js.has("Server_msg")) s_js.getString("Server_msg") else ""
            ).apply()
            editor!!.putBoolean(SettingsConstants.CONFIG_EXP_KEY, isConfigXpired(s_js)).apply()
            if (serType == SettingsConstants.SERVER_TYPE_UDP_HYSTERIA_V1) {
                if (mHost!!.isEmpty()) return false
                config!!.serverName = s_js.getString("Name")
                config!!.setServerHost(mHost)
                config!!.setUDPConfig(p_js.getString("NetworkPayload"))
                config!!.configIsAutoLogIn = s_js.getBoolean("AutoLogIn")
                if (s_js.getBoolean("AutoLogIn")) {
                    config!!.setUser(s_js.getString("Username"))
                    config!!.setUserPass(s_js.getString("Password"))
                } else {
                    val user = getStoredUsername()
                    val pass = getStoredPassword()
                    config!!.setUser(if (user.isEmpty()) "" else c_01.encrypt(user))
                    config!!.setUserPass(if (pass.isEmpty()) "" else c_01.encrypt(pass))
                }
                editor!!.putString(
                    SettingsConstants.SERVER_WEB_RENEW_KEY,
                    if (s_js.has("server_web_renew")) s_js.getString("server_web_renew") else ""
                ).apply()
                editor!!.putString("IPHunter_pName", p_js.getString("Name")).apply()
                //if (p_js.has("Info") && p_js.getString("Info").isNotEmpty()) {
                //    editor!!.putString("Network_info", p_js.getString("Info")).apply()
                //}
                config!!.payloadName = p_js.getString("Name")
                return true
            }
            if (serType == SettingsConstants.SERVER_TYPE_V2RAY) {

                config!!.serverName = s_js.getString("Name")
                val servertype = config!!.socketTYPE
                if (servertype.equals("cf")) {
                    config!!.setputv2(s_js.getString("ServerIP"))
                } else if (servertype.equals("ws")) {
                    config!!.setputv2(s_js.getString("ServerCloudFront"))
                } else if (servertype.equals("http")) {
                    config!!.setputv2(s_js.getString("ServerHTTP"))
                } else if (servertype.equals("v2ray1")) {
                    config!!.setputv2(s_js.getString("ServerV2ray1"))
                } else if (servertype.equals("v2ray2")) {
                    config!!.setputv2(s_js.getString("ServerV2ray2"))
                } else if (servertype.equals("v2ray3")) {
                    config!!.setputv2(s_js.getString("ServerV2ray3"))
                } else if (servertype.equals("v2ray4")) {
                    config!!.setputv2(s_js.getString("ServerV2ray4"))
                } else if (servertype.equals("v2ray5")) {
                    config!!.setputv2(s_js.getString("ServerV2ray5"))
                } else if (servertype.equals("v2ray6")) {
                    config!!.setputv2(s_js.getString("ServerV2ray6"))
                } else if (servertype.equals("v2ray7")) {
                    config!!.setputv2(s_js.getString("ServerV2ray7"))
                }
                var useraccount = ""
                var passaccount = ""
                config!!.configIsAutoLogIn = s_js.getBoolean("AutoLogIn")
                if (s_js.getBoolean("AutoLogIn")) {
                    useraccount = s_js.getString("Username")
                    passaccount = s_js.getString("Password")
                } else {
                    useraccount = getStoredUsername()
                    passaccount = getStoredPassword()
                }
                val nana = c_01.decrypt(config!!.getputv2())

                if (nana.contains("[account]")) {
                    config!!.setConfigV2ray(
                        c_01.encrypt(
                            nana.replace(
                                "[account]",
                                String.format("%s-mtkadmindex-%s", useraccount, passaccount)
                            )
                        )
                    )
                } else {
                    config!!.setConfigV2ray(c_01.encrypt(nana))
                }
                config!!.configIsAutoLogIn = s_js.getBoolean("AutoLogIn")
                if (s_js.getBoolean("AutoLogIn")) {
                    config!!.setUser(s_js.getString("Username"))
                    config!!.setUserPass(s_js.getString("Password"))
                } else {
                    val user = getStoredUsername()
                    val pass = getStoredPassword()
                    config!!.setUser(if (user.isEmpty()) "" else c_01.encrypt(user))
                    config!!.setUserPass(if (pass.isEmpty()) "" else c_01.encrypt(pass))
                }
                config!!.payloadName = p_js.getString("Name")
                editor!!.putString(
                    SettingsConstants.SERVER_WEB_RENEW_KEY,
                    if (s_js.has("server_web_renew")) s_js.getString("server_web_renew") else ""
                ).apply()
                return true
            }
            if (serType == SettingsConstants.SERVER_TYPE_DNS) {
                config!!.serverName = s_js.getString("Name")
                config!!.setServerHost(c_01.encrypt("127.0.0.1"))
                config!!.setServerPort("2222")
                config!!.setDNSpublicKey(s_js.getString("ServerCloudFront"))
                config!!.setDNSnameServer(s_js.getString("ServerIP"))
                config!!.setDNSaddress(p_js.getString("NetworkPayload"))
                config!!.configIsAutoLogIn = s_js.getBoolean("AutoLogIn")
                if (s_js.getBoolean("AutoLogIn")) {
                    config!!.setUser(s_js.getString("Username"))
                    config!!.setUserPass(s_js.getString("Password"))
                } else {
                    val user = getStoredUsername()
                    val pass = getStoredPassword()
                    config!!.setUser(if (user.isEmpty()) "" else c_01.encrypt(user))
                    config!!.setUserPass(if (pass.isEmpty()) "" else c_01.encrypt(pass))
                }
                editor!!.putString("IPHunter_pName", p_js.getString("Name")).apply()
                //if (p_js.has("Info") && p_js.getString("Info").isNotEmpty()) {
                //    editor!!.putString("Network_info", p_js.getString("Info")).apply()
                //}
                config!!.payloadName = p_js.getString("Name")
                editor!!.putString(
                    SettingsConstants.SERVER_WEB_RENEW_KEY,
                    if (s_js.has("server_web_renew")) s_js.getString("server_web_renew") else ""
                ).apply()
                return true
            }
            if (serType == SettingsConstants.SERVER_TYPE_OVPN) {
                if (s_js.has("MultiCert")) {
                    config!!.ovpnCert =
                        if (s_js.getBoolean("MultiCert")) s_js.getString("ovpnCertificate") else pref!!.getString(
                            SettingsConstants.OPEN_VPN_CERT,
                            ""
                        )
                } else {
                    config!!.ovpnCert = pref!!.getString(SettingsConstants.OPEN_VPN_CERT, "")
                }
            }
            if (netType == "HTTP") {
                if (mHost!!.isEmpty()) return false
                val useDefProxy: Boolean = p_js.getBoolean("UseDefProxy")
                val serverProxyHost: String =
                    if (generateServerProxy(s_js)) s_js.getString("ProxyHost") else mHost
                val serverProxyPort: String =
                    if (!generateServerProxyPort(s_js)) s_js.getString("ProxyPort") else p_js.getString(
                        "SquidPort"
                    ).ifEmpty { "80" }
                val proxy: String = p_js.getString("SquidProxy").ifEmpty { serverProxyHost }
                val SquidProxy: String = if (useDefProxy) serverProxyHost else proxy
                val front_query = p_js.getString("NetworkFrontQuery")
                val back_query = p_js.getString("NetworkBackQuery")
                val port = p_js.getString("SquidPort").ifEmpty { serverProxyPort }
                val SquidPort = if (useDefProxy) serverProxyPort else port
                config!!.setPaylodType(SettingsConstants.PAYLOAD_TYPE_HTTP_PROXY)
                config!!.autoReplace =
                    if (p_js.has("AutoReplace")) p_js.getBoolean("AutoReplace") else false
                config!!.setServerHost(mHost)
                config!!.setProxyHost(SquidProxy)
                config!!.setPayload(p_js.getString("NetworkPayload"))
                if (front_query.isEmpty() && back_query.isEmpty()) {
                    config!!.setIsQueryMode(false)
                } else if (back_query.isNotEmpty()) {
                    config!!.setIsQueryMode(true)
                    config!!.setBackQuery(back_query)
                    config!!.setFrontQuery("")
                } else if (front_query.isNotEmpty()) {
                    config!!.setIsQueryMode(true)
                    config!!.setFrontQuery(front_query)
                    config!!.setBackQuery("")
                }
                config!!.setProxyPort(SquidPort)
                if (s_js.getString("TcpPort").contains(":")) {
                    val split = s_js.getString("TcpPort").split(":")[0]
                    config!!.setServerPort(split)
                } else {
                    config!!.setServerPort(s_js.getString("TcpPort"))
                }
                if (p_js.getString("Name").contains("Direct") || p_js.getString("Name")
                        .contains("direct")
                ) {
                    if (serType == SettingsConstants.SERVER_TYPE_OVPN) {
                        if (p_js.getString("NetworkPayload").isEmpty()) {
                            config!!.setPaylodType(SettingsConstants.PAYLOAD_TYPE_DIRECT)
                        } else {
                            config!!.setPaylodType(SettingsConstants.PAYLOAD_TYPE_DIRECT_PAYLOAD)
                        }
                    }
                    if (serType == SettingsConstants.SERVER_TYPE_SSH) {
                        if (p_js.getString("NetworkPayload").isEmpty()) {
                            config!!.setPaylodType(SettingsConstants.PAYLOAD_TYPE_DIRECT)
                        } else {
                            config!!.setPaylodType(SettingsConstants.PAYLOAD_TYPE_DIRECT_PAYLOAD)
                            if (s_js.getString("TcpPort").contains(":")) {
                                val split = s_js.getString("TcpPort").split(":")[1]
                                config!!.setServerPort(split)
                            } else {
                                config!!.setServerPort(s_js.getString("TcpPort"))
                            }
                        }
                    }
                }
            }
            if (netType == "SSL") {
                if (mHost!!.isEmpty()) return false
                val sslPort = p_js.getString("SSLPort").ifEmpty { s_js.getString("SSLPort") }
                config!!.setPaylodType(SettingsConstants.PAYLOAD_TYPE_SSL)
                config!!.setSni(p_js.getString("SSLSNI"))
                config!!.setServerHost(mHost)
                config!!.setServerPort(sslPort)
            }
            if (netType == "SSL+PAYLOAD") {
                if (mHost!!.isEmpty()) return false
                config!!.setPaylodType(SettingsConstants.PAYLOAD_TYPE_SSL_PAYLOAD)
                val sslPort = p_js.getString("SSLPort").ifEmpty { s_js.getString("SSLPort") }
                config!!.setSni(p_js.getString("SSLSNI"))
                config!!.setPayload(p_js.getString("NetworkPayload"))
                config!!.setServerHost(mHost)
                config!!.setServerPort(sslPort)
            }
            if (netType == "SSL+PROXY") {
                if (mHost!!.isEmpty()) return false
                config!!.setPaylodType(SettingsConstants.PAYLOAD_TYPE_SSL_PROXY)
                val useDefProxy: Boolean = p_js.getBoolean("UseDefProxy")
                val serverProxyHost: String =
                    if (generateServerProxy(s_js)) s_js.getString("ProxyHost") else mHost
                val serverProxyPort: String =
                    if (generateServerProxyPort(s_js)) s_js.getString("ProxyPort") else p_js.getString(
                        "SquidPort"
                    ).ifEmpty { "80" }
                val proxy: String = p_js.getString("SquidProxy").ifEmpty { serverProxyHost }
                val SquidProxy: String = if (useDefProxy) serverProxyHost else proxy
                val port = p_js.getString("SquidPort").ifEmpty { serverProxyPort }
                val SquidPort = if (useDefProxy) serverProxyPort else port
                val sslPort = p_js.getString("SSLPort").ifEmpty { s_js.getString("SSLPort") }
                config!!.setSni(p_js.getString("SSLSNI"))
                config!!.setPayload(p_js.getString("NetworkPayload"))
                config!!.setServerHost(mHost)
                config!!.setServerPort(sslPort)
                config!!.setProxyHost(SquidProxy)
                config!!.setProxyPort(SquidPort)
                config!!.autoReplace =
                    if (p_js.has("AutoReplace")) p_js.getBoolean("AutoReplace") else false
            }
            if (serType == SettingsConstants.SERVER_TYPE_OVPN) {
                if (config!!.ovpnCert.contains("http-proxy-option")) {
                    config!!.setServerPort(
                        if (s_js.getString("TcpPort").contains(":")) s_js.getString("TcpPort")
                            .split(":")[0] else s_js.getString("TcpPort")
                    )
                }
                if (config!!.ovpnCert.contains("proto udp")) {
                    if (mHost!!.isEmpty()) return false
                    config!!.setPaylodType(SettingsConstants.PAYLOAD_TYPE_OVPN_UDP)
                    config!!.setServerHost(mHost)
                    if (s_js.getString("TcpPort").contains(":")) {
                        val port = s_js.getString("TcpPort").split(":")[1]
                        config!!.setServerPort(port)
                    } else {
                        config!!.setServerPort("53")
                    }
                }
            }
            config!!.serverName = s_js.getString("Name")
            config!!.payloadName = p_js.getString("Name")
            if (s_js.getBoolean("AutoLogIn")) {
                config!!.setUser(s_js.getString("Username"))
                config!!.setUserPass(s_js.getString("Password"))
            } else {
                val user = getStoredUsername()
                val pass = getStoredPassword()
                config!!.setUser(if (user.isEmpty()) "" else c_01.encrypt(user))
                config!!.setUserPass(if (pass.isEmpty()) "" else c_01.encrypt(pass))
            }
            editor!!.putString(
                SettingsConstants.SERVER_WEB_RENEW_KEY,
                if (s_js.has("server_web_renew")) s_js.getString("server_web_renew") else ""
            ).apply()
            editor!!.putString("IPHunter_pName", p_js.getString("Name")).apply()
            config!!.configIsAutoLogIn = s_js.getBoolean("AutoLogIn")
            //if (p_js.has("Info") && !p_js.getString("Info").isEmpty()) {
            //    editor!!.putString("Network_info", p_js.getString("Info")).apply()
            //}
            return true
        } catch (e: Exception) {
            //editor!!.putBoolean("isRandom", false).apply
            editor?.putInt(SERVER_POSITION, 0)
            //  editor?.putInt(NETWORK_POSITION,0)
            // util.showToast("Error-7!", e.message)

            return false
        }
    }

    private fun generateServerProxy(js: JSONObject): Boolean {
        try {
            if (js.has("ProxyHost")) {
                return c_01.decrypt(js.getString("ProxyHost")).isNotEmpty()
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    private fun generateServerProxyPort(js: JSONObject): Boolean {
        try {
            if (js.has("ProxyPort")) {
                return js.getString("ProxyPort").isNotEmpty()
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }

    private fun isConfigXpired(js: JSONObject): Boolean {
        try {
            if (js.has("Server_exp_box")) {
                val mValidade: Long = js.getString("Server_exp").split("-")[0].toLong()
                if (mValidade > 0 && isValidadeExpirou(mValidade)) {
                    return true
                }
                return false
            } else {
                return false
            }
        } catch (e: Exception) {
            return false
        }
    }

    private fun isValidadeExpirou(validadeDateMillis: Long): Boolean {
        if (validadeDateMillis == 0L) {
            return false
        }
        val date_atual = Calendar.getInstance().time.time
        if (date_atual >= validadeDateMillis) {
            return true
        }
        return false
    }


    protected fun loadV2rayConfig() {
        try {
            var conFigID = ""

            if (config!!.getSecureString(SettingsConstants.CONFIG_V2RAY_ID).isNotEmpty()) {
                conFigID =
                    c_01.decodeID(c_01.decrypt(config!!.getSecureString(SettingsConstants.CONFIG_V2RAY_ID)))
                        .replace("[v2_id]", conFigID)
            }
            var conFigSNI = ""
            if (config!!.getSecureString(SettingsConstants.SNI_V2RAY_KEY).isNotEmpty()) {
                conFigSNI = c_01.decrypt(config!!.getSecureString(SettingsConstants.SNI_V2RAY_KEY))
            }

            var conFig = c_01.decrypt(config!!.getSecureString(SettingsConstants.CONFIG_V2RAY))
                .replace("[v2_id]", conFigID).replace("[v2_host]", conFigSNI)
            // Swap shared UUID with user's personal xray UUID
            val userXrayUUID = securePref?.getString("_xray_uuid_key", "") ?: ""
            if (userXrayUUID.isNotEmpty() && conFig.contains("vless://")) {
                conFig = conFig.replace(Regex("(vless://)([^@]+)(@)")) { matchResult ->
                    "${matchResult.groupValues[1]}$userXrayUUID${matchResult.groupValues[3]}"
                }
            }
            if (!importCustomizeConfig(conFig)) {
                if (!importBatchConfig(conFig)) {
                    addlogInfo("<font color='red'><b>" + "V2RAY config failure!" + "</b>")
                    hLogStatus.updateStateString(
                        hLogStatus.VPN_DISCONNECTED,
                        resString(R.string.state_disconnected)
                    )

                }
            }


        } catch (e: Exception) {
            hLogStatus.updateStateString(
                hLogStatus.VPN_DISCONNECTED,
                getString(R.string.state_disconnected)
            )
            addlogInfo(e.toString())
        }
    }

    private fun copyAssets() {
        val extFolder = Utils.userAssetPath(this@MainBaseActivity)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geo = arrayOf("geosite.dat", "geoip.dat")
                assets.list("")
                    ?.filter { geo.contains(it) }
                    ?.filter { !File(extFolder, it).exists() }
                    ?.forEach {
                        val target = File(extFolder, it)
                        assets.open(it).use { input ->
                            FileOutputStream(target).use { output ->
                                input.copyTo(output)
                            }
                        }
                        hLogStatus.logDebug("Copied from apk assets folder to ${target.absolutePath}")
                    }
            } catch (e: Exception) {
                addlogInfo("asset copy failed! " + e.message)
            }
        }
    }

    protected fun loadV2RaySetups() {
        //  v2rayRegisterUnregisterReceiver(true)
        settingsViewModel.startListenPreferenceChange()
        copyAssets()
    }

    protected fun reloadV2RAY() {
        mainViewModel.reloadServerList()
    }

    protected fun clearAllTestDelay() {
        if (config?.serverType.equals(SettingsConstants.SERVER_TYPE_V2RAY)) {
            mainViewModel.clearAllTestDelay()
        }
    }

    fun stoptV2Ray() {
        mainViewModel.clearAllTestDelay()
        V2RayServiceManager.stopVService(this)

    }


    companion object {
        private const val TAG = "mtk0003"
    }

    protected fun init_default_preferences(oEditor: PrefUtil, vEditor: SharedPreferences.Editor) {
        oEditor.set_string("pref_vpn_proxy_address", "127.0.0.1:8989")
        oEditor.set_string("vpn_proto", "adaptive")
        oEditor.set_string("ipv6", "default")
        oEditor.set_string("conn_timeout", "15")
        oEditor.set_string("compression_mode", "yes")
        oEditor.set_string("tls_version_min_override", "default")
        oEditor.set_boolean("google_dns_fallback", true)
        oEditor.set_boolean("autostart_finish_on_connect", true)
        vEditor.putBoolean(AppConfig.PREF_SPEED_ENABLED, false)
        vEditor.putBoolean(AppConfig.PREF_SNIFFING_ENABLED, true)
        vEditor.putBoolean(AppConfig.PREF_PER_APP_PROXY, false)
        vEditor.putBoolean(AppConfig.PREF_LOCAL_DNS_ENABLED, false)
        vEditor.putBoolean(AppConfig.PREF_FAKE_DNS_ENABLED, false)
        vEditor.putString(AppConfig.PREF_LOCAL_DNS_PORT, AppConfig.PORT_LOCAL_DNS)
        vEditor.putString(AppConfig.PREF_VPN_DNS, "8.8.8.8,8.8.4.4")
        vEditor.putString(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY, "AsIs")
        vEditor.putString(AppConfig.PREF_VPN_INTERFACE_ADDRESS_CONFIG_INDEX, "0")
        vEditor.putBoolean(AppConfig.PREF_PREFER_IPV6, false)
        vEditor.putBoolean(AppConfig.PREF_ALLOW_INSECURE, true)
        vEditor.putString(AppConfig.PREF_SOCKS_PORT, AppConfig.PORT_SOCKS)
        // vEditor.putString(AppConfig.PREF_HTTP_PORT, AppConfig.PORT_HTTP)
        vEditor.putString(AppConfig.PREF_REMOTE_DNS, "8.8.8.8")
        vEditor.putString(AppConfig.PREF_DOMESTIC_DNS, "8.8.8.8")
        vEditor.putString(AppConfig.PREF_LOGLEVEL, "warning")
        vEditor.putString(AppConfig.PREF_MODE, "VPN")
        vEditor.putBoolean(AppConfig.PREF_CONFIRM_REMOVE, false)
        vEditor.putBoolean(AppConfig.PREF_START_SCAN_IMMEDIATE, false)
        vEditor.putString(AppConfig.PREF_LANGUAGE, "auto")
        vEditor.commit()
        settingsStorage.encode(AppConfig.PREF_SPEED_ENABLED, false)
        settingsStorage.encode(AppConfig.PREF_SNIFFING_ENABLED, true)
        settingsStorage.encode(AppConfig.PREF_PER_APP_PROXY, false)
        settingsStorage.encode(AppConfig.PREF_LOCAL_DNS_ENABLED, false)
        settingsStorage.encode(AppConfig.PREF_FAKE_DNS_ENABLED, false)
        settingsStorage.encode(AppConfig.PREF_LOCAL_DNS_PORT, AppConfig.PORT_LOCAL_DNS)
        settingsStorage.encode(AppConfig.PREF_VPN_DNS, "8.8.8.8,8.8.4.4")
        settingsStorage.encode(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY, "AsIs")
        settingsStorage.encode(AppConfig.PREF_VPN_INTERFACE_ADDRESS_CONFIG_INDEX, "0")
        settingsStorage.encode(AppConfig.PREF_PREFER_IPV6, false)
        settingsStorage.encode(AppConfig.PREF_ALLOW_INSECURE, true)
        settingsStorage.encode(AppConfig.PREF_SOCKS_PORT, AppConfig.PORT_SOCKS)
        // settingsStorage.encode(AppConfig.PREF_HTTP_PORT, AppConfig.PORT_HTTP)
        settingsStorage.encode(AppConfig.PREF_REMOTE_DNS, AppConfig.DNS_PROXY)
        settingsStorage.encode(AppConfig.PREF_DOMESTIC_DNS, AppConfig.DNS_DIRECT)
        settingsStorage.encode(AppConfig.PREF_LOGLEVEL, "warning")
        settingsStorage.encode(AppConfig.PREF_MODE, "VPN")
        settingsStorage.encode(AppConfig.PREF_CONFIRM_REMOVE, false)
        settingsStorage.encode(AppConfig.PREF_START_SCAN_IMMEDIATE, false)
        settingsStorage.encode(AppConfig.PREF_LANGUAGE, "auto")
    }


    private fun register_connectivity_receiver() {
        try {
            mDeviceStateReceiver = DeviceStateReceiver(this@MainBaseActivity)
            mDeviceStateReceiver!!.register()
        } catch (ignored: Exception) {
        }
    }

    private fun unregister_connectivity_receiver() {
        try {
            if (mDeviceStateReceiver != null) {
                mDeviceStateReceiver!!.unregister()
            }
        } catch (ignored: Exception) {
        }
    }


}
