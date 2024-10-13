package Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.PhotoShare.CollectionActivity;
import com.example.PhotoShare.EditInfoActivity;
import com.example.PhotoShare.LikedActivity;
import com.example.PhotoShare.MyPostsActivity;
import com.example.PhotoShare.R;
import com.example.PhotoShare.SaveActivity;
import com.example.PhotoShare.SettingsActivity;
//import setting.about_us;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
//import setting.help_feedback;

public class MineFragment extends Fragment {

    private TextView usernameTextView;
    private TextView introduceTextView;
    private ImageView avatarImageView;
    private ImageView sexImageView;
    private final OkHttpClient client = new OkHttpClient();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_mine, container, false);

        // 获取显示用户信息的 TextView 和 ImageView
        usernameTextView = view.findViewById(R.id.tv_username);
        introduceTextView = view.findViewById(R.id.tv_introduce);
        avatarImageView = view.findViewById(R.id.iv_avatar);
        sexImageView = view.findViewById(R.id.iv_sex);  // 获取性别图标的 ImageView
        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        // 设置 SwipeRefreshLayout 的刷新监听器
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 当用户下拉刷新时调用 loadUserInfo()
                loadUserInfo();
                // 在数据加载完成后，调用以下方法来隐藏刷新动画
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // 设置点击事件
        RelativeLayout informationLayout = view.findViewById(R.id.information);//修改用户信息
        informationLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到 EditInfoActivity
                Intent intent = new Intent(getActivity(), EditInfoActivity.class);
                startActivity(intent);
            }
        });
        RelativeLayout settingsLayout = view.findViewById(R.id.settings);//设置
        settingsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            }
        });
       /* RelativeLayout aboutusLayout = view.findViewById(R.id.about_us);//关于我们
        aboutusLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), about_us.class);
                startActivity(intent);
            }
        });*/
        ImageView likedLayout = view.findViewById(R.id.liked);
        likedLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), LikedActivity.class);
                startActivity(intent);
            }
        });
        ImageView mypostsLayout = view.findViewById(R.id.myposts);
        mypostsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MyPostsActivity.class);
                startActivity(intent);
            }
        });
        ImageView collectLayout = view.findViewById(R.id.collection);
        collectLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CollectionActivity.class);
                startActivity(intent);
            }
        });
        ImageView saveLayout = view.findViewById(R.id.save);
        saveLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SaveActivity.class);
                startActivity(intent);
            }
        });
       /* RelativeLayout helpLayout = view.findViewById(R.id.help);
        helpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), help_feedback.class);
                startActivity(intent);
            }
        });
*/
        // 加载用户信息并显示
        loadUserInfo();
        return view;
    }

    private void loadUserInfo() {
        // 从 SharedPreferences 中获取用户信息
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        String username  = sharedPreferences.getString("username", "");  // 获取当前登录用户ID

        String url = "https://api-store.openguet.cn/api/member/photo/user/getUserByName?username=" + username;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("appId", "63460c96c2fb45738d9cdc7deebcdde3")
                .addHeader("appSecret", "942526cc88c2a0b54411d8472919aa9ffdcfa")
                .build();

        // 发起异步请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 请求失败时的处理
                Log.e("LoginError", "Network Error: " + e.getMessage(), e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // 处理非成功响应
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.e("Error", "Unsuccessful response: " + response.code() + " - " + responseBody);
                } else {
                    // 处理成功响应
                    String responseData = response.body().string();
                    Log.d("Success", "Response: " + responseData);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        int code = jsonResponse.optInt("code", -1);
                        String message = jsonResponse.optString("msg", "Unknown error");

                        if (code == 200) {
                            JSONObject data = jsonResponse.getJSONObject("data");

                            long userId = data.isNull("id") ? -1 : data.getLong("id");
                            String username = data.optString("username", "");
                            String avatarUrl = data.optString("avatar", "");
                            int sex = data.optInt("sex", 1);  // 默认性别为男
                            String introduce = data.optString("introduce", "暂无介绍");

                            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("username", username); // 替换为新的用户名
                            editor.putString("avatar", avatarUrl); // 替换为新的头像
                            editor.putInt("sex", sex); // 替换为新的性别
                            editor.putString("introduce", introduce); // 替换为新的介绍
                            editor.apply();

                            // 更新UI
                            getActivity().runOnUiThread(() -> {
                                usernameTextView.setText(username);
                                introduceTextView.setText(introduce);
                                // 设置性别图标
                                if (sex == 1) {
                                    // 男性
                                    sexImageView.setImageResource(R.drawable.baseline_male_24);  // 使用男性图标
                                } else if (sex == 2) {
                                    // 女性
                                    sexImageView.setImageResource(R.drawable.baseline_female_24);  // 使用女性图标
                                }
                                // 使用 Picasso 或 Glide 加载头像图片
                                if (!avatarUrl.isEmpty()) {
                                    Picasso.get().load(avatarUrl).into(avatarImageView);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e("Error", "Error parsing response", e);
                    }
                }
            }

        });
   }
}
