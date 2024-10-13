package com.example.PhotoShare;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tool.RecyclerItemClickListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MyPostsActivity extends AppCompatActivity {

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

        // 添加长按删除功能
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // 普通点击事件
            }

            @Override
            public void onLongItemClick(View view, int position) {
                // 处理长按删除操作
                showDeleteConfirmationDialog(position);
            }
        }));
    }

    private void fetchLikedPhotos() {
        long userId = sharedPreferences.getLong("user_id", 0);  // 获取当前登录用户ID

        String url = "https://api-store.openguet.cn/api/member/photo/share/myself?userId=" + userId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("appId", "63460c96c2fb45738d9cdc7deebcdde3")
                .addHeader("appSecret", "942526cc88c2a0b54411d8472919aa9ffdcfa")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Error", "Network Error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(MyPostsActivity.this, "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.e("Error", "Unsuccessful response: " + response.code() + " - " + responseBody);
                    runOnUiThread(() ->
                            Toast.makeText(MyPostsActivity.this, "获取数据失败: " + response.message(), Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(MyPostsActivity.this, "获取数据失败: " + message, Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (Exception e) {
                        Log.e("Error", "Error parsing response", e);
                        runOnUiThread(() ->
                                Toast.makeText(MyPostsActivity.this, "解析响应错误", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }
        });
    }

    // 弹出确认删除对话框
    private void showDeleteConfirmationDialog(int position) {
        // 弹出确认对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("确定要删除该图文分享吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 获取选择的 Card 对象
                    Card selectedCard = cardList.get(position);

                    // 获取当前登录用户的 userId
                    SharedPreferences sharedPreferences = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
                    long userId = sharedPreferences.getLong("user_id", 0);  // 获取当前登录用户ID

                    if (userId != 0) {
                        // 调用删除方法，传入 postId 和 userId
                        deletePost(selectedCard.getId(), userId, position);
                    } else {
                        Toast.makeText(this, "用户未登录或userId无效", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    public void deletePost(long postId, long userId, int position) {
        Log.d("Delete Params", "postId: " + postId + ", userId: " + userId);

        String url = "https://api-store.openguet.cn/api/member/photo/share/delete";

        // 使用表单格式提交数据
        RequestBody requestBody = new FormBody.Builder()
                .add("shareId", String.valueOf(postId))  // 传递图文ID
                .add("userId", String.valueOf(userId))  // 传递用户ID
                .build();

        // 打印请求参数以供调试
        Log.d("Delete Request", "Request: shareId=" + postId + ", userId=" + userId);

        // 发送 POST 请求
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("appId", "63460c96c2fb45738d9cdc7deebcdde3")
                .addHeader("appSecret", "942526cc88c2a0b54411d8472919aa9ffdcfa")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("Delete Error", "Network Error: " + e.getMessage(), e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d("Delete Response", responseBody);  // 打印服务器响应

                if (!response.isSuccessful()) {
                    Log.e("Delete Failed", "Error: " + responseBody);
                } else {
                    Log.d("Delete Success", "Successfully deleted post");
                    // 删除成功后更新 UI，移除已删除的图文
                    runOnUiThread(() -> {
                        cardList.remove(position);
                        cardAdapter.notifyItemRemoved(position);  // 更新RecyclerView
                        Toast.makeText(MyPostsActivity.this, "图文删除成功", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

}