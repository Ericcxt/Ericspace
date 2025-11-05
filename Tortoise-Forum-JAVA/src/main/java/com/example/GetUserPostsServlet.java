package com.example;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.gson.Gson;

@WebServlet("/GetUserPostsServlet")
public class GetUserPostsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> jsonResponse = new HashMap<>();
        Gson gson = new Gson();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "User not logged in.");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        String username = (String) session.getAttribute("username");
        List<Map<String, Object>> posts = new ArrayList<>();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                // 1. 获取用户创建的所有主题（作为帖子）
                String sqlTopics = "SELECT " +
                                   "t.id AS topic_id, " +
                                   "p.id AS post_id, " +
                                   "t.title, " +
                                   "p.content, " +
                                   "p.createtime, " +
                                   "'topic' AS type " +
                                   "FROM javatortoisetopic t " +
                                   "JOIN javatortoisepost p ON t.id = p.topicid " +
                                   "WHERE t.userid = ? AND p.id = ( " +
                                   "SELECT MIN(id) FROM javatortoisepost WHERE topicid = t.id" +
                                   ")";
                try (PreparedStatement stmtTopics = conn.prepareStatement(sqlTopics)) {
                    stmtTopics.setString(1, username);
                    try (ResultSet rsTopics = stmtTopics.executeQuery()) {
                        while (rsTopics.next()) {
                            Map<String, Object> post = new HashMap<>();
                            post.put("topic_id", rsTopics.getInt("topic_id"));
                            post.put("post_id", rsTopics.getInt("post_id"));
                            post.put("title", rsTopics.getString("title"));
                            post.put("content", rsTopics.getString("content"));
                            post.put("createtime", rsTopics.getString("createtime"));
                            post.put("type", rsTopics.getString("type"));
                            posts.add(post);
                        }
                    }
                }

                // 2. 获取用户的所有回复
                String sqlReplies = "SELECT " +
                                    "p.topicid AS topic_id, " +
                                    "p.id AS post_id, " +
                                    "t.title, " +
                                    "p.content, " +
                                    "p.createtime, " +
                                    "'reply' AS type " +
                                    "FROM javatortoisepost p " +
                                    "JOIN javatortoisetopic t ON p.topicid = t.id " +
                                    "WHERE p.userid = ? AND p.id NOT IN ( " +
                                    "SELECT MIN(id) FROM javatortoisepost GROUP BY topicid" +
                                    ")";
                try (PreparedStatement stmtReplies = conn.prepareStatement(sqlReplies)) {
                    stmtReplies.setString(1, username);
                    try (ResultSet rsReplies = stmtReplies.executeQuery()) {
                        while (rsReplies.next()) {
                            Map<String, Object> reply = new HashMap<>();
                            reply.put("topic_id", rsReplies.getInt("topic_id"));
                            reply.put("post_id", rsReplies.getInt("post_id"));
                            reply.put("title", rsReplies.getString("title"));
                            reply.put("content", rsReplies.getString("content"));
                            reply.put("createtime", rsReplies.getString("createtime"));
                            reply.put("type", rsReplies.getString("type"));
                            posts.add(reply);
                        }
                    }
                }
            }

            // 按创建时间降序排序所有帖子和回复
            Collections.sort(posts, new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                    String time1 = (String) o1.get("createtime");
                    String time2 = (String) o2.get("createtime");
                    return time2.compareTo(time1); // 降序
                }
            });

            jsonResponse.put("status", "success");
            jsonResponse.put("posts", posts);

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "An error occurred: " + e.getMessage());
        }

        response.getWriter().write(gson.toJson(jsonResponse));
    }
}