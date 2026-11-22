package com.example.cyber_trap;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.UUID;

public class PostDetailActivity extends AppCompatActivity {

    private Post currentPost;
    private CommentAdapter commentAdapter;
    private TextView tvDetailCommentCount, tvEmptyComments, tvReplyingTo;
    private LinearLayout layoutReplyingBanner;
    private EditText etCommentInput;

    private Comment replyingTarget = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        int postPosition = getIntent().getIntExtra("POST_POSITION", -1);
        if (postPosition < 0 || postPosition >= NewsFeedActivity.allPosts.size()) {
            finish();
            return;
        }
        currentPost = NewsFeedActivity.allPosts.get(postPosition);
        if (currentPost.comments == null) {
            currentPost.comments = new ArrayList<>();
        }

        findViewById(R.id.btnCloseDetail).setOnClickListener(v -> finish());

        TextView tvDetailAvatar = findViewById(R.id.tvDetailAvatar);
        TextView tvDetailAuthor = findViewById(R.id.tvDetailAuthor);
        TextView tvDetailTime = findViewById(R.id.tvDetailTime);
        TextView tvDetailTag = findViewById(R.id.tvDetailTag);
        TextView tvDetailTitle = findViewById(R.id.tvDetailTitle);
        TextView tvDetailContent = findViewById(R.id.tvDetailContent);
        GridLayout gridDetailImages = findViewById(R.id.gridDetailImages);

        tvDetailCommentCount = findViewById(R.id.tvDetailCommentCount);
        tvEmptyComments = findViewById(R.id.tvEmptyComments);
        layoutReplyingBanner = findViewById(R.id.layoutReplyingBanner);
        tvReplyingTo = findViewById(R.id.tvReplyingTo);
        etCommentInput = findViewById(R.id.etCommentInput);

        String privacyText = (currentPost.privacy != null && !currentPost.privacy.isEmpty()) ? currentPost.privacy : "Công khai";
        tvDetailAvatar.setText(currentPost.authorInitials);
        tvDetailAuthor.setText(currentPost.authorName);
        tvDetailTime.setText(currentPost.timeAgo + " · " + privacyText);
        tvDetailTag.setText(currentPost.tag);
        tvDetailTitle.setText(currentPost.title);
        tvDetailContent.setText(currentPost.content);

        if (currentPost.imageUris != null && !currentPost.imageUris.isEmpty()) {
            gridDetailImages.setVisibility(View.VISIBLE);
            gridDetailImages.removeAllViews();

            int heightPx = (int) (170 * getResources().getDisplayMetrics().density);
            int marginPx = (int) (4 * getResources().getDisplayMetrics().density);

            for (Uri uri : currentPost.imageUris) {
                try {
                    ImageView iv = new ImageView(this);
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = 0;
                    params.height = heightPx;
                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                    params.setMargins(marginPx, marginPx, marginPx, marginPx);
                    iv.setLayoutParams(params);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setImageURI(uri);
                    gridDetailImages.addView(iv);
                } catch (Exception ignored) {
                }
            }
        }

        RecyclerView rvComments = findViewById(R.id.rvComments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this, currentPost.comments, comment -> {
            replyingTarget = comment;
            layoutReplyingBanner.setVisibility(View.VISIBLE);
            tvReplyingTo.setText("Đang trả lời " + comment.authorName);
            etCommentInput.requestFocus();
        });
        rvComments.setAdapter(commentAdapter);

        findViewById(R.id.btnCancelReply).setOnClickListener(v -> clearReplyTarget());

        findViewById(R.id.btnSendComment).setOnClickListener(v -> {
            String text = etCommentInput.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập bình luận", Toast.LENGTH_SHORT).show();
                return;
            }

            String loggedUser = UserUtils.getLoggedUsername(this);
            String loggedInitials = UserUtils.getInitials(loggedUser);

            if (replyingTarget != null) {
                String parentId = replyingTarget.isReply ? replyingTarget.parentId : replyingTarget.id;
                Comment reply = new Comment(
                        UUID.randomUUID().toString(),
                        parentId,
                        loggedUser,
                        loggedInitials,
                        "Vừa xong",
                        "@" + replyingTarget.authorName + " " + text,
                        true
                );
                int insertIndex = findInsertIndexForReply(parentId);
                currentPost.comments.add(insertIndex, reply);
                clearReplyTarget();
            } else {
                Comment newComment = new Comment(
                        UUID.randomUUID().toString(),
                        null,
                        loggedUser,
                        loggedInitials,
                        "Vừa xong",
                        text,
                        false
                );
                currentPost.comments.add(newComment);
            }

            currentPost.commentCount = currentPost.comments.size();

            // Thêm thông báo tương tác mới khi có bình luận
            NotificationItem notif = new NotificationItem(
                    UUID.randomUUID().toString(),
                    loggedUser,
                    loggedInitials,
                    "COMMENT",
                    currentPost.title,
                    postPosition,
                    "Vừa xong",
                    false
            );
            NotificationManager.addNotification(this, notif);

            etCommentInput.setText("");
            commentAdapter.notifyDataSetChanged();
            updateCommentHeader();
        });

        updateCommentHeader();
    }

    private int findInsertIndexForReply(String parentId) {
        int lastIndex = currentPost.comments.size();
        boolean foundParent = false;
        for (int i = 0; i < currentPost.comments.size(); i++) {
            Comment c = currentPost.comments.get(i);
            if (parentId.equals(c.id) || parentId.equals(c.parentId)) {
                foundParent = true;
                lastIndex = i + 1;
            } else if (foundParent) {
                break;
            }
        }
        return lastIndex;
    }

    private void clearReplyTarget() {
        replyingTarget = null;
        layoutReplyingBanner.setVisibility(View.GONE);
    }

    private void updateCommentHeader() {
        int count = currentPost.comments.size();
        tvDetailCommentCount.setText(count + " bình luận");
        tvEmptyComments.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }
}