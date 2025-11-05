package com.example;

import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/GetUserProfileServlet")
public class GetUserProfileServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> jsonResponse = new HashMap<>();
        Gson gson = new Gson();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_OK); 
            jsonResponse.put("status", "error");
            // 前端JS会处理这个消息并重定向
            jsonResponse.put("message", "マイページを表示するにはログインしてください"); 
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        String username = (String) session.getAttribute("username");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ServletException("MySQL JDBC Driver not found.", e);
        }

        String sql = "SELECT email, country, profilepic FROM javatortoiseuser WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // 3. 查询成功，构建成功的JSON响应
                    jsonResponse.put("status", "success");
                    jsonResponse.put("username", username);
                    jsonResponse.put("email", rs.getString("email"));
                    jsonResponse.put("country", rs.getString("country"));
                    jsonResponse.put("profilepic", rs.getString("profilepic"));
                } else {
                    // 虽然用户在session中，但在数据库里找不到，这是异常情况
                    jsonResponse.put("status", "error");
                    jsonResponse.put("message", "ユーザーが見つかりません。");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404 Not Found
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "データベース接続に失敗しました。");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.getWriter().write(gson.toJson(jsonResponse));
    }
}