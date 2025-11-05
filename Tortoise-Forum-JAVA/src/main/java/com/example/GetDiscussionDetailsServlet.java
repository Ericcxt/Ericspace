package com.example;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

@WebServlet("/get_discussion_details")
public class GetDiscussionDetailsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        int topicId = 0;
        try {
            topicId = Integer.parseInt(request.getParameter("topic_id"));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"无效的 topic_id\"}");
            return;
        }

        Connection conn = null;
        // 解决中文乱码问题：在数据库连接上指定 UTF-8 编码
        String dbURL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
        String dbUser = "root";
        String dbPassword = "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(dbURL, dbUser, dbPassword);
            conn.setAutoCommit(false);

            // 1. 更新浏览量
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE javatortoisetopic SET views = views + 1 WHERE id = ?")) {
                stmt.setInt(1, topicId);
                stmt.executeUpdate();
            }

            // 2. 获取帖子标题
            String topicTitle = "";
            try (PreparedStatement stmt = conn.prepareStatement("SELECT title FROM javatortoisetopic WHERE id = ?")) {
                stmt.setInt(1, topicId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        topicTitle = rs.getString("title");
                    } else {
                        throw new SQLException("找不到主题");
                    }
                }
            }

            // 3. 获取所有帖子内容和作者信息
            List<Map<String, Object>> allPosts = new ArrayList<>();
            String postsSql = "SELECT p.content, p.userid AS author, p.createtime, u.profilepic FROM javatortoisepost p LEFT JOIN javatortoiseuser u ON p.userid = u.username WHERE p.topicid = ? ORDER BY p.createtime ASC";
            try (PreparedStatement stmt = conn.prepareStatement(postsSql)) {
                stmt.setInt(1, topicId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> post = new HashMap<>();
                        post.put("content", rs.getString("content"));
                        post.put("author", rs.getString("author"));
                        post.put("createtime", rs.getTimestamp("createtime").toString());
                        
                        // 直接使用从数据库获取的相对路径
                        post.put("profilepic", rs.getString("profilepic"));
                        allPosts.add(post);
                    }
                }
            }
            
            conn.commit();

            // 4. 构建返回的 JSON 结构
            Map<String, Object> jsonResponse = new HashMap<>();
            if (allPosts.isEmpty()) {
                Map<String, Object> topicData = new HashMap<>();
                topicData.put("title", topicTitle);
                topicData.put("author", "N/A");
                topicData.put("createtime", "N/A");
                topicData.put("content", "此主题下没有内容。");
                jsonResponse.put("topic", topicData);
                jsonResponse.put("replies", new ArrayList<>());
            } else {
                Map<String, Object> mainPost = allPosts.remove(0);
                mainPost.put("title", topicTitle);
                jsonResponse.put("topic", mainPost);
                jsonResponse.put("replies", allPosts);
            }

            response.getWriter().write(new Gson().toJson(jsonResponse));

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"获取帖子详情失败: " + e.getMessage() + "\"}");
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}