package Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.PhotoShare.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import entity.Comment;
import tool.ReplyAdapter;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private Context context;

   // private OnReplyClickListener onReplyClickListener;

    public CommentAdapter(Context context, List<Comment> commentList,
                          OnReplyClickListener onReplyClickListener) {
        this.context = context;
        this.commentList = commentList;
      //  this.onReplyClickListener = onReplyClickListener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.commentUserName.setText(comment.getUserName());
        holder.commentContent.setText(comment.getContent());
        holder.commentTime.setText(comment.getCreateTime());

        // 加载用户头像（假设有头像的 URL 字段）
        String avatarUrl = "https://tse4-mm.cn.bing.net/th/id/OIP-C.X-VG5gTN2ak8rGRij3oCogAAAA?w=165&h=180&c=7&r=0&o=5&dpr=1.3&pid=1.7";  // 从 comment 中获取实际的头像 URL
        Picasso.get().load(avatarUrl).into(holder.commentUserAvatar);


        // 设置回复区域的 RecyclerView
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            holder.replyRecyclerView.setVisibility(View.VISIBLE);
            ReplyAdapter replyAdapter = new ReplyAdapter(context, comment.getReplies());
            holder.replyRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            holder.replyRecyclerView.setAdapter(replyAdapter);
        } else {
            holder.replyRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView commentUserAvatar, btnLike;
        TextView commentUserName, commentContent, commentTime, likeCount;
        RecyclerView replyRecyclerView;  // 用于展示二级评论

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            commentUserAvatar = itemView.findViewById(R.id.Cuser_avatar);
            commentUserName = itemView.findViewById(R.id.Cusername);
            commentContent = itemView.findViewById(R.id.comment_content);
            commentTime = itemView.findViewById(R.id.comment_time);
            btnLike = itemView.findViewById(R.id.btn_like);
            likeCount = itemView.findViewById(R.id.like_count);
            replyRecyclerView = itemView.findViewById(R.id.recycler_view_replies);  // 嵌套的 RecyclerView 用于展示二级评论
        }
    }



    public interface OnReplyClickListener {
        void onReply(long commentId);
    }
}
