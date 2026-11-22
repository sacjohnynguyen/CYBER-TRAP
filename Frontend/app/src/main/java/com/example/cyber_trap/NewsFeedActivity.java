package com.example.cyber_trap;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class NewsFeedActivity extends AppCompatActivity {

    public static final List<Post> allPosts = new ArrayList<>();

    private RecyclerView rvPosts;
    private LinearLayout layoutEmptyState, layoutSearchFilter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText etSearchPost;
    private PostAdapter adapter;
    private final List<Post> displayedPosts = new ArrayList<>();
    private String currentTagFilter = "";
    private boolean isLoadingMore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_feed);

        rvPosts = findViewById(R.id.rvPosts);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        layoutSearchFilter = findViewById(R.id.layoutSearchFilter);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        etSearchPost = findViewById(R.id.etSearchPost);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvPosts.setLayoutManager(layoutManager);

        adapter = new PostAdapter(this, displayedPosts, new PostAdapter.OnPostActionListener() {
            @Override
            public void onPostClick(Post post) {
                int realIndex = allPosts.indexOf(post);
                Intent intent = new Intent(NewsFeedActivity.this, PostDetailActivity.class);
                intent.putExtra("POST_POSITION", realIndex);
                startActivity(intent);
            }

            @Override
            public void onEditPost(Post post, int position) {
                int realIndex = allPosts.indexOf(post);
                Intent intent = new Intent(NewsFeedActivity.this, CreatePostActivity.class);
                intent.putExtra("EDIT_POSITION", realIndex);
                startActivity(intent);
            }

            @Override
            public void onDeletePost(Post post, int position) {
                new AlertDialog.Builder(NewsFeedActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có muốn xóa bài viết này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            allPosts.remove(post);
                            ForumCacheManager.savePosts(NewsFeedActivity.this, allPosts);
                            filterPosts();
                            Toast.makeText(NewsFeedActivity.this, "Đã xóa bài viết", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
        rvPosts.setAdapter(adapter);

        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_red_dark);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                filterPosts();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "Đã làm mới bảng tin", Toast.LENGTH_SHORT).show();
            }, 600);
        });

        rvPosts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && !isLoadingMore && !displayedPosts.isEmpty()) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount) {
                        isLoadingMore = true;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> isLoadingMore = false, 1000);
                    }
                }
            }
        });

        TextView btnToggleSearch = findViewById(R.id.btnToggleSearch);
        btnToggleSearch.setOnClickListener(v -> {
            boolean isVisible = layoutSearchFilter.getVisibility() == View.VISIBLE;
            if (isVisible) {
                layoutSearchFilter.setVisibility(View.GONE);
                etSearchPost.setText("");
                currentTagFilter = "";
                filterPosts();
            } else {
                layoutSearchFilter.setVisibility(View.VISIBLE);
            }
        });

        etSearchPost.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterPosts(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.chipAll).setOnClickListener(v -> { currentTagFilter = ""; filterPosts(); });
        findViewById(R.id.chipBank).setOnClickListener(v -> { currentTagFilter = "#GiaMaoNganHang"; filterPosts(); });
        findViewById(R.id.chipJob).setOnClickListener(v -> { currentTagFilter = "#ViecNheLuongCao"; filterPosts(); });
        findViewById(R.id.chipSaved).setOnClickListener(v -> { currentTagFilter = "#SAVED#"; filterPosts(); });

        MaterialCardView cardCreatePost = findViewById(R.id.cardCreatePost);
        cardCreatePost.setOnClickListener(v -> {
            currentTagFilter = "";
            etSearchPost.setText("");
            Intent intent = new Intent(NewsFeedActivity.this, CreatePostActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, TrangChuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navSearch).setOnClickListener(v -> {
            Intent intent = new Intent(this, GiaoDienTraCuu.class);
            intent.putExtra("TAB_INDEX", 1);
            startActivity(intent);
            finish();
        });

        View navBaoCao = findViewById(R.id.navBaoCao);
        if (navBaoCao != null) {
            navBaoCao.setOnClickListener(v -> {
                Intent intent = new Intent(this, BaoCaoActivity.class);
                startActivity(intent);
            });
        }
        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        View btnNotifyContainer = findViewById(R.id.btnNotifyContainer);
        if (btnNotifyContainer != null) {
            btnNotifyContainer.setOnClickListener(v -> showNotificationBottomSheet());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (allPosts.isEmpty()) {
            List<Post> cached = ForumCacheManager.loadPosts(this);
            if (!cached.isEmpty()) {
                allPosts.addAll(cached);
            }
        }
        updateUserAvatars();
        updateNotificationBadge();
        filterPosts();
        if (!displayedPosts.isEmpty()) {
            rvPosts.scrollToPosition(0);
        }
    }

    private void updateNotificationBadge() {
        TextView tvNotifyBadge = findViewById(R.id.tvNotifyBadge);
        if (tvNotifyBadge != null) {
            int unread = NotificationManager.getUnreadCount(this);
            if (unread > 0) {
                tvNotifyBadge.setVisibility(View.VISIBLE);
                tvNotifyBadge.setText(String.valueOf(unread));
            } else {
                tvNotifyBadge.setVisibility(View.GONE);
            }
        }
    }

    private void showNotificationBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_notification_bottom_sheet, null);
        dialog.setContentView(view);

        RecyclerView rvNotifs = view.findViewById(R.id.rvNotifList);
        LinearLayout layoutEmpty = view.findViewById(R.id.layoutEmptyNotif);
        TextView btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead);
        TextView tabAll = view.findViewById(R.id.tabAllNotifs);
        TextView tabUnread = view.findViewById(R.id.tabUnreadNotifs);

        rvNotifs.setLayoutManager(new LinearLayoutManager(this));

        List<NotificationItem> allNotifs = NotificationManager.getNotifications(this);
        List<NotificationItem> displayNotifs = new ArrayList<>(allNotifs);

        NotificationAdapter notifAdapter = new NotificationAdapter(this, displayNotifs, item -> {
            NotificationManager.markAsRead(this, item.id);
            updateNotificationBadge();
            dialog.dismiss();
            if (item.postPosition >= 0 && item.postPosition < allPosts.size()) {
                Intent intent = new Intent(NewsFeedActivity.this, PostDetailActivity.class);
                intent.putExtra("POST_POSITION", item.postPosition);
                startActivity(intent);
            }
        });
        rvNotifs.setAdapter(notifAdapter);

        if (displayNotifs.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvNotifs.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvNotifs.setVisibility(View.VISIBLE);
        }

        tabAll.setOnClickListener(v1 -> {
            tabAll.setBackgroundColor(android.graphics.Color.parseColor("#E7F3FF"));
            tabAll.setTextColor(android.graphics.Color.parseColor("#1877F2"));
            tabUnread.setBackgroundColor(android.graphics.Color.parseColor("#F0F2F5"));
            tabUnread.setTextColor(android.graphics.Color.parseColor("#65676B"));
            displayNotifs.clear();
            displayNotifs.addAll(NotificationManager.getNotifications(this));
            notifAdapter.notifyDataSetChanged();
            layoutEmpty.setVisibility(displayNotifs.isEmpty() ? View.VISIBLE : View.GONE);
            rvNotifs.setVisibility(displayNotifs.isEmpty() ? View.GONE : View.VISIBLE);
        });

        tabUnread.setOnClickListener(v2 -> {
            tabUnread.setBackgroundColor(android.graphics.Color.parseColor("#E7F3FF"));
            tabUnread.setTextColor(android.graphics.Color.parseColor("#1877F2"));
            tabAll.setBackgroundColor(android.graphics.Color.parseColor("#F0F2F5"));
            tabAll.setTextColor(android.graphics.Color.parseColor("#65676B"));
            displayNotifs.clear();
            for (NotificationItem item : NotificationManager.getNotifications(this)) {
                if (!item.isRead) displayNotifs.add(item);
            }
            notifAdapter.notifyDataSetChanged();
            layoutEmpty.setVisibility(displayNotifs.isEmpty() ? View.VISIBLE : View.GONE);
            rvNotifs.setVisibility(displayNotifs.isEmpty() ? View.GONE : View.VISIBLE);
        });

        btnMarkAllRead.setOnClickListener(v3 -> {
            NotificationManager.markAllAsRead(this);
            updateNotificationBadge();
            displayNotifs.clear();
            displayNotifs.addAll(NotificationManager.getNotifications(this));
            notifAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void updateUserAvatars() {
        String loggedUser = UserUtils.getLoggedUsername(this);
        String initials = UserUtils.getInitials(loggedUser);

        TextView tvHeaderAvatarText = findViewById(R.id.tvHeaderAvatarText);
        if (tvHeaderAvatarText != null) {
            tvHeaderAvatarText.setText(initials);
        }

        TextView tvCreatePostAvatarText = findViewById(R.id.tvCreatePostAvatarText);
        if (tvCreatePostAvatarText != null) {
            tvCreatePostAvatarText.setText(initials);
        }
    }

    private void filterPosts() {
        String keyword = etSearchPost.getText().toString().trim().toLowerCase();
        displayedPosts.clear();

        for (Post post : allPosts) {
            boolean matchKeyword = keyword.isEmpty()
                    || post.title.toLowerCase().contains(keyword)
                    || post.content.toLowerCase().contains(keyword)
                    || post.tag.toLowerCase().contains(keyword);

            boolean matchTag;
            if ("#SAVED#".equals(currentTagFilter)) {
                matchTag = post.isSaved;
            } else {
                matchTag = currentTagFilter.isEmpty() || post.tag.equalsIgnoreCase(currentTagFilter);
            }

            if (matchKeyword && matchTag) {
                displayedPosts.add(post);
            }
        }

        adapter.notifyDataSetChanged();

        if (displayedPosts.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvPosts.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvPosts.setVisibility(View.VISIBLE);
        }
    }
}