package com.test.hypernotification;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment {

    private TextInputEditText etToken;
    private TextInputEditText etPicgoKey;
    private TextInputEditText etPicgoUrl;
    private TextInputEditText etAlbumId;
    private TextInputEditText etTileDelay; // 添加这个变量
    private SwitchMaterial switchRootMode;
    private Button btnSave;
    private SeekBar seekTileDelay; // 添加这个变量

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        initViews(view);
        loadPreferences();

        return view;
    }

    private void initViews(View view) {
        etToken = view.findViewById(R.id.et_token);
        etPicgoKey = view.findViewById(R.id.et_picgo_key);
        etPicgoUrl = view.findViewById(R.id.et_picgo_url);
        etAlbumId = view.findViewById(R.id.et_album_id);
        switchRootMode = view.findViewById(R.id.switch_root_mode);
        btnSave = view.findViewById(R.id.btn_save);
        
        // 初始化磁贴延迟相关的视图
        seekTileDelay = view.findViewById(R.id.seek_tile_delay);
        etTileDelay = view.findViewById(R.id.et_tile_delay);

        prefs = requireContext().getSharedPreferences("config", Context.MODE_PRIVATE);

        // 设置默认值
        etPicgoUrl.setText("https://www.picgo.net/api/1/upload");

        // 加载 root 模式设置
        switchRootMode.setChecked(prefs.getBoolean("use_root_mode", false));

        // 检查 root 权限
        if (switchRootMode.isChecked()) {
            checkRootAccess();
        }

        switchRootMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (RootScreenshotHelper.isRootAvailable()) {
                    prefs.edit().putBoolean("use_root_mode", true).apply();
                    Toast.makeText(getContext(), "已启用 Root 模式截图", Toast.LENGTH_SHORT).show();
                } else {
                    switchRootMode.setChecked(false);
                    Toast.makeText(getContext(), "Root 权限不可用", Toast.LENGTH_SHORT).show();
                    prefs.edit().putBoolean("use_root_mode", false).apply();
                }
            } else {
                prefs.edit().putBoolean("use_root_mode", false).apply();
                Toast.makeText(getContext(), "已切换到普通模式截图", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> saveConfiguration());
        
        // 加载磁贴延迟设置
        int tileDelay = prefs.getInt("tile_click_delay", 1000);
        seekTileDelay.setProgress(tileDelay);
        etTileDelay.setText(String.valueOf(tileDelay));
        
        // 滑块监听
        seekTileDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    etTileDelay.setText(String.valueOf(progress));
                }
            }
        
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
        
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 输入框监听
        etTileDelay.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    int delay = Integer.parseInt(etTileDelay.getText().toString());
                    delay = Math.max(0, Math.min(5000, delay));
                    seekTileDelay.setProgress(delay);
                    etTileDelay.setText(String.valueOf(delay));
                } catch (NumberFormatException e) {
                    // 输入无效时恢复滑块值
                    etTileDelay.setText(String.valueOf(seekTileDelay.getProgress()));
                }
            }
        });
    }

    private void loadPreferences() {
        etToken.setText(prefs.getString("token", ""));
        etPicgoKey.setText(prefs.getString("picgo_key", ""));
        etPicgoUrl.setText(prefs.getString("picgo_url", "https://www.picgo.net/api/1/upload"));
        etAlbumId.setText(prefs.getString("album_id", ""));
        
        // 加载磁贴延迟设置
        int tileDelay = prefs.getInt("tile_click_delay", 1000);
        if (seekTileDelay != null) {
            seekTileDelay.setProgress(tileDelay);
        }
        if (etTileDelay != null) {
            etTileDelay.setText(String.valueOf(tileDelay));
        }
    }

    private void saveConfiguration() {
        String token = etToken.getText().toString().trim();
        String picgoKey = etPicgoKey.getText().toString().trim();
        String picgoUrl = etPicgoUrl.getText().toString().trim();
        String albumId = etAlbumId.getText().toString().trim();

        if (TextUtils.isEmpty(token) || TextUtils.isEmpty(picgoKey) ||
            TextUtils.isEmpty(picgoUrl) || TextUtils.isEmpty(albumId)) {
            Toast.makeText(getContext(), "请填写所有配置项", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("token", token);
        editor.putString("picgo_key", picgoKey);
        editor.putString("picgo_url", picgoUrl);
        editor.putString("album_id", albumId);
        
        // 保存磁贴延迟设置
        try {
            int tileDelay = Integer.parseInt(etTileDelay.getText().toString());
            tileDelay = Math.max(0, Math.min(5000, tileDelay));
            editor.putInt("tile_click_delay", tileDelay);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "磁贴延迟格式错误", Toast.LENGTH_SHORT).show();
            return;
        }
        
        editor.apply();

        Toast.makeText(getContext(), "配置已保存", Toast.LENGTH_SHORT).show();
    }

    private void checkRootAccess() {
        if (!RootScreenshotHelper.isRootAvailable()) {
            switchRootMode.setChecked(false);
            prefs.edit().putBoolean("use_root_mode", false).apply();
            Toast.makeText(getContext(), "Root 权限不可用，已切换到普通模式", Toast.LENGTH_SHORT).show();
        }
    }
}