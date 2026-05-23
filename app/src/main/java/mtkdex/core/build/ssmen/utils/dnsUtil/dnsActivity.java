package mtkdex.core.build.ssmen.utils.dnsUtil;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.v2ray.ang.R;
import mtkdex.core.build.ssmen.activities.MainBaseActivity;
import mtkdex.core.build.ssmen.utils.util;

public class dnsActivity extends MainBaseActivity implements mtkdex.core.build.ssmen.utils.dnsUtil.Response{

    private TextView dnsAnswer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dns_resolver);
        getWindow().setStatusBarColor(getConfig().getColorAccent());
        Toolbar mToolbar = findViewById(R.id.toolbar_main);
        mToolbar.setTitle(resString(R.string.app_name));    
        mToolbar.setSubtitle("DNS Resolver");          
        mToolbar.setBackgroundColor(getConfig().getColorAccent());
        mToolbar.setTitleTextColor(getConfig().getAppThemeUtil()? Color.BLACK:Color.WHITE);
        mToolbar.setSubtitleTextColor(getConfig().getAppThemeUtil()? Color.BLACK:Color.WHITE);
        mToolbar.setNavigationIcon(getConfig().getAppThemeUtil()? R.drawable.arrow_d:R.drawable.arrow_l);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(v -> {
            finish();
        });
        this.dnsAnswer = findViewById(R.id.dnsAnswer);
        this.dnsLookupClick();
    }

    private void dnsLookupClick() {
        final EditText domainElement = findViewById(R.id.ed_domainName);
        final Spinner recordElement = findViewById(R.id.recordSpinner);
        Button dnsLookupButton = findViewById(R.id.dnsLookup);
        ((TextInputLayout)findViewById(R.id.domainName_l)).setBoxStrokeColor(getConfig().getColorAccent());
        findViewById(R.id.dnsLookup_card).setBackgroundTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
        domainElement.setTextColor(getConfig().gettextColor());
        dnsLookupButton.setOnClickListener(view -> {
            String domain = domainElement.getText().toString();
            if (domain.isEmpty() || recordElement.getSelectedItemPosition() == 0) {
                util.showToast(resString(R.string.app_name), "Wrong DNS");
                return;
            }
            Object recordType = recordElement.getSelectedItem();
            if (recordType != null) {
                String recordName = recordType.toString();
                util.showToast(resString(R.string.app_name), "Started");
                new AsyncTask(dnsActivity.this).execute(domain, recordName);
            }
        });
    }

    @Override
    public void processFinish(String output) {
        this.dnsAnswer.setText(output);
    }
}