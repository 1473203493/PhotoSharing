package Fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.PhotoShare.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

public class AddFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_PERMISSION_CODE = 100;
    private EditText titleEditText, contentEditText;
    private Button selectedImageView;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private Button shareButton,saveButton;
    private OkHttpClient client;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        titleEditText = view.findViewById(R.id.add_title);
        contentEditText = view.findViewById(R.id.add_content);
        selectedImageView = view.findViewById(R.id.add_img);
        shareButton = view.findViewById(R.id.add_button);
        saveButton =view.findViewById(R.id.save_button);
        client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        checkPermissions();

        selectedImageView.setOnClickListener(v -> openImagePicker());
        shareButton.setOnClickListener(v -> uploadPost());
        saveButton.setOnClickListener(v -> savepost());
        return view;
    }
//请求访问外部储存权限
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
        }
    }
//处理请求权限失败
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
//选择图片
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");//图片类型
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
            handleImageSelection(data);
        }
    }
//处理图片选择
    private void handleImageSelection(Intent data) {
   //选择多个图片
        if (data.getClipData() != null) {
            selectedImageUris.clear();
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                selectedImageUris.add(imageUri);
            }
            Log.d("ImagePicker", "Selected " + count + " images");
            if (!selectedImageUris.isEmpty()) {
                displayImage(selectedImageUris.get(0));
            }
        } 
        //选择单个图片
        else if (data.getData() != null) {
            Uri imageUri = data.getData();
            selectedImageUris.clear();
            selectedImageUris.add(imageUri);
            displayImage(imageUri);
        }
    }
//展示图片，用“选择图片”的按钮做位置
    private void displayImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
            selectedImageView.setBackground(new BitmapDrawable(getResources(), bitmap));
        } catch (IOException e) {
            Log.e("ImageDisplayError", e.getMessage(), e);
        }
    }

    private void uploadPost() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty() || selectedImageUris.isEmpty()) {
            Toast.makeText(getActivity(), "Please fill all fields and select at least one image", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadImages(selectedImageUris, imageCode -> {
            if (imageCode != null) {
                postTextAndImage(title, content, imageCode);
            } else {
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Image upload failed", Toast.LENGTH_SHORT).show());
            }
        });
    }
    private void savepost() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty() || selectedImageUris.isEmpty()) {
            Toast.makeText(getActivity(), "Please fill all fields and select at least one image", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadImages(selectedImageUris, imageCode -> {
            if (imageCode != null) {
                save(title, content, imageCode);
            } else {
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Image upload failed", Toast.LENGTH_SHORT).show());
            }
        });
    }
    private void uploadImages(List<Uri> imageUris, OnImageUploadedListener listener) {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        for (Uri imageUri : imageUris) {
            try {
                String filePath = getPathFromUri(imageUri);
                File file = new File(filePath);
                RequestBody fileBody = RequestBody.create(file, MediaType.parse("image/*"));
                multipartBuilder.addFormDataPart("fileList", file.getName(), fileBody);
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
                    String imageCode = data.optString("imageCode", null);
                    Log.d("UploadImages", "Extracted imageCode: " + imageCode);
                    listener.onImageUploaded(imageCode);
                } else {
                    listener.onImageUploaded(null);
                }
            } else {
                listener.onImageUploaded(null);
            }
        } catch (JSONException e) {
            Log.e("ImageUploadResponseError", e.getMessage(), e);
            listener.onImageUploaded(null);
        }
    }

    private String getPathFromUri(Uri uri) {
        try (InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                File file = new File(requireActivity().getCacheDir(), "image_" + System.currentTimeMillis() + ".jpg");
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                    }
                }
                return file.getAbsolutePath();
            }
        } catch (IOException e) {
            Log.e("FilePathError", e.getMessage(), e);
        }
        return null;
    }

    private void postTextAndImage(String title, String content, String imageCode) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        long userId = sharedPreferences.getLong("user_id", 0);

        JSONObject postData = new JSONObject();
        try {
            postData.put("title", title);
            postData.put("content", content);
            postData.put("imageCode", imageCode);
            postData.put("pUserId", userId);
        } catch (JSONException e) {
            Log.e("PostDataError", e.getMessage(), e);
        }

        RequestBody requestBody = RequestBody.create(postData.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api-store.openguet.cn/api/member/photo/share/add")
                .post(requestBody)
                .addHeader("appId", "750baf37e04f432e8425dfa10a21b4e9")
                .addHeader("appSecret", "8012134351201e3b043429ff5761d21e1eafd")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("PostError", e.getMessage(), e);
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Share failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "发布成功", Toast.LENGTH_SHORT).show());
                } else {
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Share failed", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void save(String title, String content, String imageCode) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        long userId = sharedPreferences.getLong("user_id", 0);

        JSONObject postData = new JSONObject();
        try {
            postData.put("title", title);
            postData.put("content", content);
            postData.put("imageCode", imageCode);
            postData.put("pUserId", userId);
        } catch (JSONException e) {
            Log.e("SaveDataError", e.getMessage(), e);
        }

        RequestBody requestBody = RequestBody.create(postData.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api-store.openguet.cn/api/member/photo/share/save")
                .post(requestBody)
                .addHeader("appId", "750baf37e04f432e8425dfa10a21b4e9")
                .addHeader("appSecret", "8012134351201e3b043429ff5761d21e1eafd")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("saveError", e.getMessage(), e);
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Share failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "保存成功", Toast.LENGTH_SHORT).show());
                } else {
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Share failed", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
    private interface OnImageUploadedListener {
        void onImageUploaded(String imageCode);
    }
}
