package mtkdex.core.build.ssmen.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.v2ray.ang.viewmodel.SettingsViewModel
import com.v2ray.ang.R
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
@Suppress("Deprecation")
class mtk0004 : MainBaseActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var mActivity: AppCompatActivity? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = this@mtk0004
        setContentView(R.layout.v2ray_settings)
        window.statusBarColor = config!!.colorAccent
        val mToolbar:Toolbar = findViewById(R.id.toolbar_main)
        mToolbar.title = resources.getString(R.string.app_name)
        mToolbar.subtitle = "V2Ray Settings"
        mToolbar.setBackgroundColor(config!!.colorAccent)
        mToolbar.setTitleTextColor(if (config!!.appThemeUtil) Color.BLACK else Color.WHITE)
        mToolbar.setSubtitleTextColor(if (config!!.appThemeUtil) Color.BLACK else Color.WHITE)
        mToolbar.setNavigationIcon(if (config!!.appThemeUtil) R.drawable.arrow_d else R.drawable.arrow_l)
        setSupportActionBar(mToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        settingsViewModel.startListenPreferenceChange()
        mToolbar.setNavigationOnClickListener { finish() }
    }
    fun onModeHelpClicked() {
        startActivity(Intent(this@mtk0004, mtk0005::class.java).putExtra("mConfigPanelRenew", config!!.contactUrl))
    }
}
