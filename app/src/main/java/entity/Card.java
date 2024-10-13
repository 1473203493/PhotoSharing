package entity;

import java.util.List;

public class Card {
    public String imageCode;
    private String title;
    private String content;
    private String username;
    private String avatar;
    private int likeCount;
    private long id; // 图文分享的主键id
    private long likeId; // 点赞表主键id
    private boolean isLiked;
    private List<String> imageUrlList;

    public Card(String title, String content, String username, String avatar, int likeCount, List<String> imageUrlList, long id, long likeId, boolean isLiked) {
        this.title = title;
        this.content = content;
        this.username = username;
        this.avatar = avatar;
        this.likeCount = likeCount;
        this.imageUrlList = imageUrlList;
        this.id = id;
        this.likeId = likeId;
        this.isLiked = isLiked;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarUrl() {
        return avatar;
    }


    public int getLikeCount() {
        return likeCount;
    }

    public List<String> getImageUrlList() {
        return imageUrlList;
    }

    public long getId() {
        return id;
    }

    public long getLikeId() {
        return likeId;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public void setLikeId(long likeId) {
        this.likeId = likeId;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        this.likeCount--;
    }
}
