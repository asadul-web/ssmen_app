package mtkdex.core.build.ssmen.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.CheckBox;
import android.view.View;
import android.widget.EditText;
import com.google.android.material.textfield.TextInputLayout;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.widget.TextView;
import android.util.TypedValue;
import android.text.TextWatcher;
import android.text.Editable;
import com.v2ray.ang.R;
import mtkdex.core.build.ssmen.logger.hLogStatus;
import mtkdex.core.build.ssmen.utils.util;
import mtkdex.core.build.ssmen.view.BufferSizeDialog;

public class mtk0001 extends MainBaseActivity {

    private int i1 = 0;
    private int i2 = 0;
    private int i4 = 0;
    private int i5 = 0;
    private final CheckBox[] mCheckBox = new CheckBox[10];
    private final View[] mView = new View[13];
    private final EditText[] setEditxt = new EditText[9];
    private final TextInputLayout[] seInputLayout = new TextInputLayout[9];    
            
    @Override    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getConfig().getColorAccent());
        setContentView(R.layout.advance_settings);
        Toolbar mToolbar = findViewById(R.id.toolbar_main);
        mToolbar.setTitle(resString(R.string.app_name));
        mToolbar.setSubtitle("Advance Settings");
        mToolbar.setBackgroundColor(getConfig().getColorAccent());
        mToolbar.setTitleTextColor(getConfig().getAppThemeUtil()? Color.BLACK:Color.WHITE);
        mToolbar.setSubtitleTextColor(getConfig().getAppThemeUtil()? Color.BLACK:Color.WHITE);
        mToolbar.setNavigationIcon(getConfig().getAppThemeUtil()? R.drawable.arrow_d:R.drawable.arrow_l);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(v -> mtk0001.this.finish());
        try{
            final int[] view = new int[]{R.id.set1 ,R.id.set2, R.id.set3, R.id.set4 ,R.id.set5, R.id.set6, R.id.set7 ,R.id.set8, R.id.set9, R.id.set10 ,R.id.set11};
            final int[] checkbx = new int[]{R.id.check1 ,R.id.check2, R.id.check3, R.id.check4 ,R.id.check5, R.id.check6, R.id.check7 ,R.id.check8, R.id.check9};
            final int[] setEdLay = new int[]{R.id.ed_ly0, R.id.ed_ly1, R.id.ed_ly2,R.id.ed_ly3 ,R.id.ed_ly4, R.id.ed_ly5, R.id.ed_ly6, R.id.ed_ly7,R.id.ed_ly8};
            final int[] setEdit = new int[]{R.id.set_ed1, R.id.set_ed2, R.id.set_ed3,R.id.set_ed4 ,R.id.set_ed5, R.id.set_ed6, R.id.set_ed7, R.id.set_ed8,R.id.con_time_ed};
            final boolean isRunning = hLogStatus.isTunnelActive();
            for(i4=0;i4<setEditxt.length;i4++){
                setEditxt[i4]=findViewById(setEdit[i4]);
                setEditxt[i4].setEnabled(!isRunning);
                setEditxt[i4].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
            }
            for(i5=0;i5<seInputLayout.length;i5++){
                seInputLayout[i5]=findViewById(setEdLay[i5]);
                seInputLayout[i5].setBoxStrokeColor(getConfig().getColorAccent());
            }
            setEditxt[0].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String txt0 = setEditxt[0].getText().toString().trim();
                    getConfig().setVpnUdpResolver(txt0);
                }
                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            setEditxt[1].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String txt1 = setEditxt[1].getText().toString().trim();
                    String txt2 = setEditxt[2].getText().toString().trim();
                    getConfig().setVpnDnsResolver(txt1+":"+txt2);
                }
                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            setEditxt[2].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String txt1 = setEditxt[1].getText().toString().trim();
                    String txt2 = setEditxt[2].getText().toString().trim();
                    getConfig().setVpnDnsResolver(txt1+":"+txt2);
                }
                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            setEditxt[4].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String txt4 = setEditxt[4].getText().toString().trim();
                    getConfig().setPingServer(txt4);
                }
                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            setEditxt[5].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String txt5 = setEditxt[5].getText().toString().trim();
                    getConfig().setPingThread(Integer.parseInt(txt5.isEmpty()?"3":txt5));
                }
                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            setEditxt[6].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String txt6 = setEditxt[6].getText().toString().trim();
                    getConfig().setProxyAddress(txt6);
                }
                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            setEditxt[7].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String txt7 = setEditxt[7].getText().toString().trim();
                    getConfig().setLocalPort(txt7);
                }
                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            setEditxt[8].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String txt8 = setEditxt[8].getText().toString().trim();
                    getConfig().setReconnTime(Integer.parseInt(txt8.isEmpty()?"5":txt8));
                }
                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            for(i1=0;i1<view.length;i1++){
                mView[i1]=findViewById(view[i1]);
                mView[i1].setOnClickListener(v -> {
                    if (v.getId()==R.id.set2){
                        if(!isRunning)mCheckBox[1].setChecked(getChecked(mCheckBox[1].isChecked()));
                        getConfig().setCompression(mCheckBox[1].isChecked());
                    }
                    else if (v.getId()==R.id.set3){
                        if(!isRunning)mCheckBox[2].setChecked(getChecked(mCheckBox[2].isChecked()));
                        getConfig().setTetheringSubnet(mCheckBox[2].isChecked());
                    }
                    else if (v.getId()==R.id.set4){
                        if(!isRunning)mCheckBox[3].setChecked(getChecked(mCheckBox[3].isChecked()));
                        getConfig().setPowerSaver(mCheckBox[3].isChecked());
                    }
                    else if (v.getId()==R.id.set5){
                        if(!isRunning)mCheckBox[4].setChecked(getChecked(mCheckBox[4].isChecked()));
                        getConfig().setAutoClearLog(mCheckBox[4].isChecked());
                    }
                    else if (v.getId()==R.id.set6){
                        if(!isRunning)mCheckBox[5].setChecked(getChecked(mCheckBox[5].isChecked()));
                        getConfig().setDisabledDelaySSH(mCheckBox[5].isChecked());
                    }
                });
            }
            for(i2=0;i2<checkbx.length;i2++){
                mCheckBox[i2]=findViewById(checkbx[i2]);
                mCheckBox[i2].setOnClickListener(v -> {
                    if (v.getId()==R.id.check2){
                        getConfig().setCompression(mCheckBox[1].isChecked());
                    }
                    else if (v.getId()==R.id.check3){
                        getConfig().setTetheringSubnet(mCheckBox[2].isChecked());
                    }
                    else if (v.getId()==R.id.check4){
                        getConfig().setPowerSaver(mCheckBox[3].isChecked());
                    }
                    else if (v.getId()==R.id.check5){
                        getConfig().setAutoClearLog(mCheckBox[4].isChecked());
                    }
                    else if (v.getId()==R.id.check6){
                        getConfig().setDisabledDelaySSH(mCheckBox[5].isChecked());
                    }
                    else if (v.getId()==R.id.check7){
                        getConfig().setVpnUdpForward(mCheckBox[6].isChecked());
                    }
                    else if (v.getId()==R.id.check8){
                        getConfig().setVpnDnsForward(mCheckBox[7].isChecked());
                    }
                });
                mCheckBox[i2].setEnabled(!isRunning);
                mCheckBox[i2].setButtonTintList(ColorStateList.valueOf(getConfig().getColorAccent()));
            }
            TextView buff1 = findViewById(R.id.bufSend);
            TextView buff2 = findViewById(R.id.bufReceive);
            buff1.setText("Send: "+getPref().getString("buffer_send", "16384"));
            buff2.setText("Receive: "+getPref().getString("buffer_receive", "32768"));
            findViewById(R.id.mBufferSize).setOnClickListener(v -> {
                BufferSizeDialog buff = new BufferSizeDialog(mtk0001.this);
                buff.setOnBufferDialogListener(new BufferSizeDialog.OnBufferDialogListener() {
                    @Override
                    public void onSave() {
                        buff1.setText("Buffer send: "+getPref().getString("buffer_send", "16384"));
                        buff2.setText("Buffer receive: "+getPref().getString("buffer_receive", "32768"));
                    }
                    @Override
                    public void onReset() {
                        buff1.setText("Buffer send: "+getPref().getString("buffer_send", "16384"));
                        buff2.setText("Buffer receive: "+getPref().getString("buffer_receive", "32768"));
                    }
                });
                if (!isRunning)buff.show();
            });
            String[] m_dnsResolvers = getConfig().getVpnDnsResolver();
            setEditxt[0].setText(getConfig().getVpnUdpResolver());
            setEditxt[1].setText(m_dnsResolvers[0]);
            setEditxt[2].setText(m_dnsResolvers[1]);
            setEditxt[4].setText(getConfig().getPingServer());
            setEditxt[5].setText(String.valueOf(getConfig().getPingThread()));
            setEditxt[6].setText(getConfig().getProxyAddress());
            setEditxt[7].setText(getConfig().getLocalPort());
            setEditxt[8].setText(String.valueOf(getConfig().getReconnTime()));
            mCheckBox[1].setChecked(getConfig().getCompression());
            mCheckBox[2].setChecked(getConfig().getIsTetheringSubnet());
            mCheckBox[3].setChecked(getConfig().getPowerSaver());
            mCheckBox[4].setChecked(getConfig().getAutoClearLog());
            mCheckBox[5].setChecked(getConfig().getIsDisabledDelaySSH());
            mCheckBox[6].setChecked(getConfig().getVpnUdpForward());
            mCheckBox[7].setChecked(getConfig().getVpnDnsForward());
            ((TextView)findViewById(R.id.advancesettingsTextView1)).setTextColor(getConfig().getColorAccent());
        }catch (Exception e){
            util.showToast("Error!", e.getMessage());
        }
    }
        
    private boolean getChecked(boolean is){
        if(is){
            return false;
        }
        return true;
    }    
        
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(mtk0001.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        mtk0001.this.finish();
        super.onBackPressed();
    }
        
}
