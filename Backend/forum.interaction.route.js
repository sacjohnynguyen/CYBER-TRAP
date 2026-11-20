const express = require("express");
const jwt = require("jsonwebtoken");
const { pool } = require("./db");

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || "cyber_trap_jwt_secret_key_2026";
const MAX_COMMENT_LENGTH = 2000;

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
