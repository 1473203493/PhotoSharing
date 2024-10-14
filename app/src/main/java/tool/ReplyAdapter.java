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
        String avatarUrl = "https://tse4-mm.cn.bing.net/th/id/OIP-C.X-VG5gTN2ak8rGRij3oCogAAAA?w=165&h=180&c=7&r=0&o=5&dpr=1.3&pid=1.7";  // 你可能需要从 reply 中获取实际的头像 URL
        if (!avatarUrl.isEmpty()) {
            Picasso.get().load(avatarUrl).into(holder.replyUserAvatar);
        }
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    public static class ReplyViewHolder extends RecyclerView.ViewHolder {
        ImageView replyUserAvatar;
        TextView replyUserName, replyContent, replyTime;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            replyUserAvatar = itemView.findViewById(R.id.reply_user_avatar);
            replyUserName = itemView.findViewById(R.id.reply_user_name);
            replyContent = itemView.findViewById(R.id.reply_content);
            replyTime = itemView.findViewById(R.id.reply_time);

        }
    }
}
