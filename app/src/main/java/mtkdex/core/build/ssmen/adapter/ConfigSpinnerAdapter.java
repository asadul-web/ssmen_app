package mtkdex.core.build.ssmen.adapter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Objects;

import com.v2ray.ang.MainApplication;
import com.v2ray.ang.R;
import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.config.SettingsConstants;
import mtkdex.core.build.ssmen.utils.Model;
import mtkdex.core.build.ssmen.utils.util;
import mtkdex.core.build.ssmen.view.swipe.DragListView;
import mtkdex.core.build.ssmen.view.swipe.ItemAdapter;
import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
public class ConfigSpinnerAdapter extends AppCompatActivity implements SettingsConstants {

	private DragListView ConfigListView;
	private EditText search;
	private ItemAdapter listAdapter;
	private ImageView search_btn,clear_btn;
	private String ConfigType;
	private SharedPreferences mPref;
	private SharedPreferences.Editor mEditor;
	private ConfigUtil mConfig;
	private View randonSpaceLay;
	private final ArrayList<Model> arrayList = new ArrayList<>();
	private JSONArray cachedServers = null;
	private JSONArray cachedNetworks = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mConfig = ConfigUtil.getInstance(ConfigSpinnerAdapter.this);
		setTheme(mConfig.getAppThemeUtil()? R.style.AppThemeDialogDark : R.style.AppThemeDialogLight);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_adapters);
		mPref = MainApplication.getPrivateSharedPreferences();
		mEditor = mPref.edit();
		Bundle bundle = getIntent().getExtras();
		if (bundle == null){
			finish();
			return;
		}
		search = findViewById(R.id.config_search);
		ConfigListView = findViewById(R.id.config_listview);
		search_btn = findViewById(R.id.search_btn);
		clear_btn = findViewById(R.id.clear_btn);
		
		// Set initial visibility based on existing search text
		boolean hasText = search.getText().length() > 0;
		search_btn.setVisibility(hasText ? View.VISIBLE : View.GONE);
		clear_btn.setVisibility(hasText ? View.VISIBLE : View.GONE);

		TextView config_title = findViewById(R.id.config_title);
		View show_random_ly = findViewById(R.id.show_random_l);
		randonSpaceLay = findViewById(R.id.random_space_lay);
		
		View tabContainer = findViewById(R.id.tab_container);
		TextView tabAll = findViewById(R.id.tab_all);
		TextView tabCustom = findViewById(R.id.tab_custom);

		search.setTextSize(TypedValue.COMPLEX_UNIT_DIP,9);
		config_title.setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
		LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(ConfigSpinnerAdapter.this);
		ConfigType = bundle.getString("mConfigType");

		if (Objects.equals(ConfigType, "0")){
			config_title.setText("Choose A Server Location");
			// Only show if NOT searching
			show_random_ly.setVisibility(search.getText().length() > 0 ? View.GONE : View.VISIBLE);
			tabContainer.setVisibility(View.GONE);
		}else if (Objects.equals(ConfigType, "1")){
			config_title.setText("Choice Payload Tweaks");
			show_random_ly.setVisibility(View.GONE);
			tabContainer.setVisibility(View.VISIBLE);
			
			tabAll.setOnClickListener(v -> {
				// Instant UI highlight
				tabAll.setBackgroundResource(R.drawable.bg_tab_selected);
				tabAll.setTextColor(Color.WHITE);
				tabCustom.setBackground(null);
				tabCustom.setTextColor(mConfig.getColorAccent());
				
				// Fast filter update
				if (listAdapter != null) {
					listAdapter.filter(""); 
				}
			});

			tabCustom.setOnClickListener(v -> {
				// Instant UI highlight
				tabCustom.setBackgroundResource(R.drawable.bg_tab_selected);
				tabCustom.setTextColor(Color.WHITE);
				tabAll.setBackground(null);
				tabAll.setTextColor(mConfig.getColorAccent());
				
				// Fast filter update
				if (listAdapter != null) {
					listAdapter.filter("custom"); 
				}
			});
		}
		ConfigListView.setBackgroundColor(Color.TRANSPARENT);
		((TextView)findViewById(R.id.config_btn_closetv)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
		((TextView)findViewById(R.id.use_random1)).setTextColor(mConfig.gettextColor());
		((TextView)findViewById(R.id.use_random2)).setTextSize(TypedValue.COMPLEX_UNIT_DIP,7);
		findViewById(R.id.config_bord_title).setBackgroundColor(mConfig.getColorAccent());
		findViewById(R.id.main_adapt_bg).setBackgroundColor(mConfig.getAppThemeUtil()? getResources().getColor(R.color.black_light): Color.WHITE);
		randonSpaceLay.setVisibility(mPref.getBoolean(CONFIG_PASSCODE_KEY, false) ?View.VISIBLE:View.GONE);
		ConfigListView.getRecyclerView().setVerticalScrollBarEnabled(false);

		// Load data instantly instead of background thread to avoid "empty menu" delay
		final ArrayList<Pair<Long, JSONObject>> initialData = getConfigAdapter(ConfigType);

		ConfigListView.setLayoutManager(mLinearLayoutManager);
		listAdapter = new ItemAdapter(ConfigSpinnerAdapter.this, initialData, R.layout.list_item, R.id.dragHandle, Integer.parseInt(ConfigType));
		ConfigListView.setAdapter(listAdapter, true);
		ConfigListView.setCanDragHorizontally(false);
		ConfigListView.setCanDragVertically(true);

		ConfigListView.setDragListListener(new DragListView.DragListListenerAdapter() {
			@Override
			public void onItemDragEnded(int fromPosition, int toPosition) {
				loadNewJS(listAdapter.getNewJS(), toPosition);
			}
		});

		listAdapter.setOnSelectedSerListener(new ItemAdapter.OnSelectedSerListener() {
			@Override
			public void onSelectSer(String positionStr) {
				int p = Integer.parseInt(positionStr);
				if (Objects.equals(ConfigType, "0")) {
					mEditor.putInt(SERVER_POSITION, p);
					mEditor.putBoolean("isRandom", false);
				} else if (Objects.equals(ConfigType, "1")) {
					mEditor.putInt(NETWORK_POSITION, p);
					try {
						JSONArray jar = getNetworkArrayDragaPosition();
						if (jar != null && p < jar.length()) {
							JSONObject js = jar.getJSONObject(p);
							if (js.has("proto_spin")) {
								int proto = js.getInt("proto_spin");
								int currentCat = mPref.getInt(manual_tunnel_radio_key, 0);

								if (proto == 1) { // Hysteria
									if (currentCat != 1) {
										mEditor.putInt(manual_tunnel_radio_key, 1);
										mEditor.putInt(SERVER_POSITION, 0);
									}
								} else if (proto == 2) { // SLOWDNS
									if (currentCat != 3) {
										mEditor.putInt(manual_tunnel_radio_key, 3);
										mEditor.putInt(SERVER_POSITION, 0);
									}
								} else if (proto == 6) { // V2ray
									if (currentCat < 4 || currentCat > 6) {
										mEditor.putInt(manual_tunnel_radio_key, 4);
										mEditor.putInt(SERVER_POSITION, 0);
									}
								} else {
									// Generic payloads (HTTP, SSL, etc.)
									// If we are in a specialized category, default back to OVPN (0)
									if (currentCat == 1 || currentCat == 3 || (currentCat >= 4 && currentCat <= 6)) {
										mEditor.putInt(manual_tunnel_radio_key, 0);
										mEditor.putInt(SERVER_POSITION, 0);
									}
								}
							}
						}
					} catch (Exception e) {
						android.util.Log.e("ConfigSpinner", "Error updating tunnel category", e);
					}
				}
				mEditor.commit();
				finish();
			}

			@Override
			public void onReloadConfig(int position) {
				setupListRecyclerView(ConfigListView, listAdapter);
			}
		});

		// Apply initial filter if exists
		String savedSearch = mPref.getString("my_config_research_" + ConfigType, "");
		if (!savedSearch.isEmpty()) {
			search.setText(savedSearch);
			listAdapter.filter(savedSearch);
			if (Objects.equals(ConfigType, "0")) {
				show_random_ly.setVisibility(listAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
			}
		}

		clear_btn = findViewById(R.id.clear_btn);
		search_btn = findViewById(R.id.search_btn);
		RelativeLayout cancel = findViewById(R.id.config_btn_close);
		search.setText(mPref.getString("my_config_research_"+ConfigType,""));

		boolean isRandomSelected = mPref.getBoolean("isRandom", false);
		show_random_ly.setBackgroundResource(isRandomSelected ? R.drawable.border_item_selected : R.drawable.border_item_normal);

		show_random_ly.setOnClickListener(p1 -> {
			mEditor.putBoolean("isRandom", true).commit();
			finish();
		});

		cancel.setBackgroundTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
		cancel.setOnClickListener(p1 -> {
			finish();
		});

		search.setOnEditorActionListener((v, actionId, event) -> {
			String newText = search.getText().toString().trim();
			mEditor.putString("my_config_research_"+ConfigType,newText).apply();
			if (listAdapter != null) {
				if (TextUtils.isEmpty(newText)){
					listAdapter.filter("");
				}else {
					listAdapter.filter(newText);
				}
				setupListRecyclerView(ConfigListView,listAdapter);
				if (Objects.equals(ConfigType, "0")) {
					show_random_ly.setVisibility(listAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
				}
			}
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
			return false;
		});

		search.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String newText = search.getText().toString().trim();
				mEditor.putString("my_config_research_"+ConfigType,newText).apply();
				boolean hasText = search.length() > 0;
				clear_btn.setVisibility(hasText ? View.VISIBLE : View.GONE);
				search_btn.setVisibility(hasText ? View.VISIBLE : View.GONE);
				listAdapter.filter(newText);
				if (Objects.equals(ConfigType, "0")) {
					show_random_ly.setVisibility(listAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
				}
			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		clear_btn.setOnClickListener(v -> {
			search.getText().clear();
			mEditor.putString("my_config_research_"+ConfigType,"").apply();
			if (reloadAdapterView()) {
				mLinearLayoutManager.scrollToPosition(Objects.equals(ConfigType, "0") ? mPref.getInt(SERVER_POSITION, 0) : mPref.getInt(NETWORK_POSITION, 0));
			}
		});
		search_btn.setOnClickListener(v -> {
			String newText = search.getText().toString().trim();
			mEditor.putString("my_config_research_"+ConfigType,newText).apply();
			if (listAdapter != null) {
				if (TextUtils.isEmpty(newText)){
					listAdapter.filter("");
				}else {
					listAdapter.filter(newText);
				}
				setupListRecyclerView(ConfigListView,listAdapter);
				if (Objects.equals(ConfigType, "0")) {
					show_random_ly.setVisibility(listAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
				}
			}
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		});

		findViewById(R.id.config_btn_close_add).setOnClickListener(v -> {
			final boolean isEditMode = mPref.getBoolean(CONFIG_PASSCODE_KEY, false);
			PopupMenu popup = new PopupMenu(ConfigSpinnerAdapter.this, v);
			
			// 1. Show/Close Edit Menu (Order 0)
			popup.getMenu().add(0, 0, 0, isEditMode ? "Close Edit Menu" : "Show Edit Menu");

			// 2. Add Options (Order 1 and 2)
			if ("0".equals(ConfigType)) {
				popup.getMenu().add(0, 1, 1, "Add Server");
			} else {
				String serverType = mConfig.getServerType();
				if (serverType.contains("OVPN") || serverType.contains("SSH")) {
					popup.getMenu().add(0, 1, 1, "Add HTTP Payload");
					popup.getMenu().add(0, 3, 2, "Add SSL Payload");
				} else if (serverType.contains("V2RAY")) {
					popup.getMenu().add(0, 1, 1, "Add V2Ray Payload");
				} else if (serverType.contains("UDP") || serverType.contains("Hysteria")) {
					popup.getMenu().add(0, 1, 1, "Add UDP Tweak");
				} else if (serverType.contains("DNS")) {
					popup.getMenu().add(0, 1, 1, "Add DNS Payload");
				} else {
					popup.getMenu().add(0, 1, 1, "Add Payload");
				}
			}

			// 3. Paste Config (Order 99 to be last)
			popup.getMenu().add(0, 2, 99, "0".equals(ConfigType) ? "Past Config" : "Paste Config");

			popup.setOnMenuItemClickListener(item -> {
				int id = item.getItemId();
				if (id == 0) {
					// Toggle Edit Mode
					if (isEditMode) {
						mEditor.putBoolean(CONFIG_PASSCODE_KEY, false).apply();
						if (listAdapter != null) {
							listAdapter.setShowEditMenu(false);
						}
						setupListRecyclerView(ConfigListView, listAdapter);
					} else {
						show_input_code();
					}
				} else if (id == 1) {
					// Add Action
					if (listAdapter != null) {
						if ("0".equals(ConfigType)) {
							listAdapter.addServer();
						} else {
							String serverType = mConfig.getServerType();
							if (serverType.contains("V2RAY")) {
								listAdapter.addPayload(3); // 3 is V2Ray type in PayloadDialog
							} else if (serverType.contains("UDP") || serverType.contains("Hysteria")) {
								listAdapter.addPayload(1); // 1 is Config.json/UDP type
							} else if (serverType.contains("DNS")) {
								listAdapter.addPayload(2); // 2 is DNS type
							} else {
								listAdapter.addPayload(0); // 0 is HTTP/Payload type
							}
						}
					}
				} else if (id == 3) {
					// Add SSL
					if (listAdapter != null) {
						listAdapter.addSSL();
					}
				} else if (id == 4) {
					// Add V2Ray - In this project, V2Ray is usually handled in addPayload or a specific dialog
					// If there's no specific addV2Ray(), we'll use addPayload() as it likely contains V2Ray options
					if (listAdapter != null) {
						listAdapter.addPayload();
					}
				} else if (id == 2) {
					// Paste Config Action
					String clipData = mtkdex.core.build.ssmen.utils.c_01.getClipboard(ConfigSpinnerAdapter.this);
					if (TextUtils.isEmpty(clipData)) {
						util.showToast("Error!", "Config Clipboard is empty!");
					} else {
						try {
							JSONObject jo = new JSONObject(clipData);
							if (listAdapter != null) {
								if (jo.has("serverType") || jo.has("ServerIP")) {
									// It's a server config
									listAdapter.updateAllData(jo);
									util.showToast("Success", "Server config imported!");
								} else if (jo.has("NetworkPayload") || jo.has("proto_spin")) {
									// It's a payload/SSL config
									listAdapter.updateAllData(jo);
									util.showToast("Success", "Payload config imported!");
								} else {
									util.showToast("Error", "Invalid config format!");
								}
								loadNewJS(listAdapter.getNewJS(), listAdapter.getItemCount() - 1);
								setupListRecyclerView(ConfigListView, listAdapter);
							}
						} catch (JSONException e) {
							util.showToast("Error", "Invalid JSON config!");
						}
					}
				}
				return true;
			});
			popup.show();
		});

		findViewById(R.id.paygen_menu).setOnClickListener(v -> {
			final boolean show_pass = mPref.getBoolean(CONFIG_PASSCODE_KEY, false);
			PopupMenu popup = new PopupMenu(ConfigSpinnerAdapter.this, v);
			popup.getMenu().add(0, 0, 0, show_pass?"Close Edit menu":"Show Edit menu");
			if (mConfig.getServerType().equals(SERVER_TYPE_V2RAY)) {
				popup.getMenu().add(1, 1, 1, "Add server");
			} else {
				if (mConfig.getServerType().equals(SERVER_TYPE_OVPN) || mConfig.getServerType().equals(SERVER_TYPE_SSH)){
					popup.getMenu().add(1, 1, 1, Objects.equals(ConfigType, "0")?"Add server":"Add HTTP");
					if (Objects.equals(ConfigType, "1"))popup.getMenu().add(2, 2, 2, "Add SSL");
				}else {
					popup.getMenu().add(1, 1, 1, Objects.equals(ConfigType, "0")?"Add server":"Add tweak");
				}
			}
			popup.setOnMenuItemClickListener(item -> {
				if (item.getItemId() == 0) {
					if(show_pass){
						mEditor.putBoolean(CONFIG_PASSCODE_KEY,false).apply();
						setupListRecyclerView(ConfigListView,listAdapter);
					}else{
						show_input_code();
					}
				} else if (item.getItemId() == 1){
					if (listAdapter!=null){
						if (listAdapter.getServerDialog()){
							listAdapter.addServer();
						}else{
							listAdapter.addPayload();
						}
					}
				} else if (item.getItemId() == 2){
					if (listAdapter!=null){
						listAdapter.addSSL();
					}
				}
				popup.dismiss();
				return false;
			});
			popup.show();
		});
		if (reloadAdapterView())mLinearLayoutManager.scrollToPosition(Objects.equals(ConfigType, "0")?mPref.getInt(SERVER_POSITION,0):mPref.getInt(NETWORK_POSITION,0));
		if (mPref.getBoolean("show_random_layout",false)&&Objects.equals(ConfigType, "0")){
			show_random_ly.setVisibility(View.VISIBLE);
		}
	}


	protected JSONArray networkArrayDragaPosition() {
		if (cachedNetworks != null) return cachedNetworks;
		try {
			JSONArray jarr = new JSONArray(mPref.getString(SettingsConstants.LOAD_ALL_TWEAKS_KEY, "[]"));
			cachedNetworks = jarr;
			return jarr;
		} catch (JSONException e) {
			util.showToast("Error-13!", e.getMessage());
		}
        return new JSONArray();
    }

	private boolean reloadAdapterView(){
		String research = search.getText().toString().trim();
		clear_btn.setVisibility(search.length()>0? View.VISIBLE:View.GONE);
		search_btn.setVisibility(search.length()>0? View.VISIBLE:View.GONE);
		if (TextUtils.isEmpty(research)){
			return true;
		}else {
			listAdapter.filter(research);
			setupListRecyclerView(ConfigListView,listAdapter);
			return false;
		}
	}


	private void loadNewJS(String data,int position){
		try {
			if (Objects.equals(ConfigType, "0")){
				String a = SERVER_TYPE_OVPN;
				mEditor.putInt(SERVER_POSITION,position).apply();
				if (mPref.getInt(manual_tunnel_radio_key, 0)==0){
					a = SERVER_TYPE_OVPN;
				}else if (mPref.getInt(manual_tunnel_radio_key, 0)==1){
					a = SERVER_TYPE_UDP_HYSTERIA_V1;
				}else if (mPref.getInt(manual_tunnel_radio_key, 0)==2){
					a = SERVER_TYPE_SSH;
				}else if (mPref.getInt(manual_tunnel_radio_key, 0)==3){
					a = SERVER_TYPE_DNS;
				}else if (mPref.getInt(manual_tunnel_radio_key, 0)==4){
					a = SERVER_TYPE_V2RAY;
				}else if (mPref.getInt(manual_tunnel_radio_key, 0)==5){
					a = SERVER_TYPE_CDN_V2RAY;
				}else if (mPref.getInt(manual_tunnel_radio_key, 0)==6){
					a = SERVER_TYPE_SSL_V2RAY;
				}

				mEditor.putString(a, data).apply();

			}else if (Objects.equals(ConfigType, "1")){
				String b = SERVER_TYPE_OVPN;
				mEditor.putInt(NETWORK_POSITION,position).apply();
				if (mPref.getInt(manual_tunnel_radio_key, 0)==0 || mPref.getInt(manual_tunnel_radio_key, 0)==2){
					b = LOAD_ALL_TWEAKS_KEY;
				}else if (mPref.getInt(manual_tunnel_radio_key, 0)==1){
					b = LOAD_ALL_TWEAKS_KEY;
				}else if (mPref.getInt(manual_tunnel_radio_key, 0)==3){
					b = LOAD_ALL_TWEAKS_KEY;
				}else if (mPref.getInt(manual_tunnel_radio_key, 0)==4){
					b = LOAD_ALL_TWEAKS_KEY;
				}else if (mPref.getInt(manual_tunnel_radio_key, 0)==5){
					b = LOAD_ALL_TWEAKS_KEY;
				}else if (mPref.getInt(manual_tunnel_radio_key, 0)==6){
					b = LOAD_ALL_TWEAKS_KEY;
				}
				mEditor.putString(b, data).apply();
			}
		} catch (Exception ignored) {
		}
	}

	private void setupListRecyclerView(DragListView mDragListView,ItemAdapter listAdapter) {
		LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(ConfigSpinnerAdapter.this);
		randonSpaceLay.setVisibility(mPref.getBoolean(CONFIG_PASSCODE_KEY, false) ?View.VISIBLE:View.GONE);
		mDragListView.setLayoutManager(mLinearLayoutManager);
		mDragListView.setAdapter(listAdapter, true);
		mDragListView.setCanDragHorizontally(false);
		mDragListView.setCanDragVertically(true);
		mLinearLayoutManager.scrollToPosition(Objects.equals(ConfigType, "0")?mPref.getInt(SERVER_POSITION,0):mPref.getInt(NETWORK_POSITION,0));
	}

	private ArrayList<Pair<Long, JSONObject>> getConfigAdapter(String t) {
		ArrayList<Pair<Long, JSONObject>> mItemArray = new ArrayList<>();
		arrayList.clear();
		JSONArray jar = null;
		try {
			if (Objects.equals(t, "0")){
				jar = getServerArrayDragaPosition();
			}else if (Objects.equals(t, "1")){
				jar = getNetworkArrayDragaPosition();
			}
			if (jar == null) return mItemArray;
			for (int i=0;i < jar.length();i++) {
				JSONObject jo = jar.getJSONObject(i);
				String name = jo.getString("Name");
				Model model = new Model(name, i);
				arrayList.add(model);
				mItemArray.add(new Pair<>((long) i, jo));
			}
			return mItemArray;
		} catch (Exception e) {
			return mItemArray;
		}
	}

	public JSONArray getServerArrayDragaPosition(){
		try {
			JSONArray jar = new JSONArray();
			if (mPref.getInt(manual_tunnel_radio_key, 0)==0){
				JSONArray jarr1 = new JSONArray(mPref.getString(SERVER_TYPE_OVPN,"[]"));
				for (int i=0;i < jarr1.length();i++){
					if (jarr1.getJSONObject(i).getInt("serverType")==0){
						jar.put(jarr1.getJSONObject(i));
					}
				}
			}else if (mPref.getInt(manual_tunnel_radio_key, 0)==2){
				JSONArray jarr2 = new JSONArray(mPref.getString(SERVER_TYPE_SSH,"[]"));
				for (int i=0;i < jarr2.length();i++){
					if (jarr2.getJSONObject(i).getInt("serverType")==1){
						jar.put(jarr2.getJSONObject(i));
					}
				}
			}else if (mPref.getInt(manual_tunnel_radio_key, 0)==3){
				JSONArray jarr3 = new JSONArray(mPref.getString(SERVER_TYPE_DNS,"[]"));
				for (int i=0;i < jarr3.length();i++){
					if (jarr3.getJSONObject(i).getInt("serverType")==2){
						jar.put(jarr3.getJSONObject(i));
					}
				}
			}else if (mPref.getInt(manual_tunnel_radio_key, 0)==1){
				JSONArray jarr4 = new JSONArray(mPref.getString(SERVER_TYPE_UDP_HYSTERIA_V1,"[]"));
				for (int i=0;i < jarr4.length();i++){
					if (jarr4.getJSONObject(i).getInt("serverType")==4){
						jar.put(jarr4.getJSONObject(i));
					}
				}
			}else if (mPref.getInt(manual_tunnel_radio_key, 0)==4){
				JSONArray jarr5 = new JSONArray(mPref.getString(SERVER_TYPE_V2RAY,"[]"));
				for (int i=0;i < jarr5.length();i++){
					if (jarr5.getJSONObject(i).getInt("serverType")==3){
						if (jarr5.getJSONObject(i).getInt("V2rayType") == 0) {
							jar.put(jarr5.getJSONObject(i));
						}
					}
				}
			}else if (mPref.getInt(manual_tunnel_radio_key, 0)==5){
				JSONArray jarr6 = new JSONArray(mPref.getString(SERVER_TYPE_CDN_V2RAY,"[]"));
				for (int i=0;i < jarr6.length();i++){
					if (jarr6.getJSONObject(i).getInt("serverType")==3){
						if (jarr6.getJSONObject(i).getInt("V2rayType") == 1) {
							jar.put(jarr6.getJSONObject(i));
						}
					}
				}
			}else if (mPref.getInt(manual_tunnel_radio_key, 0)==6){
				JSONArray jarr7 = new JSONArray(mPref.getString(SERVER_TYPE_SSL_V2RAY,"[]"));
				for (int i=0;i < jarr7.length();i++){
					if (jarr7.getJSONObject(i).getInt("serverType")==3){
						if (jarr7.getJSONObject(i).getInt("V2rayType") == 2) {
							jar.put(jarr7.getJSONObject(i));
						}
					}
				}
			}
			if (jar.length()>=2){
				mEditor.putBoolean("show_random_layout",true).apply();
			}else{
				mEditor.putBoolean("show_random_layout",false).apply();
			}
			return jar;
		} catch (JSONException e) {
			mEditor.putBoolean("isRandom", false).apply();
			mEditor.putBoolean("show_random_layout",false).apply();
			util.showToast("Error-8!", e.getMessage());
		}
		return null;
	}
	public JSONArray getNetworkArrayDragaPosition() {
		try {
			JSONArray jar = new JSONArray();
			JSONArray jarr = new JSONArray(mPref.getString(LOAD_ALL_TWEAKS_KEY, "[]"));
			for (int i = 0; i < jarr.length(); i++) {
				jar.put(jarr.getJSONObject(i));
			}
			// jar = JsonUtils.sort(jar, JsonUtils.getComparator(this, "FLAG", 1));
			return jar;
		} catch (JSONException e) {
			util.showToast("Error-2!", e.getMessage());
		}
		return null;
	}

	private void show_input_code() {
		final String b1 = mPref.getString(CONFIG_EDITOR_CODE,"");
		if (b1.isEmpty()) {
			// If no passcode is set, bypass the check and show the edit menu directly
			mEditor.putBoolean(CONFIG_PASSCODE_KEY, true).apply();
			if (listAdapter != null) {
				listAdapter.setShowEditMenu(true);
			}
			setupListRecyclerView(ConfigListView, listAdapter);
			return;
		}
		randonSpaceLay.setVisibility(View.GONE);
		View inflate = LayoutInflater.from(ConfigSpinnerAdapter.this).inflate(R.layout.add_pass_config, null);
		final AlertDialog cBuiler = new AlertDialog.Builder(ConfigSpinnerAdapter.this,mConfig.getAlertDialog()).create();
		final EditText ed1 = inflate.findViewById(R.id.editText1);
		final EditText ed2 = inflate.findViewById(R.id.editText2);
		final EditText ed3 = inflate.findViewById(R.id.editText3);
		final EditText ed4 = inflate.findViewById(R.id.editText4);
		final TextView title = inflate.findViewById(R.id.codeTv);
		inflate.findViewById(R.id.color_bg).setBackgroundColor(mConfig.getColorAccent());
		((TextView)inflate.findViewById(R.id.appButton1)).setTextColor(mConfig.getColorAccent());
		((TextView)inflate.findViewById(R.id.confirm_btn)).setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
		title.setTextColor(mConfig.getAppThemeUtil()? Color.BLACK:Color.WHITE);
		inflate.findViewById(R.id.appButton2).setBackgroundTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
		ed1.setText(mPref.getString("_p01",""));
		ed2.setText(mPref.getString("_p02",""));
		ed3.setText(mPref.getString("_p03",""));
		ed4.setText(mPref.getString("_p04",""));
		title.setText("Admin PassCode");
		inflate.findViewById(R.id.appButton2).setOnClickListener(view -> {
			mEditor.putBoolean(CONFIG_PASSCODE_KEY, false).apply();
			Animation animation = AnimationUtils.loadAnimation(ConfigSpinnerAdapter.this, R.anim.shake);
			String p01 = ed1.getText().toString().trim();
			String p02 = ed2.getText().toString().trim();
			String p03 = ed3.getText().toString().trim();
			String p04 = ed4.getText().toString().trim();
			String b2 = p01+":"+p02+"-"+p03+":"+p04;
			if (p01.isEmpty()) {
				title.setText("Invalid key code");
				title.startAnimation(animation);
				util.showToast("Error!", "Please enter the code");
				return;
			}
			if (p02.isEmpty()) {
				title.setText("Invalid key code");
				title.startAnimation(animation);
				util.showToast("Error!", "Please enter the code");
				return;
			}
			if (p03.isEmpty()) {
				title.setText("Invalid key code");
				title.startAnimation(animation);
				util.showToast("Error!", "Please enter the code");
				return;
			}
			if (p04.isEmpty()) {
				title.setText("Invalid key code");
				title.startAnimation(animation);
				util.showToast("Error!", "Please enter the code");
				return;
			}
			if(!b2.equals(b1)){
				if (listAdapter != null) {
					listAdapter.setShowEditMenu(false);
				}
				setupListRecyclerView(ConfigListView,listAdapter);
				title.setText("Invalid key code");
				title.startAnimation(animation);
				util.showToast("Oppss...!", "Invalid key code");
				return;
			}
			mEditor.putBoolean(CONFIG_PASSCODE_KEY, true).apply();
			if (listAdapter != null) {
				listAdapter.setShowEditMenu(true);
			}
			title.clearAnimation();
			animation.cancel();
			setupListRecyclerView(ConfigListView,listAdapter);
			cBuiler.dismiss();
		});
		inflate.findViewById(R.id.cancel_btn).setOnClickListener(p1 -> {
			setupListRecyclerView(ConfigListView,listAdapter);
			cBuiler.dismiss();
		});
		ed1.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}
			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				mEditor.putString("_p01",ed1.getText().toString().trim()).apply();
				if (ed1.getText().toString().length() == 1) {
					ed2.requestFocus();
				}
			}
			@Override
			public void afterTextChanged(Editable editable) {
			}
		});
		ed2.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}
			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				mEditor.putString("_p02",ed2.getText().toString().trim()).apply();
				if (ed2.getText().toString().length() == 1) {
					ed3.requestFocus();
				}
			}
			@Override
			public void afterTextChanged(Editable editable) {
			}
		});
		ed3.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}
			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				mEditor.putString("_p03",ed3.getText().toString().trim()).apply();
				if (ed3.getText().toString().length() == 1) {
					ed4.requestFocus();
				}
			}
			@Override
			public void afterTextChanged(Editable editable) {
			}
		});
		ed4.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}
			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				mEditor.putString("_p04",ed4.getText().toString().trim()).apply();
			}
			@Override
			public void afterTextChanged(Editable editable) {
			}
		});
		cBuiler.setView(inflate);
		cBuiler.setCancelable(false);
		cBuiler.getWindow().getAttributes().windowAnimations = R.style.alertDialog;
		cBuiler.show();
	}

	private void reloadOvpnProfile(boolean isOvpnServer,String profile_content) {
		/*if (isOvpnServer && mConfig.getServerType().equals(SERVER_TYPE_OVPN)) {
			String prefix = dex003.INTENT_PREFIX;
			startService(new Intent(ConfigSpinnerAdapter.this, dex003.class).setAction(dex003.ACTION_REFRESH_PROFILE).putExtra(prefix + ".CONTENT", profile_content));
		}*/
	}

}
