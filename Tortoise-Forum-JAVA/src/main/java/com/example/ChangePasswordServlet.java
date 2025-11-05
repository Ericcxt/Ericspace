package com.example;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;

import com.google.gson.Gson;

@WebServlet("/ChangePasswordServlet")
public class ChangePasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> jsonResponse = new HashMap<>();
        Gson gson = new Gson();
        Connection conn = null;

        try {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("username") == null) {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "ログインしていません。再度ログインしてください。");
            } else {
                String username = (String) session.getAttribute("username");
                String currentPassword = request.getParameter("current_password");
                String newPassword = request.getParameter("new_password");
                String confirmPassword = request.getParameter("confirm_new_password");

                if (currentPassword == null || newPassword == null || confirmPassword == null ||
                    currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    jsonResponse.put("status", "error");
                    jsonResponse.put("message", "すべてのパスワードフィールドは必須です。");
                } else if (!newPassword.equals(confirmPassword)) {
                    jsonResponse.put("status", "error");
                    jsonResponse.put("message", "新しいパスワードと確認用パスワードが一致しません。");
                } else {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

                    String sqlSelect = "SELECT password FROM javatortoiseuser WHERE username = ?";
                    String hashedPasswordFromDB = "";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlSelect)) {
                        stmt.setString(1, username);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                hashedPasswordFromDB = rs.getString("password");
                            } else {
                                throw new SQLException("User not found in database, but session exists.");
                            }
                        }
                    }

                    if (!BCrypt.checkpw(currentPassword, hashedPasswordFromDB)) {
                        jsonResponse.put("status", "error");
                        jsonResponse.put("message", "現在のパスワードが正しくありません。");
                    } else {
                        String newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                        String sqlUpdate = "UPDATE javatortoiseuser SET password = ? WHERE username = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                            stmt.setString(1, newHashedPassword);
                            stmt.setString(2, username);
                            int rowsAffected = stmt.executeUpdate();
                            if (rowsAffected > 0) {
                                jsonResponse.put("status", "success");
                                jsonResponse.put("message", "パスワードが正常に更新されました。");
                            } else {
                                throw new SQLException("Password update failed, no rows affected.");
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            // 确保在发生异常时，客户端能收到一个标准的错误信息
            if (jsonResponse.isEmpty()) {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "サーバーエラーが発生しました。");
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            response.getWriter().write(gson.toJson(jsonResponse));
        }
    }
}