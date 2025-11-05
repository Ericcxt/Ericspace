package com.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
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

@WebServlet("/get_paged_topics")
public class GetPagedTopicsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        int page = 1;
        try {
            if (request.getParameter("page") != null && !request.getParameter("page").isEmpty()) {
                page = Integer.parseInt(request.getParameter("page"));
            }
        } catch (NumberFormatException e) {
            // 默认设置为第一页
            page = 1;
        }

        String sortBy = request.getParameter("sort_by");
        if (sortBy == null || !sortBy.equals("popularity")) {
            sortBy = "time";
        }

        int limit = 10;
        int offset = (page - 1) * limit;

        Map<String, Object> jsonResponse = new HashMap<>();
        List<Map<String, Object>> topics = new ArrayList<>();
        int totalPages = 0;

        String countSql = "SELECT COUNT(*) as total FROM javatortoisetopic";
        String dataSql = "SELECT " +
                         "    t.id, " +
                         "    t.title, " +
                         "    t.userid AS author, " +
                         "    t.createtime, " +
                         "    t.views, " +
                         "    (SELECT COUNT(*) FROM javatortoisepost WHERE topicid = t.id) AS replies, " +
                         "    COALESCE((SELECT MAX(createtime) FROM javatortoisepost WHERE topicid = t.id), t.createtime) AS last_post_time_sort, " +
                         "    (SELECT userid FROM javatortoisepost WHERE topicid = t.id ORDER BY createtime DESC LIMIT 1) AS last_poster, " +
                         "    (SELECT MAX(createtime) FROM javatortoisepost WHERE topicid = t.id) AS last_post_time " +
                         "FROM " +
                         "    javatortoisetopic t " +
                         "ORDER BY " +
                         ("popularity".equals(sortBy) ? "views DESC, last_post_time_sort DESC" : "last_post_time_sort DESC") +
                         " LIMIT ? OFFSET ?";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "MySQL JDBC Driver not found: " + e.getMessage());
            response.getWriter().write(new Gson().toJson(jsonResponse));
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // 取得总主题数
            try (PreparedStatement countStmt = conn.prepareStatement(countSql);
                 ResultSet rs = countStmt.executeQuery()) {
                if (rs.next()) {
                    int totalTopics = rs.getInt("total");
                    totalPages = (int) Math.ceil((double) totalTopics / limit);
                }
            }

            // 取得分页数据
            try (PreparedStatement dataStmt = conn.prepareStatement(dataSql)) {
                dataStmt.setInt(1, limit);
                dataStmt.setInt(2, offset);
                try (ResultSet rs = dataStmt.executeQuery()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    while (rs.next()) {
                        Map<String, Object> topic = new HashMap<>();
                        topic.put("id", rs.getInt("id"));
                        topic.put("title", rs.getString("title"));
                        topic.put("author", rs.getString("author"));
                        topic.put("createtime", sdf.format(rs.getTimestamp("createtime")));
                        topic.put("views", rs.getInt("views"));
                        topic.put("replies", rs.getInt("replies"));
                        topic.put("last_poster", rs.getString("last_poster"));
                        
                        Timestamp lastPostTime = rs.getTimestamp("last_post_time");
                        topic.put("last_post_time", lastPostTime != null ? sdf.format(lastPostTime) : null);
                        
                        topics.add(topic);
                    }
                }
            }
            
            jsonResponse.put("status", "success");
            jsonResponse.put("total_pages", totalPages);
            jsonResponse.put("current_page", page);
            jsonResponse.put("topics", topics);

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "Database error: " + e.getMessage());
        }

        PrintWriter out = response.getWriter();
        out.print(new Gson().toJson(jsonResponse));
        out.flush();
    }
}