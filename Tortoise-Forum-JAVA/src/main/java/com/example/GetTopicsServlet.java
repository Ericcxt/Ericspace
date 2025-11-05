package com.example;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/get_topics")
public class GetTopicsServlet extends HttpServlet {

    // 数据库连接信息
    private static final String DB_URL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        List<Map<String, Object>> topics = new ArrayList<>();
        // 将SQL语句中的表名更新为新的Java表名
        String sql = "SELECT " +
                     "    t.id, t.title, t.userid AS author, t.createtime, t.views, " +
                     "    (SELECT COUNT(*) FROM javatortoisepost WHERE topicid = t.id) AS replies, " +
                     "    (SELECT userid FROM javatortoisepost WHERE topicid = t.id ORDER BY createtime DESC LIMIT 1) AS last_poster, " +
                     "    (SELECT MAX(createtime) FROM javatortoisepost WHERE topicid = t.id) AS last_post_time " +
                     "FROM javatortoisetopic t " +
                     "ORDER BY t.createtime DESC LIMIT 6";

        Map<String, Object> responseMap = new HashMap<>();
        PrintWriter out = resp.getWriter();
        Gson gson = new Gson();

        try {
            // 1. 加载驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            // 2. 建立连接、执行查询
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                // 3. 处理结果
                while (rs.next()) {
                    Map<String, Object> topic = new HashMap<>();
                    topic.put("id", rs.getInt("id"));
                    topic.put("title", rs.getString("title"));
                    topic.put("author", rs.getString("author"));
                    
                    // 格式化时间以匹配PHP的输出
                    Timestamp createTimestamp = rs.getTimestamp("createtime");
                    if (createTimestamp != null) {
                        topic.put("createtime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(createTimestamp));
                    }

                    topic.put("views", rs.getInt("views"));
                    topic.put("replies", rs.getInt("replies"));
                    topic.put("last_poster", rs.getString("last_poster"));

                    Timestamp lastPostTimestamp = rs.getTimestamp("last_post_time");
                    if (lastPostTimestamp != null) {
                        topic.put("last_post_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastPostTimestamp));
                    } else {
                        topic.put("last_post_time", null);
                    }
                    
                    topics.add(topic);
                }
                responseMap.put("status", "success");
                responseMap.put("topics", topics);
            }
        } catch (ClassNotFoundException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("status", "error");
            responseMap.put("message", "数据库驱动加载失败: " + e.getMessage());
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("status", "error");
            responseMap.put("message", "数据库查询错误: " + e.getMessage());
        }

        // 4. 将结果转换为JSON并发送回前端
        out.print(gson.toJson(responseMap));
        out.flush();
    }
}