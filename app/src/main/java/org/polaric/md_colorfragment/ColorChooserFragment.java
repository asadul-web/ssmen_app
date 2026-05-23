package org.polaric.md_colorfragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import com.v2ray.ang.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.ColorInt;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import mtkdex.core.build.ssmen.config.ConfigUtil;

import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.fragment.app.DialogFragment;
import android.widget.LinearLayout;
import mtkdex.core.build.ssmen.config.SettingsConstants;
import mtkdex.core.build.ssmen.utils.util;

public class ColorChooserFragment extends DialogFragment implements SettingsConstants, View.OnClickListener, View.OnLongClickListener {

    private int mCircleSize;
    private ColorFragmentCallback mCallback;
    private GridView mGrid;
    private int mPreselect;
    private boolean mSetPreselectionColor = false;
    private int mSubIndex=-1;
    private int mTopIndex=-1;
    private boolean mInSub=false;
    private String name = "ColorFragment";
    private Activity activity;
    private View root;
    @NonNull
    private int[] mColorsTop;
    @Nullable
    private int[][] mColorsSub;
    
    @NonNull
    public ColorChooserFragment preselect(@ColorInt int preselect) {
        mPreselect = preselect;
        mSetPreselectionColor = true;
        return this;
    }

    @NonNull
    public ColorChooserFragment setName(String name) {
        this.name=name;
        return this;
    }

    private void invalidate() {
        if (mGrid.getAdapter() == null) {
            mGrid.setAdapter(new ColorGridAdapter(activity));
            mGrid.setSelector(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.bg_transparent, null));
        } else ((BaseAdapter) mGrid.getAdapter()).notifyDataSetChanged();
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.activity = getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        this.activity = getActivity();
        getDialog().setCanceledOnTouchOutside(false);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        mCallback.onFragmentDone("cancel");
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        ConfigUtil mConfig = ConfigUtil.getInstance(activity);
        root = getLayoutInflater().inflate(R.layout.colorchooser, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        int preselectColor;
        try {
            RelativeLayout apply = root.findViewById(R.id.apply);
            LinearLayout back = root.findViewById(R.id.back);
            ((TextView) root.findViewById(R.id.back_tv)).setTextColor(mConfig.getColorAccent());
            root.findViewById(R.id.color_bg).setBackgroundColor(mConfig.getColorAccent());
            apply.setBackgroundTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
            // Fix text color logic - in dark mode (getAppThemeUtil() == true) text should be WHITE, in light mode BLACK
            ((TextView)root.findViewById(R.id.notiftext1)).setTextColor(mConfig.getAppThemeUtil()? Color.WHITE : Color.BLACK);
            ((TextView)root.findViewById(R.id.apply_tv)).setTextColor(mConfig.getAppThemeUtil()? Color.WHITE : Color.BLACK);
            RadioGroup rg = root.findViewById(R.id.dialog_tunnel_radio);
            RadioButton r1 = root.findViewById(R.id.type_light);
            RadioButton r2 = root.findViewById(R.id.type_dark);
            
            // Set text color for radio buttons based on theme
            int textColor = mConfig.getAppThemeUtil() ? Color.WHITE : Color.BLACK;
            r1.setTextColor(textColor);
            r2.setTextColor(textColor);
            rg.check(mConfig.getAppThemeUtil()? R.id.type_dark:R.id.type_light);
            rg.setOnCheckedChangeListener((radioGroup, id) -> {
                mConfig.setAppThemeUtil(id==R.id.type_dark);
                getDialog().dismiss();
                activity.recreate();
            });
            r1.setTextColor(mConfig.gettextColor());
            r2.setTextColor(mConfig.gettextColor());
            r1.setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
            r2.setButtonTintList(ColorStateList.valueOf(mConfig.getColorAccent()));
            apply.setOnClickListener(this);
            back.setOnClickListener(this);
            generateColors();
        }catch (Exception x){
            util.showToast("Color chooser error",x.toString());
        }
        if (savedInstanceState == null) {
            if (mSetPreselectionColor) {
                preselectColor = mPreselect;
                if (preselectColor != 0) {
                    for (int topIndex = 0; topIndex < mColorsTop.length; topIndex++) {
                        if (mColorsTop[topIndex] == preselectColor) {
                            topIndex(topIndex);
                            if (mColorsSub != null) {
                                findSubIndexForColor(topIndex, preselectColor);
                            } else {
                                subIndex(5);
                            }
                            break;
                        }
                        if (mColorsSub != null) {
                            for (int subIndex = 0; subIndex < mColorsSub[topIndex].length; subIndex++) {
                                if (mColorsSub[topIndex][subIndex] == preselectColor) {
                                    topIndex(topIndex);
                                    subIndex(subIndex);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        mCircleSize = 56;
        mGrid = root.findViewById(R.id.grid);
        invalidate();
        builder.setView(root);
        builder.setCancelable(false);
        AlertDialog alertDialog = builder.create();
        return alertDialog;
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        this.activity = activity;
        super.onAttach(activity);
        if (!(activity instanceof ColorFragmentCallback))
            throw new IllegalStateException("ColorChooserFragment needs to be created from an Activity implementing ColorFragmentCallback.");
        mCallback = (ColorFragmentCallback) activity;
    }



    private boolean isInSub() {
        return mInSub;
    }

    private void isInSub(boolean value) {
        mInSub=value;
    }

    private int topIndex() {
        return mTopIndex;
    }

    public void topIndex(int value) {
        if (topIndex() != value && value > -1)
            findSubIndexForColor(value, mColorsTop[value]);
        mTopIndex=value;
    }

    private int subIndex() {
        if (mColorsSub == null) return -1;
        return mSubIndex;
    }

    public void subIndex(int value) {
        if (mColorsSub == null) return;
        mSubIndex=value;
    }

    private void generateColors() {
        mColorsTop = ColorPalette.PRIMARY_COLORS;
        mColorsSub = ColorPalette.PRIMARY_COLORS_SUB;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() != null) {
            final String[] tag = ((String) v.getTag()).split(":");
            final int index = Integer.parseInt(tag[0]);
            int color = Integer.parseInt(tag[1]);
            if (isInSub()) {
                subIndex(index);
            } else {
                topIndex(index);
                if (mColorsSub != null && index < mColorsSub.length) {
                    isInSub(true);
                }
            }
            mCallback.onColorSelection(color);
        }
        if (v.getId()==R.id.apply) {
            mCallback.onFragmentDone("Apply");
            getDialog().dismiss();
        } else if (v.getId()==R.id.back) {
            if(!isInSub()){
                mCallback.onFragmentDone("cancel");
                getDialog().dismiss();
            }
            isInSub(false);
        }
        invalidate();
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getTag() != null) {
            final String[] tag = ((String) v.getTag()).split(":");
            final int color = Integer.parseInt(tag[1]);
            ((CircleView) v).showHint(color);
            return true;
        }
        return false;
    }

    public interface ColorFragmentCallback {
        void onColorSelection(@ColorInt int selectedColor);
        void onFragmentDone(String name);
    }

    private void findSubIndexForColor(int topIndex, int color) {
        if (mColorsSub == null || mColorsSub.length - 1 < topIndex)
            return;
        int[] subColors = mColorsSub[topIndex];
        for (int subIndex = 0; subIndex < subColors.length; subIndex++) {
            if (subColors[subIndex] == color) {
                subIndex(subIndex);
                break;
            }
        }
    }

    private class ColorGridAdapter extends BaseAdapter {
        Activity activity;
        public ColorGridAdapter(Activity activity) {
            this.activity = activity;
        }

        @Override
        public int getCount() {
            if (isInSub()) return mColorsSub[topIndex()].length;
            else return mColorsTop.length;
        }

        @Override
        public Object getItem(int position) {
            if (isInSub()) return mColorsSub[topIndex()][position];
            else return mColorsTop[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressWarnings("ResourceAsColor")
        @SuppressLint("DefaultLocale")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new CircleView(activity);
                convertView.setLayoutParams(new GridView.LayoutParams(mCircleSize, mCircleSize));
            }
            CircleView child = (CircleView) convertView;
            final int color = isInSub() ? mColorsSub[topIndex()][position] : mColorsTop[position];
            child.setBackgroundColor(color);
            if (isInSub())
                child.setSelected(subIndex() == position);
            else child.setSelected(topIndex() == position);
            child.setTag(String.format("%d:%d", position, color));
            child.setOnClickListener(ColorChooserFragment.this);
            child.setOnLongClickListener(ColorChooserFragment.this);
            return convertView;
        }
    }

}
