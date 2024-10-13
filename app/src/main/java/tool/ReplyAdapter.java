package tool;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.PhotoShare.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import entity.Comment;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {

    private List<Comment> replyList;  // 回复列表
    private Context context;

    public ReplyAdapter(Context context, List<Comment> replyList) {
        this.context = context;
        this.replyList = replyList;
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reply, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Comment reply = replyList.get(position);

        holder.replyUserName.setText(reply.getUserName());
        holder.replyContent.setText(reply.getContent());
        holder.replyTime.setText(reply.getCreateTime());

        // 加载用户头像（假设你有头像的 URL 字段）
        String avatarUrl = "https://guet-lab.oss-cn-hangzhou.aliyuncs.com/api/2023/12/14/b15f3e06-4175-494a-9a81-8ed0b47ae87b.png";  // 你可能需要从 reply 中获取实际的头像 URL
        if (!avatarUrl.isEmpty()) {
            Picasso.get().load(avatarUrl).into(holder.replyUserAvatar);
        }
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    public static class ReplyViewHolder extends RecyclerView.ViewHolder {
        ImageView replyUserAvatar, replyBtnLike;
        TextView replyUserName, replyContent, replyTime, replyLikeCount;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            replyUserAvatar = itemView.findViewById(R.id.reply_user_avatar);
            replyUserName = itemView.findViewById(R.id.reply_user_name);
            replyContent = itemView.findViewById(R.id.reply_content);
            replyTime = itemView.findViewById(R.id.reply_time);
            replyBtnLike = itemView.findViewById(R.id.reply_btn_like);
            replyLikeCount = itemView.findViewById(R.id.reply_like_count);
        }
    }
}
