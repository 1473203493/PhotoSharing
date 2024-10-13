package entity;

import java.util.ArrayList;
import java.util.List;

public class Comment {
    private long id;
    private String content;
    private String createTime;
    private String userName;
    private List<Comment> replies; // 二级评论列表

    // 构造函数
    public Comment(long id, String content, String createTime, String userName) {
        this.id = id;
        this.content = content;
        this.createTime = createTime;
        this.userName = userName;
        this.replies = new ArrayList<>(); // 初始化回复列表
    }
    public long getId() {
        return id;
    }

    // 获取评论内容
    public String getContent() {
        return content;
    }

    // 获取评论时间
    public String getCreateTime() {
        return createTime;
    }

    // 获取用户名
    public String getUserName() {
        return userName;
    }


    // 其他 getter 和 setter 方法
    public List<Comment> getReplies() {
        return replies;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }
}

