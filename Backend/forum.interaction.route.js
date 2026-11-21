const express = require("express");
const jwt = require("jsonwebtoken");
const multer = require("multer");
const fs = require("fs");
const path = require("path");
const { randomUUID } = require("crypto");
const { pool } = require("./db");

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || "cyber_trap_jwt_secret_key_2026";
const MAX_COMMENT_LENGTH = 2000;
const MAX_POST_TITLE_LENGTH = 255;
const MAX_POST_CONTENT_LENGTH = 100000;
const MAX_POST_IMAGES = 6;
const MAX_POST_IMAGE_SIZE = 5 * 1024 * 1024;
const POST_IMAGE_DIRECTORY = path.join(__dirname, "uploads", "posts");

const postImageUpload = multer({
    storage: multer.memoryStorage(),
    limits: {
        fileSize: MAX_POST_IMAGE_SIZE,
        files: MAX_POST_IMAGES,
        fields: 4
    },
    fileFilter(req, file, callback) {
        const allowedTypes = ["image/jpeg", "image/png", "image/gif", "image/webp"];
        if (!allowedTypes.includes(file.mimetype)) {
            return callback(new Error("Chỉ chấp nhận ảnh JPEG, PNG, GIF hoặc WebP."));
        }
        return callback(null, true);
    }
}).array("images", MAX_POST_IMAGES);

function parsePositiveId(value) {
    if (typeof value !== "string" || !/^[1-9]\d*$/.test(value)) {
        return null;
    }

    const id = Number(value);
    return Number.isSafeInteger(id) ? id : null;
}

function getAuthenticatedUserId(req) {
    const authorization = req.get("authorization");
    const match = authorization && authorization.match(/^Bearer\s+(.+)$/i);
    if (!match) {
        return null;
    }

    try {
        const payload = jwt.verify(match[1], JWT_SECRET);
        return Number.isSafeInteger(payload.id) && payload.id > 0
            ? payload.id
            : null;
    } catch (error) {
        return null;
    }
}

async function rollback(connection, transactionStarted) {
    if (connection && transactionStarted) {
        try {
            await connection.rollback();
        } catch (error) {
            console.error("Lỗi rollback tương tác diễn đàn:", error);
        }
    }
}

function getImageFormat(buffer) {
    if (
        buffer.length >= 8 &&
        buffer.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))
    ) {
        return { mimeType: "image/png", extension: "png" };
    }
    if (buffer.length >= 3 && buffer[0] === 0xff && buffer[1] === 0xd8 && buffer[2] === 0xff) {
        return { mimeType: "image/jpeg", extension: "jpg" };
    }
    if (
        buffer.length >= 6 &&
        ["GIF87a", "GIF89a"].includes(buffer.toString("ascii", 0, 6))
    ) {
        return { mimeType: "image/gif", extension: "gif" };
    }
    if (
        buffer.length >= 12 &&
        buffer.toString("ascii", 0, 4) === "RIFF" &&
        buffer.toString("ascii", 8, 12) === "WEBP"
    ) {
        return { mimeType: "image/webp", extension: "webp" };
    }
    return null;
}

function parsePostImages(req, res, next) {
    postImageUpload(req, res, (error) => {
        if (!error) {
            return next();
        }
        if (error instanceof multer.MulterError) {
            return res.status(error.code === "LIMIT_FILE_SIZE" ? 413 : 400).json({
                success: false,
                message: error.code === "LIMIT_FILE_SIZE"
                    ? "Mỗi ảnh tải lên tối đa 5 MB."
                    : "Dữ liệu ảnh tải lên không hợp lệ hoặc vượt quá giới hạn."
            });
        }
        return res.status(400).json({
            success: false,
            message: error.message || "Không thể xử lý ảnh tải lên."
        });
    });
}

async function removePostImageFiles(imagePaths, context) {
    const results = await Promise.allSettled(
        imagePaths.map((imagePath) => fs.promises.unlink(imagePath))
    );
    for (const result of results) {
        if (result.status === "rejected") {
            console.error(context, result.reason);
        }
    }
}

function getManagedPostImagePath(mediaUrl) {
    const prefix = "/uploads/posts/";
    if (typeof mediaUrl !== "string" || !mediaUrl.startsWith(prefix)) {
        return null;
    }

    const filename = mediaUrl.slice(prefix.length);
    if (!filename || filename !== path.basename(filename)) {
        return null;
    }
    const imagePath = path.resolve(POST_IMAGE_DIRECTORY, filename);
    if (path.dirname(imagePath) !== POST_IMAGE_DIRECTORY) {
        return null;
    }
    return imagePath;
}

router.get("/", async (req, res) => {
    const page = req.query.page === undefined ? 1 : Number(req.query.page);
    const limit = req.query.limit === undefined ? 10 : Number(req.query.limit);
    if (
        !Number.isSafeInteger(page) || page < 1 ||
        !Number.isSafeInteger(limit) || limit < 1 || limit > 50
    ) {
        return res.status(400).json({
            success: false,
            message: "page phải là số nguyên dương và limit phải từ 1 đến 50."
        });
    }
    const offset = (page - 1) * limit;
    if (!Number.isSafeInteger(offset)) {
        return res.status(400).json({
            success: false,
            message: "page vượt quá giới hạn hỗ trợ."
        });
    }

    let categoryId = null;
    if (req.query.categoryId !== undefined) {
        categoryId = parsePositiveId(String(req.query.categoryId));
        if (categoryId === null) {
            return res.status(400).json({
                success: false,
                message: "categoryId không hợp lệ."
            });
        }
    }
    const keyword = typeof req.query.keyword === "string"
        ? req.query.keyword.trim()
        : "";
    if (keyword.length > 255) {
        return res.status(400).json({
            success: false,
            message: "Từ khóa tìm kiếm tối đa 255 ký tự."
        });
    }

    const conditions = [];
    const values = [];
    if (categoryId !== null) {
        conditions.push("p.CategoryId = ?");
        values.push(categoryId);
    }
    if (keyword) {
        const escapedKeyword = keyword.replace(/[\\%_]/g, "\\$&");
        conditions.push("(p.Title LIKE ? ESCAPE '\\\\' OR p.Content LIKE ? ESCAPE '\\\\')");
        values.push(`%${escapedKeyword}%`, `%${escapedKeyword}%`);
    }
    const whereClause = conditions.length > 0
        ? `WHERE ${conditions.join(" AND ")}`
        : "";

    try {
        const [countRows] = await pool.execute(
            `SELECT COUNT(*) AS total FROM ForumPosts p ${whereClause}`,
            values
        );
        const total = Number(countRows[0].total);
        const [posts] = await pool.execute(
            `SELECT p.PostId, p.UserId, u.username AS AuthorName,
                    p.CategoryId, c.CategoryName, p.Title, p.Content,
                    p.LikeCount, p.CommentCount, p.CreatedAt, p.UpdatedAt
             FROM ForumPosts p
             INNER JOIN users u ON u.id = p.UserId
             INNER JOIN PostCategories c ON c.CategoryId = p.CategoryId
             ${whereClause}
             ORDER BY p.CreatedAt DESC, p.PostId DESC
             LIMIT ? OFFSET ?`,
            [...values, limit, offset]
        );

        let mediaRows = [];
        if (posts.length > 0) {
            [mediaRows] = await pool.execute(
                `SELECT PostId, MediaUrl FROM PostMedia
                 WHERE PostId IN (${posts.map(() => "?").join(", ")})
                 ORDER BY SortOrder ASC, MediaId ASC`,
                posts.map((post) => post.PostId)
            );
        }
        const imagesByPostId = new Map();
        for (const media of mediaRows) {
            const postId = Number(media.PostId);
            if (!imagesByPostId.has(postId)) {
                imagesByPostId.set(postId, []);
            }
            imagesByPostId.get(postId).push(media.MediaUrl);
        }

        return res.status(200).json({
            success: true,
            data: posts.map((post) => ({
                postId: Number(post.PostId),
                userId: Number(post.UserId),
                authorName: post.AuthorName,
                categoryId: Number(post.CategoryId),
                categoryName: post.CategoryName,
                title: post.Title,
                content: post.Content,
                likeCount: Number(post.LikeCount),
                commentCount: Number(post.CommentCount),
                images: imagesByPostId.get(Number(post.PostId)) || [],
                createdAt: post.CreatedAt,
                updatedAt: post.UpdatedAt
            })),
            pagination: { page, limit, total, totalPages: Math.ceil(total / limit) }
        });
    } catch (error) {
        console.error("Lỗi GET /api/v1/posts:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi máy chủ khi lấy danh sách bài viết."
        });
    }
});

router.post("/", (req, res, next) => {
    const userId = getAuthenticatedUserId(req);
    if (userId === null) {
        return res.status(401).json({
            success: false,
            message: "Vui lòng đăng nhập để đăng bài viết."
        });
    }
    req.authenticatedUserId = userId;
    return next();
}, parsePostImages, async (req, res) => {
    const body = req.body || {};
    const title = typeof body.title === "string" ? body.title.trim() : "";
    const content = typeof body.content === "string" ? body.content.trim() : "";
    const categoryId = typeof body.categoryId === "string"
        ? parsePositiveId(body.categoryId)
        : null;
    if (!title || title.length > MAX_POST_TITLE_LENGTH) {
        return res.status(400).json({
            success: false,
            message: `Tiêu đề không được để trống và tối đa ${MAX_POST_TITLE_LENGTH} ký tự.`
        });
    }
    if (!content || content.length > MAX_POST_CONTENT_LENGTH) {
        return res.status(400).json({
            success: false,
            message: `Nội dung không được để trống và tối đa ${MAX_POST_CONTENT_LENGTH} ký tự.`
        });
    }
    if (categoryId === null) {
        return res.status(400).json({
            success: false,
            message: "categoryId không hợp lệ."
        });
    }

    const files = req.files || [];
    const imageFormats = files.map((file) => getImageFormat(file.buffer));
    if (imageFormats.some((format, index) => !format || format.mimeType !== files[index].mimetype)) {
        return res.status(400).json({
            success: false,
            message: "Tệp tải lên không phải ảnh hợp lệ hoặc định dạng không khớp."
        });
    }

    let connection;
    let transactionStarted = false;
    const writtenFiles = [];
    try {
        connection = await pool.getConnection();
        const [categories] = await connection.execute(
            "SELECT CategoryId FROM PostCategories WHERE CategoryId = ? LIMIT 1",
            [categoryId]
        );
        if (categories.length === 0) {
            return res.status(400).json({
                success: false,
                message: "Danh mục bài viết không tồn tại."
            });
        }
        const [users] = await connection.execute(
            "SELECT id, username FROM users WHERE id = ? LIMIT 1",
            [req.authenticatedUserId]
        );
        if (users.length === 0) {
            return res.status(401).json({
                success: false,
                message: "Tài khoản trong access token không còn tồn tại."
            });
        }

        await connection.beginTransaction();
        transactionStarted = true;
        const [insertResult] = await connection.execute(
            `INSERT INTO ForumPosts (UserId, CategoryId, Title, Content)
             VALUES (?, ?, ?, ?)`,
            [req.authenticatedUserId, categoryId, title, content]
        );
        const postId = Number(insertResult.insertId);
        await fs.promises.mkdir(POST_IMAGE_DIRECTORY, { recursive: true });

        for (let index = 0; index < files.length; index += 1) {
            const filename = `${randomUUID()}.${imageFormats[index].extension}`;
            const imagePath = path.join(POST_IMAGE_DIRECTORY, filename);
            await fs.promises.writeFile(imagePath, files[index].buffer, { flag: "wx" });
            writtenFiles.push(imagePath);
            await connection.execute(
                "INSERT INTO PostMedia (PostId, MediaUrl, SortOrder) VALUES (?, ?, ?)",
                [postId, `/uploads/posts/${filename}`, index]
            );
        }

        await connection.commit();
        transactionStarted = false;
        return res.status(201).json({
            success: true,
            message: "Đã đăng bài viết.",
            data: {
                postId,
                userId: Number(req.authenticatedUserId),
                authorName: users[0].username,
                categoryId,
                title,
                content,
                images: writtenFiles.map((imagePath) => (
                    `/uploads/posts/${path.basename(imagePath)}`
                )),
                likeCount: 0,
                commentCount: 0
            }
        });
    } catch (error) {
        await rollback(connection, transactionStarted);
        await removePostImageFiles(
            writtenFiles,
            "Lỗi xóa ảnh bài viết sau khi lưu thất bại:"
        );
        console.error("Lỗi POST /api/v1/posts:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi máy chủ khi lưu bài viết."
        });
    } finally {
        if (connection) {
            connection.release();
        }
    }
});

router.put("/:id", (req, res, next) => {
    const postId = parsePositiveId(req.params.id);
    if (postId === null) {
        return res.status(400).json({
            success: false,
            message: "ID bài viết không hợp lệ."
        });
    }

    const userId = getAuthenticatedUserId(req);
    if (userId === null) {
        return res.status(401).json({
            success: false,
            message: "Vui lòng đăng nhập để chỉnh sửa bài viết."
        });
    }
    req.postId = postId;
    req.authenticatedUserId = userId;
    return next();
}, parsePostImages, async (req, res) => {
    const body = req.body || {};
    const hasTitle = Object.prototype.hasOwnProperty.call(body, "title");
    const hasContent = Object.prototype.hasOwnProperty.call(body, "content");
    const hasCategoryId = Object.prototype.hasOwnProperty.call(body, "categoryId");
    const title = hasTitle && typeof body.title === "string" ? body.title.trim() : null;
    const content = hasContent && typeof body.content === "string" ? body.content.trim() : null;
    let categoryId = null;
    if (hasCategoryId) {
        if (typeof body.categoryId === "string") {
            categoryId = parsePositiveId(body.categoryId);
        } else if (Number.isSafeInteger(body.categoryId) && body.categoryId > 0) {
            categoryId = body.categoryId;
        }
    }
    const files = req.files || [];
    const hasImages = files.length > 0;

    if (
        (hasTitle && (title === null || !title || title.length > MAX_POST_TITLE_LENGTH)) ||
        (hasContent && (content === null || !content || content.length > MAX_POST_CONTENT_LENGTH))
    ) {
        return res.status(400).json({
            success: false,
            message: "Tiêu đề hoặc nội dung không hợp lệ."
        });
    }
    if (hasCategoryId && categoryId === null) {
        return res.status(400).json({
            success: false,
            message: "categoryId không hợp lệ."
        });
    }
    if (!hasTitle && !hasContent && !hasCategoryId && !hasImages) {
        return res.status(400).json({
            success: false,
            message: "Cần cung cấp ít nhất một trường cần cập nhật."
        });
    }

    const imageFormats = files.map((file) => getImageFormat(file.buffer));
    if (imageFormats.some((format, index) => !format || format.mimeType !== files[index].mimetype)) {
        return res.status(400).json({
            success: false,
            message: "Tệp tải lên không phải ảnh hợp lệ hoặc định dạng không khớp."
        });
    }

    let connection;
    let transactionStarted = false;
    const writtenFiles = [];
    let oldImagePaths = [];
    try {
        connection = await pool.getConnection();
        await connection.beginTransaction();
        transactionStarted = true;

        const [posts] = await connection.execute(
            "SELECT PostId, UserId FROM ForumPosts WHERE PostId = ? FOR UPDATE",
            [req.postId]
        );
        if (posts.length === 0) {
            await rollback(connection, transactionStarted);
            transactionStarted = false;
            return res.status(404).json({
                success: false,
                message: "Không tìm thấy bài viết."
            });
        }
        if (Number(posts[0].UserId) !== req.authenticatedUserId) {
            await rollback(connection, transactionStarted);
            transactionStarted = false;
            return res.status(403).json({
                success: false,
                message: "Bạn không có quyền chỉnh sửa bài viết này."
            });
        }

        if (hasCategoryId) {
            const [categories] = await connection.execute(
                "SELECT CategoryId FROM PostCategories WHERE CategoryId = ? LIMIT 1",
                [categoryId]
            );
            if (categories.length === 0) {
                await rollback(connection, transactionStarted);
                transactionStarted = false;
                return res.status(400).json({
                    success: false,
                    message: "Danh mục bài viết không tồn tại."
                });
            }
        }

        const updates = [];
        const values = [];
        if (hasTitle) {
            updates.push("Title = ?");
            values.push(title);
        }
        if (hasContent) {
            updates.push("Content = ?");
            values.push(content);
        }
        if (hasCategoryId) {
            updates.push("CategoryId = ?");
            values.push(categoryId);
        }
        if (updates.length > 0) {
            await connection.execute(
                `UPDATE ForumPosts SET ${updates.join(", ")} WHERE PostId = ?`,
                [...values, req.postId]
            );
        }

        if (hasImages) {
            const [oldMedia] = await connection.execute(
                "SELECT MediaUrl FROM PostMedia WHERE PostId = ?",
                [req.postId]
            );
            oldImagePaths = oldMedia
                .map((media) => getManagedPostImagePath(media.MediaUrl))
                .filter(Boolean);
            await connection.execute(
                "DELETE FROM PostMedia WHERE PostId = ?",
                [req.postId]
            );
            await fs.promises.mkdir(POST_IMAGE_DIRECTORY, { recursive: true });
            for (let index = 0; index < files.length; index += 1) {
                const filename = `${randomUUID()}.${imageFormats[index].extension}`;
                const imagePath = path.join(POST_IMAGE_DIRECTORY, filename);
                await fs.promises.writeFile(imagePath, files[index].buffer, { flag: "wx" });
                writtenFiles.push(imagePath);
                await connection.execute(
                    "INSERT INTO PostMedia (PostId, MediaUrl, SortOrder) VALUES (?, ?, ?)",
                    [req.postId, `/uploads/posts/${filename}`, index]
                );
            }
        }

        const [updatedPosts] = await connection.execute(
            `SELECT p.PostId, p.UserId, u.username AS AuthorName,
                    p.CategoryId, c.CategoryName, p.Title, p.Content,
                    p.LikeCount, p.CommentCount, p.CreatedAt, p.UpdatedAt
             FROM ForumPosts p
             INNER JOIN users u ON u.id = p.UserId
             INNER JOIN PostCategories c ON c.CategoryId = p.CategoryId
             WHERE p.PostId = ?`,
            [req.postId]
        );
        if (updatedPosts.length === 0) {
            throw new Error("Không tìm thấy bài viết sau khi cập nhật.");
        }
        const [updatedMedia] = await connection.execute(
            "SELECT MediaUrl FROM PostMedia WHERE PostId = ? ORDER BY SortOrder ASC, MediaId ASC",
            [req.postId]
        );

        await connection.commit();
        transactionStarted = false;
        if (hasImages) {
            await removePostImageFiles(
                oldImagePaths,
                "Lỗi xóa ảnh cũ sau khi cập nhật bài viết:"
            );
        }

        const post = updatedPosts[0];
        return res.status(200).json({
            success: true,
            message: "Đã cập nhật bài viết.",
            data: {
                postId: Number(post.PostId),
                userId: Number(post.UserId),
                authorName: post.AuthorName,
                categoryId: Number(post.CategoryId),
                categoryName: post.CategoryName,
                title: post.Title,
                content: post.Content,
                likeCount: Number(post.LikeCount),
                commentCount: Number(post.CommentCount),
                images: updatedMedia.map((media) => media.MediaUrl),
                createdAt: post.CreatedAt,
                updatedAt: post.UpdatedAt
            }
        });
    } catch (error) {
        await rollback(connection, transactionStarted);
        await removePostImageFiles(
            writtenFiles,
            "Lỗi xóa ảnh bài viết sau khi cập nhật thất bại:"
        );
        console.error("Lỗi PUT /api/v1/posts/:id:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi máy chủ khi cập nhật bài viết."
        });
    } finally {
        if (connection) {
            connection.release();
        }
    }
});

router.delete("/:id", async (req, res) => {
    const postId = parsePositiveId(req.params.id);
    if (postId === null) {
        return res.status(400).json({
            success: false,
            message: "ID bài viết không hợp lệ."
        });
    }

    const userId = getAuthenticatedUserId(req);
    if (userId === null) {
        return res.status(401).json({
            success: false,
            message: "Vui lòng đăng nhập để xóa bài viết."
        });
    }

    let connection;
    let transactionStarted = false;
    let imagePaths = [];
    try {
        connection = await pool.getConnection();
        await connection.beginTransaction();
        transactionStarted = true;

        const [posts] = await connection.execute(
            "SELECT PostId, UserId FROM ForumPosts WHERE PostId = ? FOR UPDATE",
            [postId]
        );
        if (posts.length === 0) {
            await rollback(connection, transactionStarted);
            transactionStarted = false;
            return res.status(404).json({
                success: false,
                message: "Không tìm thấy bài viết."
            });
        }
        if (Number(posts[0].UserId) !== userId) {
            await rollback(connection, transactionStarted);
            transactionStarted = false;
            return res.status(403).json({
                success: false,
                message: "Bạn không có quyền xóa bài viết này."
            });
        }

        const [media] = await connection.execute(
            "SELECT MediaUrl FROM PostMedia WHERE PostId = ?",
            [postId]
        );
        imagePaths = media
            .map((item) => getManagedPostImagePath(item.MediaUrl))
            .filter(Boolean);
        await connection.execute(
            "DELETE FROM ForumPosts WHERE PostId = ?",
            [postId]
        );

        await connection.commit();
        transactionStarted = false;
        await removePostImageFiles(
            imagePaths,
            "Lỗi xóa ảnh sau khi xóa bài viết:"
        );
        return res.status(200).json({
            success: true,
            message: "Đã xóa bài viết.",
            data: { postId }
        });
    } catch (error) {
        await rollback(connection, transactionStarted);
        console.error("Lỗi DELETE /api/v1/posts/:id:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi máy chủ khi xóa bài viết."
        });
    } finally {
        if (connection) {
            connection.release();
        }
    }
});

router.post("/:id/like", async (req, res) => {
    const postId = parsePositiveId(req.params.id);
    if (postId === null) {
        return res.status(400).json({
            success: false,
            message: "ID bài viết không hợp lệ."
        });
    }

    const userId = getAuthenticatedUserId(req);
    if (userId === null) {
        return res.status(401).json({
            success: false,
            message: "Vui lòng đăng nhập để thích bài viết."
        });
    }

    let connection;
    let transactionStarted = false;
    try {
        connection = await pool.getConnection();
        await connection.beginTransaction();
        transactionStarted = true;

        const [posts] = await connection.execute(
            "SELECT PostId FROM ForumPosts WHERE PostId = ? FOR UPDATE",
            [postId]
        );
        if (posts.length === 0) {
            await rollback(connection, transactionStarted);
            transactionStarted = false;
            return res.status(404).json({
                success: false,
                message: "Không tìm thấy bài viết."
            });
        }

        const [users] = await connection.execute(
            "SELECT id FROM users WHERE id = ? FOR UPDATE",
            [userId]
        );
        if (users.length === 0) {
            await rollback(connection, transactionStarted);
            transactionStarted = false;
            return res.status(401).json({
                success: false,
                message: "Tài khoản trong access token không còn tồn tại."
            });
        }

        const [likes] = await connection.execute(
            "SELECT LikeId FROM PostLikes WHERE PostId = ? AND UserId = ? LIMIT 1",
            [postId, userId]
        );
        const isLiked = likes.length === 0;

        if (isLiked) {
            await connection.execute(
                "INSERT INTO PostLikes (PostId, UserId) VALUES (?, ?)",
                [postId, userId]
            );
        } else {
            await connection.execute(
                "DELETE FROM PostLikes WHERE LikeId = ?",
                [likes[0].LikeId]
            );
        }

        const [updatedPosts] = await connection.execute(
            "SELECT LikeCount FROM ForumPosts WHERE PostId = ?",
            [postId]
        );
        await connection.commit();
        transactionStarted = false;

        return res.status(200).json({
            success: true,
            message: isLiked ? "Đã thích bài viết." : "Đã bỏ thích bài viết.",
            data: {
                postId,
                userId,
                isLiked,
                likeCount: Number(updatedPosts[0].LikeCount)
            }
        });
    } catch (error) {
        await rollback(connection, transactionStarted);
        console.error("Lỗi POST /api/v1/posts/:id/like:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi máy chủ khi cập nhật lượt thích."
        });
    } finally {
        if (connection) {
            connection.release();
        }
    }
});

router.get("/:id/comments", async (req, res) => {
    const postId = parsePositiveId(req.params.id);
    if (postId === null) {
        return res.status(400).json({
            success: false,
            message: "ID bài viết không hợp lệ."
        });
    }

    try {
        const [posts] = await pool.execute(
            "SELECT PostId, CommentCount FROM ForumPosts WHERE PostId = ? LIMIT 1",
            [postId]
        );
        if (posts.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Không tìm thấy bài viết."
            });
        }

        const [rows] = await pool.execute(
            `SELECT c.CommentId, c.PostId, c.UserId, c.ParentCommentId,
                    c.Content, c.CreatedAt, u.username AS AuthorName
             FROM PostComments c
             INNER JOIN users u ON u.id = c.UserId
             WHERE c.PostId = ?
             ORDER BY c.CreatedAt ASC, c.CommentId ASC`,
            [postId]
        );
        const commentsById = new Map();
        const comments = [];

        for (const row of rows) {
            const comment = {
                commentId: row.CommentId,
                postId: row.PostId,
                userId: row.UserId,
                parentCommentId: row.ParentCommentId,
                authorName: row.AuthorName,
                content: row.Content,
                createdAt: row.CreatedAt,
                replies: []
            };
            commentsById.set(Number(row.CommentId), comment);

            if (row.ParentCommentId === null) {
                comments.push(comment);
                continue;
            }

            const parent = commentsById.get(Number(row.ParentCommentId));
            if (parent) {
                parent.replies.push(comment);
            }
        }

        return res.status(200).json({
            success: true,
            commentCount: Number(posts[0].CommentCount),
            data: comments
        });
    } catch (error) {
        console.error("Lỗi GET /api/v1/posts/:id/comments:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi máy chủ khi lấy bình luận."
        });
    }
});

router.post("/:id/comments", async (req, res) => {
    const postId = parsePositiveId(req.params.id);
    if (postId === null) {
        return res.status(400).json({
            success: false,
            message: "ID bài viết không hợp lệ."
        });
    }

    const userId = getAuthenticatedUserId(req);
    if (userId === null) {
        return res.status(401).json({
            success: false,
            message: "Vui lòng đăng nhập để bình luận."
        });
    }

    const body = req.body || {};
    if (typeof body.content !== "string" || !body.content.trim()) {
        return res.status(400).json({
            success: false,
            message: "Nội dung bình luận không được để trống."
        });
    }

    const content = body.content.trim();
    if (content.length > MAX_COMMENT_LENGTH) {
        return res.status(400).json({
            success: false,
            message: `Nội dung bình luận tối đa ${MAX_COMMENT_LENGTH} ký tự.`
        });
    }

    let parentCommentId = null;
    if (body.parentCommentId !== undefined && body.parentCommentId !== null) {
        if (
            !Number.isSafeInteger(body.parentCommentId) ||
            body.parentCommentId <= 0
        ) {
            return res.status(400).json({
                success: false,
                message: "parentCommentId không hợp lệ."
            });
        }
        parentCommentId = body.parentCommentId;
    }

    let connection;
    let transactionStarted = false;
    try {
        connection = await pool.getConnection();
        await connection.beginTransaction();
        transactionStarted = true;

        const [posts] = await connection.execute(
            "SELECT PostId FROM ForumPosts WHERE PostId = ? FOR UPDATE",
            [postId]
        );
        if (posts.length === 0) {
            await rollback(connection, transactionStarted);
            transactionStarted = false;
            return res.status(404).json({
                success: false,
                message: "Không tìm thấy bài viết."
            });
        }

        if (parentCommentId !== null) {
            const [parents] = await connection.execute(
                `SELECT CommentId, ParentCommentId
                 FROM PostComments
                 WHERE CommentId = ? AND PostId = ?
                 FOR UPDATE`,
                [parentCommentId, postId]
            );
            if (parents.length === 0) {
                await rollback(connection, transactionStarted);
                transactionStarted = false;
                return res.status(400).json({
                    success: false,
                    message: "Không tìm thấy bình luận cha trong bài viết."
                });
            }
            if (parents[0].ParentCommentId !== null) {
                await rollback(connection, transactionStarted);
                transactionStarted = false;
                return res.status(400).json({
                    success: false,
                    message: "Chỉ hỗ trợ trả lời tối đa hai cấp."
                });
            }
        }

        const [users] = await connection.execute(
            "SELECT id, username FROM users WHERE id = ? LIMIT 1",
            [userId]
        );
        if (users.length === 0) {
            await rollback(connection, transactionStarted);
            transactionStarted = false;
            return res.status(401).json({
                success: false,
                message: "Tài khoản trong access token không còn tồn tại."
            });
        }

        const [result] = await connection.execute(
            `INSERT INTO PostComments (PostId, UserId, ParentCommentId, Content)
             VALUES (?, ?, ?, ?)`,
            [postId, userId, parentCommentId, content]
        );
        const [insertedComments] = await connection.execute(
            `SELECT CommentId AS commentId, PostId AS postId, UserId AS userId,
                    ParentCommentId AS parentCommentId, Content AS content,
                    CreatedAt AS createdAt
             FROM PostComments
             WHERE CommentId = ?`,
            [result.insertId]
        );
        const [updatedPosts] = await connection.execute(
            "SELECT CommentCount FROM ForumPosts WHERE PostId = ?",
            [postId]
        );

        await connection.commit();
        transactionStarted = false;

        return res.status(201).json({
            success: true,
            message: parentCommentId === null
                ? "Đã gửi bình luận."
                : "Đã gửi câu trả lời.",
            commentCount: Number(updatedPosts[0].CommentCount),
            data: {
                ...insertedComments[0],
                authorName: users[0].username,
                replies: []
            }
        });
    } catch (error) {
        await rollback(connection, transactionStarted);
        console.error("Lỗi POST /api/v1/posts/:id/comments:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi máy chủ khi lưu bình luận."
        });
    } finally {
        if (connection) {
            connection.release();
        }
    }
});

module.exports = router;
