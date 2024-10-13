package com.example.PhotoShare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;

import org.json.JSONObject;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "LoginPrefs";//记住密码
    private Boolean bPwdSwitch = false;
    private EditText etPwd;
    private final OkHttpClient client = new OkHttpClient();
    private EditText etUsername;
    private EditText etPassword;
    private TextView tvSignUp;
    private CheckBox cbRememberPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.bt_login);
        tvSignUp = findViewById(R.id.tv_sign_up);
        cbRememberPwd = findViewById(R.id.cb_remember_pwd);

        final ImageView ivPwdSwitch = findViewById(R.id.iv_pwd_switch);
        etPwd = findViewById(R.id.et_password);

        // 加载保存的用户名和密码
        loadPreferences();

        // 显示密码
        ivPwdSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bPwdSwitch = !bPwdSwitch;
                if (bPwdSwitch) {
                    ivPwdSwitch.setImageResource(
                            R.drawable.baseline_visibility_24);
                    etPwd.setInputType(
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    ivPwdSwitch.setImageResource(
                            R.drawable.baseline_visibility_off_24);
                    etPwd.setInputType(
                            InputType.TYPE_TEXT_VARIATION_PASSWORD |
                                    InputType.TYPE_CLASS_TEXT);
                    etPwd.setTypeface(Typeface.DEFAULT);
                }
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "请输入完用户名和密码", Toast.LENGTH_SHORT).show();
                } else {
                    loginUser(username, password);
                }
            }
        });

        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loadPreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (settings.getBoolean("remember", false)) {
            String username = settings.getString("username", "");
            String password = settings.getString("password", "");
            etUsername.setText(username);
            etPassword.setText(password);
            cbRememberPwd.setChecked(true);
        }
    }
    //记住密码
    private void savePreferences(String username, String password, boolean remember) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("remember", remember);
        if (remember) {
            editor.putString("username", username);
            editor.putString("password", password);
        } else {
            editor.remove("username");
            editor.remove("password");
        }
        editor.apply();
    }

    // 基于API接口登录功能
    public void loginUser(String username, String password) {
        String url = "https://api-store.openguet.cn/api/member/photo/user/login";

        // 构建请求体
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();

        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .addHeader("appId", "63460c96c2fb45738d9cdc7deebcdde3")
                .addHeader("appSecret", "942526cc88c2a0b54411d8472919aa9ffdcfa")
                .addHeader("Content-Type", "application/json")
                .post(formBody)
                .build();

        // 发起异步请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 请求失败时的处理
                Log.e("LoginError", "Network Error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // 处理非成功响应
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.e("LoginError", "Unsuccessful response: " + response.code() + " - " + responseBody);
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "登录失败: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                } else {
                    // 处理成功响应
                    String responseData = response.body().string();
                    Log.d("LoginSuccess", "Response: " + responseData);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        int code = jsonResponse.optInt("code", -1);
                        String message = jsonResponse.optString("msg", "Unknown error");

                        if (code == 200) {
                            JSONObject data = jsonResponse.getJSONObject("data");
                            long userId = data.getLong("id");
                            String username = data.getString("username");
                            String avatar = data.getString("avatar");
                            int sex = data.optInt("sex", 1);  // 默认性别为男
                            String introduce = data.optString("introduce", "暂无介绍");
                            // 保存用户名和密码
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                                savePreferences(username, password, cbRememberPwd.isChecked());

                                // 保存用户信息到 SharedPreferences
                                SharedPreferences sharedPreferences = getSharedPreferences("UserInfo", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putLong("user_id", userId);
                                editor.putString("username", username);
                                editor.putString("avatar", avatar);
                                editor.putInt("sex", sex);
                                editor.putString("introduce", introduce);
                                editor.apply();

                                // 跳转到 MainActivity
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "登录失败: " + message, Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (Exception e) {
                        Log.e("LoginError", "Error parsing response", e);
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }

        });
    }
}