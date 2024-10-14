package Adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.PhotoShare.DetailActivity;
import com.example.PhotoShare.R;
import com.squareup.picasso.Picasso;

import entity.Card;
import okhttp3.*;

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


        // 加载图片
        if (!card.getImageUrlList().isEmpty()) {
            Picasso.get().load(card.getImageUrlList().get(0)).into(holder.imageView);
        }

        // 加载用户头像
        if (!card.getAvatarUrl().isEmpty()) {
            Picasso.get().load(card.getAvatarUrl()).into(holder.avatarImageView);
        }


        // 图片点击事件
        holder.imageView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), DetailActivity.class);
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
        ImageView imageView;
        ImageView avatarImageView;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.card_title_text_view);
            usernameTextView = itemView.findViewById(R.id.card_username_text_view);
            imageView = itemView.findViewById(R.id.card_image_view);
            avatarImageView = itemView.findViewById(R.id.card_avatar_image_view);

        }
    }
}


