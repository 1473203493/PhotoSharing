package com.example.PhotoShare;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.squareup.picasso.Picasso;

import entity.Comment;
import Adapter.CommentAdapter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import Adapter.ImagePagerAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MoreActivity extends AppCompatActivity {
    private RecyclerView recyclerViewComments;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>();
    private TextView postTitle, postContent, likeCount, collectCount,userName;
    private ImageView userAvatar, btnLike, btnCollect;
    private Button btnFocus,btnSend;
    private ImageButton btnBack;
    private ViewPager viewPager;
    private EditText commentInput;

    private final OkHttpClient client = new OkHttpClient();

    private String senduserName;
    private long shareId;   // 当前分享的ID
    private long userId;    // 当前登录用户的ID
    private long likeId;    // 当前点赞的ID
    private long collectId; // 当前收藏的ID
    private long focusUserId; // 被关注的用户ID
    private boolean hasLike = false; // 是否已经点赞
    private boolean hasCollect = false; // 是否已经收藏
    private boolean hasFocus = false; // 是否已经关注

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        // 初始化视图
        postTitle = findViewById(R.id.post_title);
        postContent = findViewById(R.id.post_content);
        likeCount = findViewById(R.id.like_count);
        collectCount = findViewById(R.id.collect_count);
        userAvatar = findViewById(R.id.user_avatar);
        userName=findViewById(R.id.user_name);
        btnLike = findViewById(R.id.btn_like);
        btnCollect = findViewById(R.id.btn_collect);
        btnFocus = findViewById(R.id.btn_focus);
        viewPager = findViewById(R.id.view_pager);
        commentInput = findViewById(R.id.comment_input);
        btnSend = findViewById(R.id.btn_send);

        // 获取Intent传递过来的shareId
        shareId = getIntent().getLongExtra("share_id", 0);

        // 获取当前用户ID
        SharedPreferences sharedPreferences = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        userId = sharedPreferences.getLong("user_id", 0);
        senduserName=sharedPreferences.getString("username","");
        // 获取图文详情
        fetchPostDetail(shareId);
        // 初始化评论区域的 RecyclerView
        recyclerViewComments = findViewById(R.id.recycler_view_comments); // 使用 detail.xml 中的 RecyclerView
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));

        // 初始化 CommentAdapter 并设置给 RecyclerView
        commentAdapter = new CommentAdapter(this, commentList,  this::loadSecondLevelComments);
        recyclerViewComments.setAdapter(commentAdapter);

        // 加载并显示评论
        fetchComments();

        commentInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    // Show the send button
                    btnSend.setVisibility(View.VISIBLE);
                } else {
                    // Hide the send button
                    btnSend.setVisibility(View.GONE);
                }
            }
        });

        // 设置点赞按钮点击事件
        btnLike.setOnClickListener(v -> {
            if (hasLike) {
                cancelLike(likeId);
            } else {
                likePost(shareId);
            }
        });

        // 设置收藏按钮点击事件
        btnCollect.setOnClickListener(v -> {
            if (hasCollect) {
                cancelCollect(collectId);
            } else {
                collectPost(shareId);
            }
        });

        // 设置关注按钮点击事件
        btnFocus.setOnClickListener(v -> {
            if (hasFocus) {
                cancelFocus(focusUserId);
            } else {
                focusUser(focusUserId);
            }
        });

        btnBack = findViewById(R.id.btn_back);
        // 设置返回按钮点击事件
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        btnSend.setOnClickListener(v -> {
            String commentContent = commentInput.getText().toString().trim();
            if (!commentContent.isEmpty()) {
                sendComment(commentContent);
            } else {
                Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchComments() {
        String url = "https://api-store.openguet.cn/api/member/photo/comment/first?shareId=" + shareId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("appId", "750baf37e04f432e8425dfa10a21b4e9")
                .addHeader("appSecret", "8012134351201e3b043429ff5761d21e1eafd")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("CommentFetchError", "Failed to fetch comments: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONArray commentsArray = jsonResponse.getJSONObject("data").getJSONArray("records");
                        // 清空评论列表以防止重复
                        commentList.clear();
                        for (int i = 0; i < commentsArray.length(); i++) {
                            JSONObject commentObject = commentsArray.getJSONObject(i);
                            long id = commentObject.getLong("id");
                            String content = commentObject.getString("content");
                            String createTime = commentObject.getString("createTime");
                            String userName = commentObject.getString("userName");
                            // 创建一级评论对象
                            Comment comment = new Comment(id, content, createTime, userName);
                            // 加载二级评论
                            loadSecondLevelComments(comment.getId());
                            // 添加到评论列表
                            commentList.add(comment);
                        }
                        // 刷新适配器
                        runOnUiThread(() -> commentAdapter.notifyDataSetChanged());
                    } catch (Exception e) {
                        Log.e("CommentParseError", "Failed to parse comments: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void loadSecondLevelComments(long commentId) {
        String url = "https://api-store.openguet.cn/api/member/photo/comment/second?shareId=" + shareId + "&commentId=" + commentId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("appId", "750baf37e04f432e8425dfa10a21b4e9")
                .addHeader("appSecret", "8012134351201e3b043429ff5761d21e1eafd")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SecondLevelFetchError", "Failed to fetch second-level comments: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONArray repliesArray = jsonResponse.getJSONObject("data").getJSONArray("records");

                        // 二级评论列表
                        List<Comment> replies = new ArrayList<>();

                        for (int i = 0; i < repliesArray.length(); i++) {
                            JSONObject replyObject = repliesArray.getJSONObject(i);
                            long id = replyObject.getLong("id");
                            String content = replyObject.getString("content");
                            String createTime = replyObject.getString("createTime");
                            String userName = replyObject.getString("userName");

                            // 创建二级评论对象
                            Comment reply = new Comment(id, content, createTime, userName);
                            replies.add(reply);
                        }
                        // 查找相应的一级评论对象并设置二级评论
                        for (Comment comment : commentList) {
                            if (comment.getId() == commentId) {
                                comment.setReplies(replies);
                                break;
                            }
                        }

                        // 刷新适配器
                        runOnUiThread(() -> commentAdapter.notifyDataSetChanged());

                    } catch (Exception e) {
                        Log.e("ReplyParseError", "Failed to parse second-level comments: " + e.getMessage());
                    }
                }
            }
        });
    }



    private void sendComment(String content) {
        Log.d("PostComment", "shareId: " + shareId + ", userId: " + userId +"  username:"+senduserName+ ", content: " + content);
        JSONObject CommentData = new JSONObject();
        try {
            CommentData.put("content", content);
            CommentData.put("shareId", String.valueOf(shareId));
            CommentData.put("userId", String.valueOf(userId));
            CommentData.put("userName",String.valueOf(senduserName) );
        } catch (JSONException e) {
            Log.e("CommentDataError", e.getMessage(), e);
        }

        RequestBody requestBody = RequestBody.create(CommentData.toString(), MediaType.parse("application/json"));

        // 创建请求
        Request request = new Request.Builder()
                .url("https://api-store.openguet.cn/api/member/photo/comment/first")
                .addHeader("appId", "750baf37e04f432e8425dfa10a21b4e9")
                .addHeader("appSecret", "8012134351201e3b043429ff5761d21e1eafd")
                .post(requestBody)
                .build();

        // 发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("CommentPostError", "Failed to send comment: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d("CommentPostResponse", "Response: " + responseData);

                    runOnUiThread(() -> {
                        Toast.makeText(MoreActivity.this, "评论发送成功", Toast.LENGTH_SHORT).show();
                        commentInput.setText("");
                        fetchComments();  // 刷新评论列表
                    });
                } else {
                    String responseBody = response.body().string();
                    Log.e("CommentPostError", "Error response: " + responseBody);
                    runOnUiThread(() ->
                            Toast.makeText(MoreActivity.this, "评论发送失败", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }




    private void fetchPostDetail(long shareId) {
        String url = "https://api-store.openguet.cn/api/member/photo/share/detail?shareId=" + shareId + "&userId=" + userId;

        executeGetRequest(url, new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("PostDetailError", "Network Error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(MoreActivity.this, "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.e("PostDetailError", "Unsuccessful response: " + response.code() + " - " + responseBody);
                    runOnUiThread(() ->
                            Toast.makeText(MoreActivity.this, "获取数据失败: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                } else {
                    String responseData = response.body().string();
                    Log.d("PostDetailSuccess", "Response: " + responseData); // 打印 API 响应
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        int code = jsonResponse.optInt("code", -1);
                        if (code == 200) {
                            JSONObject data = jsonResponse.getJSONObject("data");

                            // 更新UI
                            runOnUiThread(() -> {
                                postTitle.setText(data.optString("title", ""));
                                postContent.setText(data.optString("content", ""));
                                likeCount.setText(String.valueOf(data.optInt("likeNum", 0)));
                                collectCount.setText(String.valueOf(data.optInt("collectNum", 0)));
                                userName.setText(data.optString("username",""));
                                // 设置点赞状态
                                likeId = data.optLong("likeId", -1);
                                hasLike = likeId != -1;
                                updateLikeButton();

                                // 设置收藏状态
                                collectId = data.optLong("collectId", -1);
                                hasCollect = collectId != -1;
                                updateCollectButton();

                                // 设置关注状态
                                hasFocus = data.optBoolean("hasFocus", false);
                                focusUserId = data.optLong("pUserId", 0);
                                updateFocusButton();

                                // 加载用户头像
                                String avatarUrl = data.optString("avatar", "");
                                if (!avatarUrl.isEmpty()) {
                                    Picasso.get().load(avatarUrl).into(userAvatar);
                                }

                                // 加载图片列表
                                JSONArray imageUrlList = data.optJSONArray("imageUrlList");
                                if (imageUrlList != null) {
                                    List<String> images = new ArrayList<>();
                                    try {
                                        for (int i = 0; i < imageUrlList.length(); i++) {
                                            images.add(imageUrlList.getString(i));
                                        }
                                    } catch (JSONException e) {
                                        Log.e("PostDetailError", "Error parsing imageUrlList", e);
                                        runOnUiThread(() ->
                                                Toast.makeText(MoreActivity.this, "解析图片列表时出错", Toast.LENGTH_SHORT).show()
                                        );
                                    }

                                    // 设置 ViewPager 的 Adapter 来展示图片
                                    ImagePagerAdapter adapter = new ImagePagerAdapter(MoreActivity.this, images);
                                    viewPager.setAdapter(adapter);
                                }
                            });
                        } else {
                            String message = jsonResponse.optString("msg", "Unknown error");
                            runOnUiThread(() ->
                                    Toast.makeText(MoreActivity.this, "获取数据失败: " + message, Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (Exception e) {
                        Log.e("PostDetailError", "Error parsing response", e);
                        runOnUiThread(() ->
                                Toast.makeText(MoreActivity.this, "解析响应错误", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }
        });
    }

    private void likePost(long shareId) {
        String url = "https://api-store.openguet.cn/api/member/photo/like";
        RequestBody requestBody = new FormBody.Builder()
                .add("shareId", String.valueOf(shareId))
                .add("userId", String.valueOf(userId))
                .build();

        executePostRequest(url, requestBody, new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("LikeError", "Network Error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(MoreActivity.this, "点赞失败: 网络错误", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        // Update local like status
                        hasLike = true;
                        updateLikeButton();
                        Toast.makeText(MoreActivity.this, "点赞成功", Toast.LENGTH_SHORT).show();
                        // Re-fetch post details
                        fetchPostDetail(shareId);
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MoreActivity.this, "点赞失败: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }


    private void cancelLike(long likeId) {
        String url = "https://api-store.openguet.cn/api/member/photo/like/cancel";
        RequestBody requestBody = new FormBody.Builder()
                .add("likeId", String.valueOf(likeId))
                .build();

        executePostRequest(url, requestBody, new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("CancelLikeError", "Network Error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(MoreActivity.this, "取消点赞失败: 网络错误", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    hasLike = false;
                    runOnUiThread(() -> {
                        updateLikeButton();
                        Toast.makeText(MoreActivity.this, "取消点赞成功", Toast.LENGTH_SHORT).show();
                        fetchPostDetail(shareId);
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MoreActivity.this, "取消点赞失败: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void collectPost(long shareId) {
        String url = "https://api-store.openguet.cn/api/member/photo/collect";
        RequestBody requestBody = new FormBody.Builder()
                .add("shareId", String.valueOf(shareId))
                .add("userId", String.valueOf(userId))
                .build();

        executePostRequest(url, requestBody, new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("CollectError", "Network Error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(MoreActivity.this, "收藏失败: 网络错误", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    hasCollect = true;
                    runOnUiThread(() -> {
                        updateCollectButton();
                        Toast.makeText(MoreActivity.this, "收藏成功", Toast.LENGTH_SHORT).show();
                        fetchPostDetail(shareId);
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MoreActivity.this, "收藏失败: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void cancelCollect(long collectId) {
        String url = "https://api-store.openguet.cn/api/member/photo/collect/cancel";
        RequestBody requestBody = new FormBody.Builder()
                .add("collectId", String.valueOf(collectId))
                .build();

        executePostRequest(url, requestBody, new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("CancelCollectError", "Network Error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(MoreActivity.this, "取消收藏失败: 网络错误", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    hasCollect = false;
                    runOnUiThread(() -> {
                        updateCollectButton();
                        Toast.makeText(MoreActivity.this, "取消收藏成功", Toast.LENGTH_SHORT).show();
                        fetchPostDetail(shareId);
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MoreActivity.this, "取消收藏失败: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void focusUser(long focusUserId) {
        String url = "https://api-store.openguet.cn/api/member/photo/focus";
        RequestBody requestBody = new FormBody.Builder()
                .add("focusUserId", String.valueOf(focusUserId))
                .add("userId", String.valueOf(userId))
                .build();

        executePostRequest(url, requestBody, new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("FocusError", "Network Error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(MoreActivity.this, "关注失败: 网络错误", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    hasFocus = true;
                    runOnUiThread(() -> {
                        updateFocusButton();
                        Toast.makeText(MoreActivity.this, "关注成功", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MoreActivity.this, "关注失败: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void cancelFocus(long focusUserId) {
        String url = "https://api-store.openguet.cn/api/member/photo/focus/cancel";
        RequestBody requestBody = new FormBody.Builder()
                .add("focusUserId", String.valueOf(focusUserId))
                .add("userId", String.valueOf(userId))
                .build();

        executePostRequest(url, requestBody, new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("CancelFocusError", "Network Error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(MoreActivity.this, "取消关注失败: 网络错误", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    hasFocus = false;
                    runOnUiThread(() -> {
                        updateFocusButton();
                        Toast.makeText(MoreActivity.this, "取消关注成功", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MoreActivity.this, "取消关注失败: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void updateLikeButton() {
        if (hasLike) {
            btnLike.setImageResource(R.drawable.loved);// 已点赞的图标
        } else {
            btnLike.setImageResource(R.drawable.love); // 未点赞的图标
        }
    }

    private void updateCollectButton() {
        if (hasCollect) {
            btnCollect.setImageResource(R.drawable.collection); // 已收藏的图标
        } else {
            btnCollect.setImageResource(R.drawable.no_collection); // 未收藏的图标
        }
    }

    private void updateFocusButton() {
        if (hasFocus) {
            btnFocus.setText("已关注"); // 显示已关注状态
        } else {
            btnFocus.setText("关注"); // 显示关注状态
        }
    }

    private void executePostRequest(String url, RequestBody requestBody, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("appId", "750baf37e04f432e8425dfa10a21b4e9")
                .addHeader("appSecret", "8012134351201e3b043429ff5761d21e1eafd")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(callback);
    }

    private void executeGetRequest(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("appId", "750baf37e04f432e8425dfa10a21b4e9")
                .addHeader("appSecret", "8012134351201e3b043429ff5761d21e1eafd")
                .build();

        client.newCall(request).enqueue(callback);
    }
}
