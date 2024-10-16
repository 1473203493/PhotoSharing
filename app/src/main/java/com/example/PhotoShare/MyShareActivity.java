
package com.example.PhotoShare;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import entity.Card;
import Adapter.CardAdapter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MyShareActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CardAdapter cardAdapter;
    private List<Card> cardList = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient();
    private ImageButton btnBack;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);

        // 初始化 RecyclerView
        recyclerView = findViewById(R.id.recycler_view_my_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cardAdapter = new CardAdapter(cardList);
        recyclerView.setAdapter(cardAdapter);

        btnBack = findViewById(R.id.btn_back);
        // 设置返回按钮点击事件
        btnBack.setOnClickListener(v -> onBackPressed());

        // 获取用户信息
        sharedPreferences = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);

        // 获取点赞的图文列表
        fetchLikedPhotos();


    }

    private void fetchLikedPhotos() {
        long userId = sharedPreferences.getLong("user_id", 0);  // 获取当前登录用户ID

        String url = "https://api-store.openguet.cn/api/member/photo/share/myself?userId=" + userId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("appId", "750baf37e04f432e8425dfa10a21b4e9")
                .addHeader("appSecret", "8012134351201e3b043429ff5761d21e1eafd")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Error", "Network Error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(MyShareActivity.this, "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.e("Error", "Unsuccessful response: " + response.code() + " - " + responseBody);
                    runOnUiThread(() ->
                            Toast.makeText(MyShareActivity.this, "获取数据失败: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                } else {
                    String responseData = response.body().string();
                    Log.d("Success", "Response: " + responseData); // 打印 API 响应
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        int code = jsonResponse.optInt("code", -1);
                        if (code == 200) {
                            JSONArray records = jsonResponse.getJSONObject("data").getJSONArray("records");
                            for (int i = 0; i < records.length(); i++) {
                                JSONObject record = records.getJSONObject(i);
                                String title = record.getString("title");
                                String content = record.getString("content");
                                String username = record.getString("username");
                                String avatar = record.optString("avatar", ""); // 假设有 avatarUrl 字段
                                int likeCount = record.optInt("likeNum", 0); // 获取 likeNum
                                JSONArray imageUrlList = record.getJSONArray("imageUrlList");

                                List<String> images = new ArrayList<>();
                                for (int j = 0; j < imageUrlList.length(); j++) {
                                    images.add(imageUrlList.getString(j));
                                }

                                long id = record.getLong("id");  // 图文分享的主键ID
                                long likeId = record.optLong("likeId", -1);  // 点赞表主键ID，未点赞时为 -1
                                boolean isLiked = likeId != -1;  // 如果 likeId 不为 -1，则表示已点赞

                                // 创建 Card 对象，并将其添加到列表中
                                Card card = new Card(title, content, username, avatar, likeCount, images, id, likeId, isLiked);
                                cardList.add(card);
                            }
                            runOnUiThread(() -> cardAdapter.notifyDataSetChanged());
                        } else {
                            String message = jsonResponse.optString("msg", "Unknown error");
                            runOnUiThread(() ->
                                    Toast.makeText(MyShareActivity.this, "获取数据失败: " + message, Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (Exception e) {
                        Log.e("Error", "Error parsing response", e);
                        runOnUiThread(() ->
                                Toast.makeText(MyShareActivity.this, "解析响应错误", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }
        });
    }



}