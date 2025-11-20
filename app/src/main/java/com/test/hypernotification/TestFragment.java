package com.test.hypernotification;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.database.Cursor;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.io.IOException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class TestFragment extends Fragment {

    private static final int REQ_PICK_IMAGE = 2001;

    private EditText etMerchant, etPickupCode, etCustomJson;
    private Button btnPickImage, btnUploadAndSend;
    private ImageView ivPreview;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private SharedPreferences prefs;
    private OkHttpClient httpClient = new OkHttpClient();

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivPreview.setImageURI(uri);
                }
            });

    public TestFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_test, container, false);
        etMerchant = v.findViewById(R.id.et_merchant);
        etPickupCode = v.findViewById(R.id.et_pickup_code);
        etCustomJson = v.findViewById(R.id.et_custom_json);
        btnPickImage = v.findViewById(R.id.btn_pick_image);
        btnUploadAndSend = v.findViewById(R.id.btn_upload_and_send);
        ivPreview = v.findViewById(R.id.iv_preview);
        progressBar = v.findViewById(R.id.progress_bar);

        prefs = requireActivity().getSharedPreferences("config", Activity.MODE_PRIVATE);

        btnPickImage.setOnClickListener(v1 -> pickImageLauncher.launch("image/*"));

        btnUploadAndSend.setOnClickListener(v1 -> {
            String merchant = etMerchant.getText().toString().trim();
            String code = etPickupCode.getText().toString().trim();
            String customJson = etCustomJson.getText().toString().trim();

            if (TextUtils.isEmpty(merchant) || TextUtils.isEmpty(code)) {
                Toast.makeText(requireContext(), "请填写商家和取餐码", Toast.LENGTH_SHORT).show();
                return;
            }

            JsonObject paramV2;
            if (!TextUtils.isEmpty(customJson)) {
                try {
                    paramV2 = JsonParser.parseString(customJson).getAsJsonObject();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "自定义 JSON 格式错误", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // 使用与 sendFocusNotification 相同结构但替换商家与取餐码
                paramV2 = new JsonObject();
                paramV2.addProperty("protocol", 1);
                paramV2.addProperty("aodTitle", "取餐码: " + code);
                paramV2.addProperty("business", "order_pending");
                paramV2.addProperty("ticker", "取餐提醒");
                paramV2.addProperty("isShowNotification", true);
                paramV2.addProperty("enableFloat", true);
                paramV2.addProperty("timeout", 60);
                paramV2.addProperty("updatable", true);

                JsonObject baseInfo = new JsonObject();
                baseInfo.addProperty("type", 1);
                baseInfo.addProperty("title", code);
                baseInfo.addProperty("content", "取餐码");
                paramV2.add("baseInfo", baseInfo);

                JsonObject hintInfo = new JsonObject();
                hintInfo.addProperty("type", 1);
                hintInfo.addProperty("title", merchant);
                hintInfo.addProperty("content", "商家");
                paramV2.add("hintInfo", hintInfo);

                JsonObject paramIsland = new JsonObject();
                paramIsland.addProperty("islandProperty", 1);
                paramIsland.addProperty("islandTimeout", 900);
                paramIsland.addProperty("highlightColor", "#FFBB0F");

                JsonObject bigIslandArea = new JsonObject();
                JsonObject imageTextInfoLeft = new JsonObject();
                JsonObject textInfoLeft = new JsonObject();
                textInfoLeft.addProperty("title", "取餐码");
                textInfoLeft.addProperty("showHighlightColor", true);
                imageTextInfoLeft.add("textInfo", textInfoLeft);
                imageTextInfoLeft.addProperty("type", 1);
                bigIslandArea.add("imageTextInfoLeft", imageTextInfoLeft);

                JsonObject imageTextInfoRight = new JsonObject();
                JsonObject textInfoRight = new JsonObject();
                textInfoRight.addProperty("title", code);
                imageTextInfoRight.add("textInfo", textInfoRight);
                imageTextInfoRight.addProperty("type", 2);
                bigIslandArea.add("imageTextInfoRight", imageTextInfoRight);

                paramIsland.add("bigIslandArea", bigIslandArea);
                paramV2.add("param_island", paramIsland);
            }

            // 如果选择了图片则先上传
            if (selectedImageUri != null) {
                uploadImageThenSend(selectedImageUri, paramV2);
            } else {
                // 直接发送通知（不上传图片）
                FocusNotificationHelper.sendFocusNotificationWithCustomParams(requireContext(), paramV2);
                Toast.makeText(requireContext(), "已发送焦点通知（未上传图片）", Toast.LENGTH_SHORT).show();
            }
        });

        return v;
    }

    private void uploadImageThenSend(Uri uri, JsonObject paramV2) {
        String picgoUrl = prefs.getString("picgo_url", "");
        String picgoKey = prefs.getString("picgo_key", "");

        if (TextUtils.isEmpty(picgoUrl)) {
            Toast.makeText(requireContext(), "未配置 PicGo 上传地址，请在设置中填写 picgo_url", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        try {
            // 读取文件内容到 buffer（注意：此处简化为一次性读取，适用于小图片）
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            Buffer buf = new Buffer();
            buf.readFrom(is);
            byte[] bytes = buf.readByteArray();

            // 构造 multipart 请求（PicGo 常见接口）
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "upload.jpg",
                            RequestBody.create(bytes, MediaType.parse("image/jpeg")))
                    .build();

            Request.Builder reqBuilder = new Request.Builder()
                    .url(picgoUrl)
                    .post(requestBody);

            if (!TextUtils.isEmpty(picgoKey)) {
                reqBuilder.addHeader("Authorization", picgoKey);
            }

            Request request = reqBuilder.build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "图片上传失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        String resp = response.body() != null ? response.body().string() : "";
                        // 简单尝试解析返回的直链（因各图床返回不同，需要根据 PicGo 配置调整）
                        // 将图片 url 放进 paramV2.hintInfo.content（仅示例）
                        if (!resp.isEmpty()) {
                            // 这里示例：把返回文本放到 hintInfo.extraImageResponse
                            JsonObject hintInfo = paramV2.has("hintInfo") ? paramV2.getAsJsonObject("hintInfo") : new JsonObject();
                            hintInfo.addProperty("uploadedImageResponse", resp);
                            paramV2.add("hintInfo", hintInfo);
                        }

                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            FocusNotificationHelper.sendFocusNotificationWithCustomParams(requireContext(), paramV2);
                            Toast.makeText(requireContext(), "图片上传并发送通知完成", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "解析上传响应失败", Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "读取图片失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}