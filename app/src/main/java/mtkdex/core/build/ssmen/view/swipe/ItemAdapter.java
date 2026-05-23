package mtkdex.core.build.ssmen.view.swipe;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.util.Pair;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.v2ray.ang.MainApplication;
import com.v2ray.ang.R;
import mtkdex.core.build.ssmen.config.ConfigUtil;
import mtkdex.core.build.ssmen.config.SettingsConstants;
import mtkdex.core.build.ssmen.utils.util;
import mtkdex.core.build.ssmen.view.PayloadDialog;
import mtkdex.core.build.ssmen.view.SSLDialog;
import mtkdex.core.build.ssmen.view.ServerDialog;

public class ItemAdapter extends DragItemAdapter<Pair<Long, JSONObject>, ItemAdapter.ViewHolder> implements SettingsConstants {

    private final int mLayoutId;
    private final int ConfigType;
    private final int mGrabHandleId;
    private final boolean mDragOnLongPress;
    private boolean isConfigSearch = false;
    private final Context mContext;
    private final ArrayList<Pair<Long, JSONObject>> originalList;
    private final ConfigUtil mConfig;
    private final SharedPreferences mPref;
    private final SharedPreferences.Editor mEditor;
    private OnSelectedSerListener mListener;
    private static final Map<String, Drawable> iconCache = new HashMap<>();
    
    // Cached values to avoid repeated SharedPreferences reads
    private final int accentColor;
    private final boolean isDarkMode;
    private final String currentServerType;
    private boolean showEditMenu;
    private final int selectedPosition;

    public interface OnSelectedSerListener {
        void onSelectSer(String name);
        void onReloadConfig(int position);
    }

    public void setOnSelectedSerListener(OnSelectedSerListener listener) {
        this.mListener = listener;
    }

    public ItemAdapter(Context mContext, ArrayList<Pair<Long, JSONObject>> list, int layoutId, int grabHandleId, int ConfigType) {
        this.mContext = mContext;
        this.mPref = MainApplication.getPrivateSharedPreferences();
        this.mEditor = mPref.edit();
        this.mConfig = ConfigUtil.getInstance(mContext);
        this.ConfigType = ConfigType;
        this.mLayoutId = layoutId;
        this.mGrabHandleId = grabHandleId;
        this.mDragOnLongPress = false;
        
        // Cache SharedPreferences values once
        this.accentColor = mConfig.getColorAccent();
        this.isDarkMode = mConfig.getAppThemeUtil();
        this.currentServerType = mConfig.getServerType();
        this.showEditMenu = mPref.getBoolean(CONFIG_PASSCODE_KEY, false);
        this.selectedPosition = (ConfigType == 0) ? mPref.getInt(SERVER_POSITION, 0) : mPref.getInt(NETWORK_POSITION, 0);
        
        this.originalList = new ArrayList<>(list);
        setItemList(new ArrayList<>(list));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        try {
            // Get the JSONObject directly from the current item list
            JSONObject js = mItemList.get(position).second;
            
            holder.mName1.setText(js.optString("Name", "Unnamed"));
            holder.itemView.setTag(mItemList.get(position));
            
            String flag = js.optString("FLAG", "unknown");
            String iconKey = (ConfigType == 0 ? "flags/flag_" : "networks/icon_") + flag + (ConfigType == 0 ? ".webp" : ".png");
            
            if (ConfigType == 0) {
                holder.mName3_ly.setVisibility(View.VISIBLE);
                holder.mName3.setVisibility(View.VISIBLE);
                holder.mName3_ly.setBackgroundTintList(ColorStateList.valueOf(accentColor));
                holder.mName3.setText(getServerType(js));
                
                holder.mName2.setText("Premium Speed ⚡");
                holder.mName2.setTextColor(isDarkMode ? Color.parseColor("#C6C6C6") : Color.parseColor("#666666"));
                holder.mName2.setVisibility(View.VISIBLE);
                
                String serverMsg = js.optString("Server_msg", "");
                if (!serverMsg.isEmpty()) {
                    holder.mName2.setText(serverMsg);
                }
                
                if (js.has("Server_exp_box")) {
                    String svldt = sValidity(js);
                    if (!svldt.isEmpty()) {
                        if (isConfigXpired(js)) {
                            holder.mName2.setTextColor(Color.RED);
                            holder.mName3.setText("Expired");
                            holder.mName2.setText("This server Expired!");
                            holder.mName3_ly.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#68B86B")));
                        } else {
                            holder.mName2.setText(svldt);
                        }
                    }
                }
            } else {
                holder.mName3.setText(getNetworkType(js));
                String info = js.optString("Info", "");
                if (!info.isEmpty()) {
                    holder.mName2.setText(info);
                    holder.mName2.setVisibility(View.VISIBLE);
                } else {
                    holder.mName2.setText("");
                    holder.mName2.setVisibility(View.GONE);
                }
                
                if (currentServerType.equals(SERVER_TYPE_UDP_HYSTERIA_V1) || currentServerType.equals(SERVER_TYPE_DNS)) {
                    holder.mName3_ly.setVisibility(View.GONE);
                    holder.mName3.setVisibility(View.GONE);
                } else {
                    holder.mName3_ly.setVisibility(View.VISIBLE);
                    holder.mName3.setVisibility(View.VISIBLE);
                    holder.mName3_ly.setBackgroundTintList(ColorStateList.valueOf(accentColor));
                }
            }
            
            // Optimized icon loading with cache
            Drawable icon = iconCache.get(iconKey);
            if (icon == null) {
                try (InputStream open = mContext.getAssets().open(iconKey)) {
                    icon = Drawable.createFromStream(open, null);
                    if (icon != null) iconCache.put(iconKey, icon);
                } catch (Exception ignored) {}
            }
            holder.mIcon.setImageDrawable(icon);

            holder.mDragBtn.setVisibility(isConfigSearch ? View.GONE : View.VISIBLE);
            
            // Highlight selected item - using cached selectedPosition
            long currentId = mItemList.get(position).first;
            if (currentId == (long)selectedPosition) {
                 holder.item_layout.setBackgroundResource(R.drawable.border_item_selected);
            } else {
                 holder.item_layout.setBackgroundResource(R.drawable.border_item_normal);
            }
            
        } catch (Exception e) {
            holder.mName1.setText("Error loading item");
        }
    }

    public void setShowEditMenu(boolean show) {
        this.showEditMenu = show;
        notifyDataSetChanged();
    }

    private String sValidity(JSONObject json) throws Exception {
        if (json.has("Server_exp_box")) {
            String expData = json.optString("Server_exp", "");
            if (expData.contains("-spliter-")) {
                long mValidade = Long.parseLong(expData.split("-spliter-")[0]);
                String getTime = expData.split("-spliter-")[1];
                if (mValidade != 0) {
                    Calendar cal = Calendar.getInstance();
                    long time_hoje = cal.getTimeInMillis();
                    DateFormat df = DateFormat.getDateInstance();
                    long dias = ((mValidade - time_hoje) / 1000 / 60 / 60 / 24);
                    return String.format("%s %s Until (%s) %s", dias, (dias == 1 ? "Day left" : "Days left"), df.format(mValidade), getTime);
                }
            }
        }
        return "";
    }

    private boolean isConfigXpired(JSONObject js) {
        try {
            if (js.has("Server_exp_box")) {
                String expData = js.optString("Server_exp", "");
                if (expData.contains("-spliter-")) {
                    long mValidade = Long.parseLong(expData.split("-spliter-")[0]);
                    return mValidade > 0 && Calendar.getInstance().getTime().getTime() >= mValidade;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public String getJS(int position) {
        try {
            JSONObject js = mItemList.get(position).second;
            return js.getString("Name");
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addServer() {
        ServerDialog a = new ServerDialog(mContext);
        a.add();
        a.onServerAdd(json -> {
            try {
                updateAllData(json);
                mListener.onReloadConfig(mItemList.size() - 1);
            } catch (Exception e) {
                util.showToast("Error adding server", e.getMessage());
            }
        });
        a.init();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addPayload() {
        addPayload(-1);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addPayload(int type) {
        PayloadDialog a = new PayloadDialog(mContext);
        if (type != -1) {
            a.add(type);
        } else {
            a.add();
        }
        a.onPayloadAdd(json -> {
            try {
                updateAllData(json);
                mListener.onReloadConfig(mItemList.size() - 1);
            } catch (Exception e) {
                util.showToast("Error adding payload", e.getMessage());
            }
        });
        a.init();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addSSL() {
        SSLDialog a = new SSLDialog(mContext);
        a.add();
        a.onPayloadAdd(json -> {
            try {
                updateAllData(json);
                mListener.onReloadConfig(mItemList.size() - 1);
            } catch (Exception e) {
                util.showToast("Error adding SSL", e.getMessage());
            }
        });
        a.init();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateAllData(JSONObject json) {
        long newId = originalList.size();
        Pair<Long, JSONObject> newPair = new Pair<>(newId, json);
        originalList.add(newPair);
        mItemList.add(newPair);
        notifyDataSetChanged();
    }

    public boolean getServerDialog() {
        if (ConfigType == 0) {
            int mode = mPref.getInt(manual_tunnel_radio_key, 0);
            int selection = switch (mode) {
                case 0 -> 0;
                case 2 -> 1;
                case 3 -> 2;
                case 4 -> 3;
                case 1 -> 4;
                default -> 0;
            };
            mEditor.putInt(server_spin_mSelection_key, selection).apply();
            return true;
        } else {
            int mode = mPref.getInt(manual_tunnel_radio_key, 0);
            int selection = (mode == 0 || mode == 2) ? 0 : (mode == 3 ? 2 : (mode == 1 ? 1 : 0));
            mEditor.putInt(network_spin_mSelection_key, selection).apply();
            return false;
        }
    }

    @Override
    public long getUniqueItemId(int position) {
        return mItemList.get(position).first;
    }

    public class ViewHolder extends DragItemAdapter.ViewHolder {
        TextView mName1, mName3, mName2;
        ImageView mIcon, mImageMenu;
        View mDragBtn; // Changed from RelativeLayout to View
        View mName3_ly, item_layout;

        ViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, mDragOnLongPress);
            item_layout = itemView.findViewById(R.id.item_layout);
            mName3_ly = itemView.findViewById(R.id.name3_ly);
            mName1 = itemView.findViewById(R.id.name1);
            mName2 = itemView.findViewById(R.id.name2);
            mName3 = itemView.findViewById(R.id.name3);
            mIcon = itemView.findViewById(R.id.icon);
            mDragBtn = itemView.findViewById(R.id.dragHandle);
            mImageMenu = itemView.findViewById(R.id.image_menu);

            mImageMenu.setOnClickListener(view -> showPopupMenu(view));
        }

        private void showPopupMenu(View view) {
            PopupMenu popup = new PopupMenu(mContext, view);
            popup.getMenu().add(0, 0, 0, "Edit");
            popup.getMenu().add(1, 1, 1, "Delete");
            popup.setOnMenuItemClickListener(item -> {
                int position = getAdapterPosition();
                if (item.getItemId() == 0) {
                    handleEdit(position);
                } else if (item.getItemId() == 1) {
                    handleDelete(position);
                }
                return true;
            });
            popup.show();
        }

        private void handleEdit(int position) {
            try {
                JSONObject js = mItemList.get(position).second;
                if (ConfigType == 0) {
                    mEditServer(position);
                } else {
                    if (js.optInt("proto_spin") >= 3 && js.optInt("proto_spin") <= 5) {
                        mEditSSL(position);
                    } else {
                        mEditHTTP(position);
                    }
                }
            } catch (Exception ignored) {}
        }

        private void handleDelete(int position) {
            try {
                Pair<Long, JSONObject> itemToRemove = mItemList.get(position);
                originalList.remove(itemToRemove);
                mItemList.remove(position);
                notifyItemRemoved(position);
                mListener.onReloadConfig(0);
            } catch (Exception ignored) {}
        }

        @Override
        public void onItemClicked(View view) {
            try {
                mListener.onSelectSer(String.valueOf(mItemList.get(getAdapterPosition()).first));
            } catch (Exception e) {
                mListener.onSelectSer("0");
            }
        }

        @Override
        public boolean onItemLongClicked(View view) {
            return true;
        }

        // --- Edit Dialog Helpers ---
        private void mEditServer(int position) {
            ServerDialog a = new ServerDialog(mContext);
            try {
                a.edit(mItemList.get(position).second);
                a.onServerAdd(json -> updateItem(position, json));
                a.init();
            } catch (Exception ignored) {}
        }

        private void mEditHTTP(int position) {
            PayloadDialog a = new PayloadDialog(mContext);
            try {
                a.edit(mItemList.get(position).second);
                a.onPayloadAdd(json -> updateItem(position, json));
                a.init();
            } catch (Exception ignored) {}
        }

        private void mEditSSL(int position) {
            SSLDialog a = new SSLDialog(mContext);
            try {
                a.edit(mItemList.get(position).second);
                a.onPayloadAdd(json -> updateItem(position, json));
                a.init();
            } catch (Exception ignored) {}
        }

        private void updateItem(int position, JSONObject json) {
            Pair<Long, JSONObject> oldPair = mItemList.get(position);
            Pair<Long, JSONObject> newPair = new Pair<>(oldPair.first, json);
            
            // Update in both lists
            int originalIdx = originalList.indexOf(oldPair);
            if (originalIdx != -1) originalList.set(originalIdx, newPair);
            
            mItemList.set(position, newPair);
            notifyItemChanged(position);
            mListener.onReloadConfig(position);
        }
    }

    public String getNewJS() {
        try {
            JSONArray jarr = new JSONArray();
            for (Pair<Long, JSONObject> pair : originalList) {
                jarr.put(pair.second);
            }
            return jarr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filter(String charText) {
        String query = charText.toLowerCase(Locale.getDefault());
        ArrayList<Pair<Long, JSONObject>> filtered = new ArrayList<>();
        
        if (query.isEmpty()) {
            isConfigSearch = false;
            filtered.addAll(originalList);
        } else if (query.equals("custom")) {
            isConfigSearch = false;
            for (Pair<Long, JSONObject> pair : originalList) {
                JSONObject jo = pair.second;
                if (jo.has("isAddOrEdited") || jo.optString("Name").toLowerCase().contains("custom")) {
                    filtered.add(pair);
                }
            }
        } else {
            isConfigSearch = true;
            for (Pair<Long, JSONObject> pair : originalList) {
                JSONObject jo = pair.second;
                if (jo.optString("Name").toLowerCase().contains(query)) {
                    filtered.add(pair);
                }
            }
        }
        
        setItemList(filtered);
        notifyDataSetChanged();
    }

    private String getServerType(JSONObject js) {
        return switch (js.optInt("Category", -1)) {
            case 0 -> "Premium";   //OVPN
            case 1 -> "V2Ray";  //V2Ray
            case 2 -> "SSH";    //SSH
            case 3 -> "DNSTT";  //DNSTT
            case 4 -> "UDP";    //UDP
            default -> "PRO";   //PRO
        };
    }

    private String getNetworkType(JSONObject js) {
        try {
            int proto = js.optInt("proto_spin", 0);
            return switch (proto) {
                case 0 -> "TCP";    //TCP
                case 1 -> "HYST";   //HYST
                case 2 -> "DNS";    //DNS
                case 3 -> "SSL";    //SSL
                case 4 -> "SSL+";   //SSL+
                case 5 -> "PROXY";  //PROXY
                case 6 -> "XRAY";   //XRAY
                default -> "VPN";   //VPN
            };
        } catch (Exception e) {
            return "VPN";
        }
    }
}
