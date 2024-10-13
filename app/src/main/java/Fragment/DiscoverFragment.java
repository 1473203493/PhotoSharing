package Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import entity.Card;
import Adapter.CardAdapter;
import com.example.PhotoShare.R;

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

public class DiscoverFragment extends Fragment {

    private RecyclerView recyclerView;
    private CardAdapter cardAdapter;
    private List<Card> cardList = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient();
    private SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        // 初始化 RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        cardAdapter = new CardAdapter(cardList);
        recyclerView.setAdapter(cardAdapter);

        // 初始化 SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchDiscoverItems(); // 刷新数据
            }
        });

        // 初始数据加载
        fetchDiscoverItems();

        return view;
    }

    private void fetchDiscoverItems() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        long userId = sharedPreferences.getLong("user_id", 0);  // 获取当前登录用户ID

        String url = "https://api-store.openguet.cn/api/member/photo/share?current=1&size=100&userId=" + userId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("appId", "63460c96c2fb45738d9cdc7deebcdde3")
                .addHeader("appSecret", "942526cc88c2a0b54411d8472919aa9ffdcfa")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DiscoverError", "Network Error: " + e.getMessage(), e);
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
                swipeRefreshLayout.setRefreshing(false); // 完成刷新
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.e("DiscoverError", "Unsuccessful response: " + response.code() + " - " + responseBody);
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "获取数据失败: " + response.message(), Toast.LENGTH_SHORT).show()
                    );
                    swipeRefreshLayout.setRefreshing(false); // 完成刷新
                } else {
                    String responseData = response.body().string();
                    Log.d("DiscoverSuccess", "Response: " + responseData);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        int code = jsonResponse.optInt("code", -1);
                        if (code == 200) {
                            JSONArray records = jsonResponse.getJSONObject("data").getJSONArray("records");
                            cardList.clear(); // 清空列表以防止重复添加
                            for (int i = 0; i < records.length(); i++) {
                                JSONObject record = records.getJSONObject(i);
                                String title = record.getString("title");
                                String content = record.getString("content");
                                String username = record.getString("username");
                                String avatar = record.optString("avatar", ""); // 假设有 avatarUrl 字段
                                int likeCount = record.optInt("likeNum", 0);
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
                            getActivity().runOnUiThread(() -> {
                                cardAdapter.notifyDataSetChanged(); // 更新适配器
                                swipeRefreshLayout.setRefreshing(false); // 完成刷新
                            });
                        } else {
                            String message = jsonResponse.optString("msg", "Unknown error");
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getActivity(), "获取数据失败: " + message, Toast.LENGTH_SHORT).show()
                            );
                            swipeRefreshLayout.setRefreshing(false); // 完成刷新
                        }
                    } catch (Exception e) {
                        Log.e("DiscoverError", "Error parsing response", e);
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getActivity(), "解析错误", Toast.LENGTH_SHORT).show();
                            swipeRefreshLayout.setRefreshing(false); // 完成刷新
                        });
                    }
                }
            }
        });
    }
}
