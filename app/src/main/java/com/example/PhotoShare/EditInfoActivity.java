package com.example.PhotoShare;
import static android.app.PendingIntent.getActivity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.squareup.picasso.Picasso;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EditInfoActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_PERMISSION_CODE = 100;
    private EditText etUsername, etIntroduce;
    private RadioGroup rgSex;
    private RadioButton rbMale, rbFemale;
    private Button btnUpdateInfo;
    private ImageButton etAvatar,btnBack;
    private long userId;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_info);

        // 绑定控件
        etAvatar = findViewById(R.id.edit_avatar);
        etUsername = findViewById(R.id.et_username);
        rgSex = findViewById(R.id.rg_sex);
        rbMale = findViewById(R.id.rb_male);
        rbFemale = findViewById(R.id.rb_female);
        etIntroduce = findViewById(R.id.et_introduce);
        btnUpdateInfo = findViewById(R.id.btn_update_info);
        btnBack = findViewById(R.id.btn_back);

        // 设置返回按钮点击事件
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        // 加载用户信息
        loadUserInfo();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_CODE);
        }

        // 头像点击事件
        etAvatar.setOnClickListener(v -> openImagePicker());

        // 保存按钮点击事件
        btnUpdateInfo.setOnClickListener(v -> updateUser());
    }
    private InputStream getInputStreamFromUri(Uri uri) throws IOException {
        return getContentResolver().openInputStream(uri);
    }

    private void loadUserInfo() {
        // 从 SharedPreferences 中加载用户信息
        SharedPreferences sharedPreferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
        userId = sharedPreferences.getLong("user_id", -1);  // 获取用户 ID
        String username = sharedPreferences.getString("username", "");
        int sex = sharedPreferences.getInt("sex", 1);  // 默认性别为男性
        String introduce = sharedPreferences.getString("introduce", "");
        String avatarUrl = sharedPreferences.getString("avatar", "");

        // 更新UI
        etUsername.setText(username);
        if (sex == 1) {
            rbMale.setChecked(true);
        } else {
            rbFemale.setChecked(true);
        }
        etIntroduce.setText(introduce);
        if (!avatarUrl.isEmpty()) {
            Picasso.get().load(avatarUrl).into(etAvatar);
        }
    }


    private void updateUser() {
        String newUsername = etUsername.getText().toString();
        int newSex = rgSex.getCheckedRadioButtonId() == R.id.rb_male ? 1 : 2;
        String newIntroduce = etIntroduce.getText().toString();

        if (selectedImageUris.isEmpty()) {
            // 没有选择头像，不更新头像信息
            sendUpdateRequest(null, newUsername, newSex, newIntroduce);
        } else {
            uploadImages(selectedImageUris, imageUrl -> {
                if (imageUrl != null) {
                    sendUpdateRequest(imageUrl, newUsername, newSex, newIntroduce);
                } else {
                    runOnUiThread(() -> Toast.makeText(EditInfoActivity.this, "头像上传失败", Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    private void sendUpdateRequest(String avatarUrl, String username, int sex, String introduce) {
        JSONObject editData = new JSONObject();
        try {
            editData.put("introduce", introduce);
            editData.put("sex", sex);
            editData.put("username", username);
            editData.put("id", userId);
            if (avatarUrl != null) {
                editData.put("avatar", avatarUrl);
            }
        } catch (JSONException e) {
            Log.e("EditDataError", e.getMessage(), e);
        }

        RequestBody requestBody = RequestBody.create(editData.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api-store.openguet.cn/api/member/photo/user/update")
                .post(requestBody)
                .addHeader("appId", "750baf37e04f432e8425dfa10a21b4e9")
                .addHeader("appSecret", "8012134351201e3b043429ff5761d21e1eafd")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("EditInfoError", "Network Error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(EditInfoActivity.this, "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.e("EditInfoError", "Unsuccessful response: " + response.code() + " - " + responseBody);
                    runOnUiThread(() -> Toast.makeText(EditInfoActivity.this, "更新失败: " + response.message(), Toast.LENGTH_SHORT).show());
                } else {
                    String responseData = response.body().string();
                    Log.d("EditInfoSuccess", "Response: " + responseData);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        int code = jsonResponse.optInt("code", -1);
                        String message = jsonResponse.optString("msg", "Unknown error");

                        if (code == 200) {
                            runOnUiThread(() -> {
                                Toast.makeText(EditInfoActivity.this, "信息更新成功！", Toast.LENGTH_SHORT).show();
                                finish();  // 更新成功后返回上一页
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(EditInfoActivity.this, "更新失败: " + message, Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        Log.e("EditInfoError", "Error parsing response", e);
                        runOnUiThread(() -> Toast.makeText(EditInfoActivity.this, "解析响应错误", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                selectedImageUris.clear();
                selectedImageUris.add(imageUri);
                displayImage(imageUri);
            }
        }
    }

    private void displayImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            etAvatar.setImageBitmap(bitmap);
        } catch (IOException e) {
            Log.e("ImageDisplayError", e.getMessage(), e);
        }
    }

    private void uploadImages(List<Uri> imageUris, OnImageUploadedListener listener) {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        for (Uri imageUri : imageUris) {
            try (InputStream inputStream = getInputStreamFromUri(imageUri)) {
                if (inputStream != null) {
                    File file = new File(getCacheDir(), "image_" + System.currentTimeMillis() + ".jpg");
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    RequestBody fileBody = RequestBody.create(file, MediaType.parse("image/*"));
                    multipartBuilder.addFormDataPart("fileList", file.getName(), fileBody);
                }
            } catch (Exception e) {
                Log.e("ImageUploadError", e.getMessage(), e);
                listener.onImageUploaded(null);
                return;
            }
        }

        RequestBody multipartBody = multipartBuilder.build();
        Request request = new Request.Builder()
                .url("https://api-store.openguet.cn/api/member/photo/image/upload")
                .post(multipartBody)
                .addHeader("appId", "750baf37e04f432e8425dfa10a21b4e9")
                .addHeader("appSecret", "8012134351201e3b043429ff5761d21e1eafd")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("UploadImagesError", e.getMessage(), e);
                listener.onImageUploaded(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    handleImageUploadResponse(response.body().string(), listener);
                } else {
                    listener.onImageUploaded(null);
                }
            }
        });
    }


    private void handleImageUploadResponse(String responseBody, OnImageUploadedListener listener) {
        try {
            JSONObject json = new JSONObject(responseBody);
            if (json.optInt("code", -1) == 200) {
                JSONObject data = json.optJSONObject("data");
                if (data != null) {
                    String imageUrl = data.optJSONArray("imageUrlList").optString(0, null);
                    Log.d("UploadImages", "Extracted image URL: " + imageUrl);
                    listener.onImageUploaded(imageUrl);
                } else {
                    listener.onImageUploaded(null);
                }
            } else {
                listener.onImageUploaded(null);
            }
        } catch (Exception e) {
            Log.e("UploadImagesError", "Error parsing upload response", e);
            listener.onImageUploaded(null);
        }
    }

    private interface OnImageUploadedListener {
        void onImageUploaded(String imageUrl);
    }
}
