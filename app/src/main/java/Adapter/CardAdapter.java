package Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.PhotoShare.MoreActivity;
import com.example.PhotoShare.R;
import com.squareup.picasso.Picasso;

import entity.Card;
import okhttp3.*;

import java.io.IOException;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private List<Card> cardList;
    private final OkHttpClient client = new OkHttpClient();

    public CardAdapter(List<Card> cardList) {
        this.cardList = cardList;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cardList.get(position);
        holder.titleTextView.setText(card.getTitle());
        holder.usernameTextView.setText(card.getUsername());
        holder.likeCountTextView.setText(String.valueOf(card.getLikeCount()));

        // 加载图片
        if (!card.getImageUrlList().isEmpty()) {
            Picasso.get().load(card.getImageUrlList().get(0)).into(holder.imageView);
        }

        // 加载用户头像
        if (!card.getAvatarUrl().isEmpty()) {
            Picasso.get().load(card.getAvatarUrl()).into(holder.avatarImageView);
        }

        // 根据初始状态设置点赞图标
        updateLikeIcon(holder.likeIcon, card.isLiked());

        // 点赞按钮点击事件
        holder.likeIcon.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = holder.itemView.getContext().getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
            long userId = sharedPreferences.getLong("user_id", 0);

            if (card.isLiked()) {
                // 取消点赞
                cancelLike(card.getLikeId(), holder, card, userId, position);
            } else {
                // 点赞
                like(card.getId(), holder, card, userId, position);
            }
        });
        // 图片点击事件
        holder.imageView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), MoreActivity.class);
            intent.putExtra("share_id", card.getId());  // 传递分享的主键ID
            holder.itemView.getContext().startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return cardList.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView usernameTextView;
        TextView likeCountTextView;
        ImageView imageView;
        ImageView avatarImageView;
        ImageView likeIcon;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.card_title_text_view);
            usernameTextView = itemView.findViewById(R.id.card_username_text_view);
            likeCountTextView = itemView.findViewById(R.id.card_like_count_text_view);
            imageView = itemView.findViewById(R.id.card_image_view);
            avatarImageView = itemView.findViewById(R.id.card_avatar_image_view);
            likeIcon = itemView.findViewById(R.id.card_like_icon);
        }
    }

    private void like(long shareId, CardViewHolder holder, Card card, long userId, int position) {
        RequestBody formBody = new FormBody.Builder()
                .add("shareId", String.valueOf(shareId))
                .add("userId", String.valueOf(userId))
                .build();

        Request request = new Request.Builder()
                .url("https://api-store.openguet.cn/api/member/photo/like")
                .addHeader("appId", "750baf37e04f432e8425dfa10a21b4e9")
                .addHeader("appSecret", "8012134351201e3b043429ff5761d21e1eafd")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 请求失败处理
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    card.setLiked(true);
                    card.increateLikeCount();

                    // 更新UI
                    holder.itemView.post(() -> {
                        updateLikeIcon(holder.likeIcon, true);
                        holder.likeCountTextView.setText(String.valueOf(card.getLikeCount()));
                        notifyItemChanged(position);  // 更新特定项的UI
                    });
                }
            }
        });
    }

    private void cancelLike(long likeId, CardViewHolder holder, Card card, long userId, int position) {
        RequestBody formBody = new FormBody.Builder()
                .add("likeId", String.valueOf(likeId))
                .build();

        Request request = new Request.Builder()
                .url("https://api-store.openguet.cn/api/member/photo/like/cancel")
                .addHeader("appId", "750baf37e04f432e8425dfa10a21b4e9")
                .addHeader("appSecret", "8012134351201e3b043429ff5761d21e1eafd")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 请求失败处理
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    card.setLiked(false);
                    card.decrementLikeCount();

                    // 更新UI
                    holder.itemView.post(() -> {
                        updateLikeIcon(holder.likeIcon, false);
                        holder.likeCountTextView.setText(String.valueOf(card.getLikeCount()));
                        notifyItemChanged(position);  // 更新特定项的UI
                    });
                }
            }
        });
    }

    private void updateLikeIcon(ImageView likeIcon, boolean isLiked) {
        if (isLiked) {
            likeIcon.setImageResource(R.drawable.loved); // 已点赞图标
        } else {
            likeIcon.setImageResource(R.drawable.love); // 未点赞图标
        }
    }
}