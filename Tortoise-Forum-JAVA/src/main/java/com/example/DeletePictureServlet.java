package com.example;

import java.io.File;
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

import com.google.gson.Gson;

@WebServlet("/DeletePictureServlet")
public class DeletePictureServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> jsonResponse = new HashMap<>();
        Gson gson = new Gson();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            jsonResponse.put("success", false);
            jsonResponse.put("message", "ログインしていません。");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        String username = (String) session.getAttribute("username");
        String pictureIdStr = request.getParameter("id");

        if (pictureIdStr == null || pictureIdStr.isEmpty()) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "画像IDがありません。");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        int pictureId;
        try {
            pictureId = Integer.parseInt(pictureIdStr);
        } catch (NumberFormatException e) {
            jsonResponse.put("success", false);
            jsonResponse.put("message", "無効な画像IDです。");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            conn.setAutoCommit(false); // Start transaction

            String filepath = null;
            // --- 使用正确的 'javatortoisepicture' 表 ---
            String sqlSelect = "SELECT filepath FROM javatortoisepicture WHERE id = ? AND userid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlSelect)) {
                stmt.setInt(1, pictureId);
                stmt.setString(2, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        filepath = rs.getString("filepath");
                    } else {
                        throw new SecurityException("画像が見つからないか、削除する権限がありません。");
                    }
                }
            }

            // 数据库中删除
            // --- 使用正确的 'javatortoisepicture' 表 ---
            String sqlDelete = "DELETE FROM javatortoisepicture WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlDelete)) {
                stmt.setInt(1, pictureId);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("データベースからの画像の削除に失敗しました。");
                }
            }

            // 删除物理文件
            if (filepath != null && !filepath.isEmpty()) {
                String absoluteFilePath = getServletContext().getRealPath("/") + filepath;
                File fileToDelete = new File(absoluteFilePath);
                if (fileToDelete.exists()) {
                    if (!fileToDelete.delete()) {
                        System.err.println("Warning: Failed to delete file: " + absoluteFilePath);
                    }
                }
            }

            conn.commit(); 
            jsonResponse.put("success", true);
            jsonResponse.put("message", "画像が正常に削除されました。");

        } catch (ClassNotFoundException | SQLException | SecurityException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            jsonResponse.put("success", false);
            jsonResponse.put("message", e.getMessage());
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