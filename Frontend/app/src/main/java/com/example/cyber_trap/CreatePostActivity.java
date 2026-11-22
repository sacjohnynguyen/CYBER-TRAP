package com.example.cyber_trap;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etPostTitle, etPostContent, etCustomTag;
    private TextView tvCharCount, tvHeaderCreateTitle, tvPrivacy;
    private MaterialButton btnSubmitPost;
    private GridLayout gridImagePreview;

    private TextView tagBank, tagJob, tagPhishing, tagLoan, tagOtp, tagOther;
    private String selectedHashtag = "#GiaMaoNganHang";
    private String selectedPrivacy = "Công khai";

    private final ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private ActivityResultLauncher<Intent> multiImagePickerLauncher;

    private int editPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        etPostTitle = findViewById(R.id.etPostTitle);
        etPostContent = findViewById(R.id.etPostContent);
        etCustomTag = findViewById(R.id.etCustomTag);
        tvCharCount = findViewById(R.id.tvCharCount);
        tvHeaderCreateTitle = findViewById(R.id.tvHeaderCreateTitle);
        tvPrivacy = findViewById(R.id.tvPrivacy);
        btnSubmitPost = findViewById(R.id.btnSubmitPost);
        gridImagePreview = findViewById(R.id.gridImagePreview);

        tagBank = findViewById(R.id.tagBank);
        tagJob = findViewById(R.id.tagJob);
        tagPhishing = findViewById(R.id.tagPhishing);
        tagLoan = findViewById(R.id.tagLoan);
        tagOtp = findViewById(R.id.tagOtp);
        tagOther = findViewById(R.id.tagOther);

        findViewById(R.id.btnBackCreate).setOnClickListener(v -> finish());

        if (tvPrivacy != null) {
            tvPrivacy.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(CreatePostActivity.this, tvPrivacy);
                popup.getMenu().add(0, 1, 0, "🌐 Công khai");
                popup.getMenu().add(0, 2, 1, "🔒 Riêng tư (Chỉ mình tôi)");
                popup.getMenu().add(0, 3, 2, "🕵️ Đăng ẩn danh");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        setPrivacyUI("Công khai");
                        return true;
                    } else if (item.getItemId() == 2) {
                        setPrivacyUI("Riêng tư");
                        return true;
                    } else if (item.getItemId() == 3) {
                        setPrivacyUI("Ẩn danh");
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        etPostContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCharCount.setText(s.length() + "/2.000");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        tagBank.setOnClickListener(v -> selectTag(tagBank, "Giả mạo ngân hàng", "#GiaMaoNganHang"));
        tagJob.setOnClickListener(v -> selectTag(tagJob, "Việc nhẹ lương cao", "#ViecNheLuongCao"));
        tagPhishing.setOnClickListener(v -> selectTag(tagPhishing, "Phishing Link", "#PhishingLink"));
        tagLoan.setOnClickListener(v -> selectTag(tagLoan, "Cho vay độc hại", "#ChoVayDocHai"));
        tagOtp.setOnClickListener(v -> selectTag(tagOtp, "Lừa đảo OTP", "#LuaDaoOTP"));
        tagOther.setOnClickListener(v -> {
            selectTag(tagOther, "Khác", "OTHER");
            etCustomTag.requestFocus();
        });

        multiImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) {
                            ClipData clipData = data.getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                if (selectedImageUris.size() < 6) {
                                    Uri uri = clipData.getItemAt(i).getUri();
                                    try {
                                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    } catch (Exception ignored) {
                                    }
                                    selectedImageUris.add(uri);
                                }
                            }
                        } else if (data.getData() != null) {
                            if (selectedImageUris.size() < 6) {
                                Uri uri = data.getData();
                                try {
                                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                } catch (Exception ignored) {
                                }
                                selectedImageUris.add(uri);
                            }
                        }
                        renderImageGrid();
                    }
                }
        );

        LinearLayout btnPickImages = findViewById(R.id.btnPickImages);
        btnPickImages.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            multiImagePickerLauncher.launch(intent);
        });

        editPosition = getIntent().getIntExtra("EDIT_POSITION", -1);
        if (editPosition >= 0 && editPosition < NewsFeedActivity.allPosts.size()) {
            Post post = NewsFeedActivity.allPosts.get(editPosition);
            tvHeaderCreateTitle.setText("Chỉnh sửa cảnh báo");
            btnSubmitPost.setText("Lưu");
            etPostTitle.setText(post.title != null ? post.title : "");
            etPostContent.setText(post.content != null ? post.content : "");
            restoreTagSelection(post.tag);
            if (post.privacy != null) {
                setPrivacyUI(post.privacy);
            }
            if (post.imageUris != null) {
                selectedImageUris.addAll(post.imageUris);
                renderImageGrid();
            }
        }

        btnSubmitPost.setOnClickListener(v -> {
            String content = etPostContent.getText().toString().trim();

            if (content.isEmpty()) {
                Toast.makeText(this, "Vui lòng mô tả chi tiết hình thức lừa đảo!", Toast.LENGTH_SHORT).show();
                return;
            }
            String title = content.length() > 50 ? content.substring(0, 50) + "..." : content;

            String finalHashtag = selectedHashtag;
            if ("OTHER".equals(selectedHashtag)) {
                String custom = etCustomTag.getText().toString().trim().replaceAll("\\s+", "");
                if (custom.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập hashtag cho loại lừa đảo!", Toast.LENGTH_SHORT).show();
                    etCustomTag.requestFocus();
                    return;
                }
                finalHashtag = custom.startsWith("#") ? custom : "#" + custom;
            }

            boolean isAnonymous = "Ẩn danh".equals(selectedPrivacy);
            String authorName = isAnonymous ? "Người dùng ẩn danh" : UserUtils.getLoggedUsername(this);
            String authorInitials = isAnonymous ? "ẨD" : UserUtils.getInitials(authorName);

            if (editPosition >= 0 && editPosition < NewsFeedActivity.allPosts.size()) {
                Post existing = NewsFeedActivity.allPosts.get(editPosition);
                existing.title = title;
                existing.content = content;
                existing.tag = finalHashtag;
                existing.privacy = selectedPrivacy;
                existing.authorName = authorName;
                existing.authorInitials = authorInitials;
                existing.imageUris = new ArrayList<>(selectedImageUris);
                ForumCacheManager.savePosts(this, NewsFeedActivity.allPosts);
                Toast.makeText(this, "Đã cập nhật bài viết!", Toast.LENGTH_SHORT).show();
            } else {
                Post newPost = new Post(
                        UUID.randomUUID().toString(),
                        authorName,
                        authorInitials,
                        "Vừa xong",
                        finalHashtag,
                        title,
                        content,
                        new ArrayList<>(selectedImageUris),
                        true
                );
                newPost.privacy = selectedPrivacy;
                NewsFeedActivity.allPosts.add(0, newPost);
                ForumCacheManager.savePosts(this, NewsFeedActivity.allPosts);
                Toast.makeText(this, "Đăng cảnh báo thành công!", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
    }

    private void restoreTagSelection(String tag) {
        if (tag == null || "#GiaMaoNganHang".equals(tag)) {
            selectTag(tagBank, "Giả mạo ngân hàng", "#GiaMaoNganHang");
        } else if ("#ViecNheLuongCao".equals(tag)) {
            selectTag(tagJob, "Việc nhẹ lương cao", "#ViecNheLuongCao");
        } else if ("#PhishingLink".equals(tag)) {
            selectTag(tagPhishing, "Phishing Link", "#PhishingLink");
        } else if ("#ChoVayDocHai".equals(tag)) {
            selectTag(tagLoan, "Cho vay độc hại", "#ChoVayDocHai");
        } else if ("#LuaDaoOTP".equals(tag)) {
            selectTag(tagOtp, "Lừa đảo OTP", "#LuaDaoOTP");
        } else {
            selectTag(tagOther, "Khác", "OTHER");
            etCustomTag.setText(tag);
        }
    }

    private void setPrivacyUI(String privacy) {
        selectedPrivacy = privacy;
        if (tvPrivacy == null) return;
        if ("Riêng tư".equals(privacy)) {
            tvPrivacy.setText("🔒 Riêng tư ▾");
        } else if ("Ẩn danh".equals(privacy)) {
            tvPrivacy.setText("🕵️ Ẩn danh ▾");
        } else {
            tvPrivacy.setText("🌐 Công khai ▾");
        }
    }

    private void selectTag(TextView activeView, String label, String hashtag) {
        selectedHashtag = hashtag;
        TextView[] allTags = {tagBank, tagJob, tagPhishing, tagLoan, tagOtp, tagOther};
        String[] labels = {"Giả mạo ngân hàng", "Việc nhẹ lương cao", "Phishing Link", "Cho vay độc hại", "Lừa đảo OTP", "Khác"};

        for (int i = 0; i < allTags.length; i++) {
            allTags[i].setBackgroundColor(Color.parseColor("#F5F5F5"));
            allTags[i].setTextColor(Color.parseColor("#424242"));
            allTags[i].setText(labels[i]);
        }

        activeView.setBackgroundColor(Color.parseColor("#FFEBEE"));
        activeView.setTextColor(Color.parseColor("#D32F2F"));
        activeView.setText("✓ " + label);

        if (etCustomTag != null) {
            etCustomTag.setVisibility("OTHER".equals(hashtag) ? View.VISIBLE : View.GONE);
        }
    }

    private void renderImageGrid() {
        gridImagePreview.removeAllViews();
        if (selectedImageUris.isEmpty()) {
            gridImagePreview.setVisibility(View.GONE);
            return;
        }
        gridImagePreview.setVisibility(View.VISIBLE);

        int sizePx = (int) (104 * getResources().getDisplayMetrics().density);
        int marginPx = (int) (6 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < selectedImageUris.size(); i++) {
            final int index = i;
            Uri uri = selectedImageUris.get(i);

            FrameLayout container = new FrameLayout(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = sizePx;
            params.height = sizePx;
            params.setMargins(marginPx, marginPx, marginPx, marginPx);
            container.setLayoutParams(params);

            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            try {
                iv.setImageURI(uri);
            } catch (Exception ignored) {
            }
            container.addView(iv);

            TextView btnRemove = new TextView(this);
            int btnSize = (int) (24 * getResources().getDisplayMetrics().density);
            FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(btnSize, btnSize);
            btnParams.gravity = Gravity.TOP | Gravity.END;
            btnParams.setMargins(4, 4, 4, 4);
            btnRemove.setLayoutParams(btnParams);
            btnRemove.setText("✕");
            btnRemove.setTextColor(Color.WHITE);
            btnRemove.setTextSize(12);
            btnRemove.setGravity(Gravity.CENTER);
            btnRemove.setBackgroundColor(Color.parseColor("#99000000"));
            btnRemove.setOnClickListener(v -> {
                selectedImageUris.remove(index);
                renderImageGrid();
            });
            container.addView(btnRemove);

            gridImagePreview.addView(container);
        }
    }
}