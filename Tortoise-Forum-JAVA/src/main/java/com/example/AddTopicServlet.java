package com.example;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/add_topic")
public class AddTopicServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"用户未登录\"}");
            return;
        }

        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String username = (String) session.getAttribute("username");

        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"标题和内容为必填项\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement stmtTopic = null;
        PreparedStatement stmtPost = null;
        ResultSet generatedKeys = null;
        long newTopicId = -1;

        // 更新数据库连接并准备在 Java 中生成时间
        String dbURL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
        String dbUser = "root";
        String dbPassword = "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(dbURL, dbUser, dbPassword);
            conn.setAutoCommit(false);

            // 不再使用数据库的 NOW()，而是使用 Java 生成的 UTC 时间
            java.sql.Timestamp now = java.sql.Timestamp.from(java.time.Instant.now());

            String sqlTopic = "INSERT INTO javatortoisetopic (title, userid, createtime, lastreplytime) VALUES (?, ?, ?, ?)";
            stmtTopic = conn.prepareStatement(sqlTopic, Statement.RETURN_GENERATED_KEYS);
            stmtTopic.setString(1, title);
            stmtTopic.setString(2, username);
            stmtTopic.setTimestamp(3, now); // 使用 Java 生成的时间
            stmtTopic.setTimestamp(4, now); // 使用 Java 生成的时间
            stmtTopic.executeUpdate();

            generatedKeys = stmtTopic.getGeneratedKeys();
            if (generatedKeys.next()) {
                newTopicId = generatedKeys.getLong(1);
            } else {
                throw new SQLException("创建主题失败，无法获取ID。");
            }

            String sqlPost = "INSERT INTO javatortoisepost (topicid, userid, content, createtime) VALUES (?, ?, ?, ?)";
            stmtPost = conn.prepareStatement(sqlPost);
            stmtPost.setLong(1, newTopicId);
            stmtPost.setString(2, username);
            stmtPost.setString(3, content);
            stmtPost.setTimestamp(4, now); // 使用与主题一致的 Java 生成时间
            stmtPost.executeUpdate();

            conn.commit();

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "主题创建成功");
            result.put("topic_id", newTopicId);
            response.getWriter().write(new Gson().toJson(result));

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"创建主题失败: " + e.getMessage() + "\"}");
            e.printStackTrace();
        } finally {
            try { if (generatedKeys != null) generatedKeys.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmtPost != null) stmtPost.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmtTopic != null) stmtTopic.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}