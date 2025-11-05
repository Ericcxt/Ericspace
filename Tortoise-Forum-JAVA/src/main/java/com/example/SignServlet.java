package com.example;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 引入 BCrypt
import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/SignServlet")
@MultipartConfig
public class SignServlet extends HttpServlet {

    // 将数据库连接信息直接定义在 Servlet 中
    private static final String DB_URL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String DB_USER = "root";
    private static final String DB_PASS = ""; // 您的 root 用户密码，如果为空则保持不变

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> jsonResponse = new HashMap<>();
        Gson gson = new Gson();

        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String country = request.getParameter("country");
        String password = request.getParameter("password");
        Part profilePicPart = request.getPart("profilepic");

        // --- 核心修改：将初始值从 null 改为空字符串 ---
        String profilePicPath = ""; 
        Connection conn = null;

        try {
            // 1. 加载驱动并建立数据库连接
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new ServletException("MySQL JDBC Driver not found.", e);
            }
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // 2. 检查用户名是否已存在
            if (isFieldExist(conn, "username", username)) {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "このユーザー名はすでに登録されています。");
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            // 3. 检查邮箱是否已存在
            if (isFieldExist(conn, "email", email)) {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "このメールアドレスはすでに登録されています。");
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }

            // 4. 处理头像上传
            if (profilePicPart != null && profilePicPart.getSize() > 0) {
                // 使用我们自己的辅助方法来兼容 Servlet 3.0
                String fileName = getFileName(profilePicPart);
                if (fileName != null && !fileName.isEmpty()) {
                    String fileExtension = fileName.substring(fileName.lastIndexOf("."));
                    // 移除文件名中多余的 "avatar_" 前缀，使其与图片上传功能一致
                    String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
                    
                    // 直接获取 /avatar 目录的真实路径，确保路径正确
                    String uploadPath = getServletContext().getRealPath("/avatar");
                    File uploadDir = new File(uploadPath);
                    if (!uploadDir.exists()) {
                        // 使用 mkdirs() 更稳妥，可以创建任何不存在的父目录
                        uploadDir.mkdirs();
                    }

                    try (InputStream input = profilePicPart.getInputStream()) {
                        Files.copy(input, new File(uploadDir, uniqueFileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    profilePicPath = "avatar/" + uniqueFileName;
                }
            }

            // 5. 对密码进行哈希处理
            String hashedPassword = hashPassword(password);

            // 6. 将新用户信息插入数据库
            String sql = "INSERT INTO javatortoiseuser (username, email, country, password, profilepic) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, email);
                stmt.setString(3, country);
                stmt.setString(4, hashedPassword);
                stmt.setString(5, profilePicPath);

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    jsonResponse.put("status", "success");
                    jsonResponse.put("message", "登録に成功しました！");
                } else {
                    throw new SQLException("データベースへの挿入が失敗しました。");
                }
            }

        } catch (SQLException e) { // <--- 修改这里的 catch
            e.printStackTrace();
            jsonResponse.put("status", "error");
            jsonResponse.put("message", "登録中にサーバーエラーが発生しました。");
        } finally {
            // 7. 确保数据库连接在最后被关闭
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        response.getWriter().write(gson.toJson(jsonResponse));
    }

    /**
     * 从 Part 的 "content-disposition" 头中提取文件名 (兼容 Servlet 3.0)
     */
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) {
            return null;
        }
        for (String cd : contentDisposition.split(";")) {
            if (cd.trim().startsWith("filename")) {
                // 文件名可能被双引号包围，需要去掉
                String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                // 某些浏览器会包含完整路径，我们需要提取文件名部分以防止路径遍历攻击
                return fileName.substring(fileName.lastIndexOf('/') + 1).substring(fileName.lastIndexOf('\\') + 1);
            }
        }
        return null;
    }

    private boolean isFieldExist(Connection conn, String fieldName, String value) throws SQLException {
        String sql = "SELECT " + fieldName + " FROM javatortoiseuser WHERE " + fieldName + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // 使用 BCrypt 替换 SHA-256
    private String hashPassword(String password) {
        // gensalt 方法会自动生成一个安全的盐
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}