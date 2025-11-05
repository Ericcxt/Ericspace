package com.example;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

// 将 Servlet 路径的连字符 '-' 改为下划线 '_'，以匹配 HTML 表单的 action
@WebServlet("/save_post")
public class SavePostServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 解决中文乱码问题：在读取任何参数之前，设置请求的编码为 UTF-8
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        String username = (session != null) ? (String) session.getAttribute("username") : null;

        if (username == null) {
            response.sendRedirect("TortoiseForumLog.html");
            return;
        }

        String topicIdStr = request.getParameter("topic_id");
        String content = request.getParameter("content");

        if (topicIdStr == null || content == null || content.trim().isEmpty()) {
            response.sendRedirect("TortoiseForumDiscussion.html?topic_id=" + topicIdStr + "&error=InvalidInput");
            return;
        }

        String dbUser = "root";
        String dbPassword = "";

        // 在数据库连接上指定 UTF-8 编码
        String dbURL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";

        Connection conn = null;
        PreparedStatement insertStmt = null;
        PreparedStatement updateStmt = null;
        try {
            int topicId = Integer.parseInt(topicIdStr);
            Timestamp now = Timestamp.from(Instant.now());

            Class.forName("com.mysql.cj.jdbc.Driver");
            // 【修复】使用正确的 dbURL 连接到 'test' 数据库
            conn = DriverManager.getConnection(dbURL, dbUser, dbPassword);
            conn.setAutoCommit(false); // 开始事务

            // 1. 插入新回复
            String insertSql = "INSERT INTO javatortoisepost (topicid, content, userid, createtime) VALUES (?, ?, ?, ?)";
            insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, topicId);
            insertStmt.setString(2, content);
            insertStmt.setString(3, username);
            insertStmt.setTimestamp(4, now);
            int rowsAffected = insertStmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Creating post failed, no rows affected.");
            }

            // 2. 更新主贴的最后回复时间
            String updateSql = "UPDATE javatortoisetopic SET lastreplytime = ? WHERE id = ?";
            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setTimestamp(1, now);
            updateStmt.setInt(2, topicId);
            updateStmt.executeUpdate();

            conn.commit(); // 提交事务

            response.sendRedirect("TortoiseForumDiscussion.html?topic_id=" + topicId);

        } catch (NumberFormatException e) {
            // 在重定向方案中，可以将错误信息作为查询参数传递，或显示一个通用的错误页面
            response.sendRedirect("TortoiseForumDiscussion.html?topic_id=" + request.getParameter("topic_id") + "&error=InvalidTopicID");
        } catch (SQLException | ClassNotFoundException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // 如果出错，回滚事务
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> error = new HashMap<>();
            error.put("message", "Database error: " + e.getMessage());
            response.getWriter().write(new Gson().toJson(error));
        } finally {
            try {
                if (insertStmt != null) insertStmt.close();
                if (updateStmt != null) updateStmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}