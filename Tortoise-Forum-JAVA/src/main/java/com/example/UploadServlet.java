package com.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.google.gson.Gson;

import javax.servlet.annotation.WebServlet; // 确保导入

// 使用 @MultipartConfig 注解来启用对 multipart/form-data 请求的处理
@MultipartConfig
@WebServlet("/UploadServlet")
public class UploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // 数据库连接信息
    private static final String DB_URL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> responseData = new HashMap<>();
        PrintWriter out = response.getWriter();
        Connection conn = null;

        try {
            // 1. 检查用户登录状态
            HttpSession session = request.getSession(false); // false 表示不创建新 session
            if (session == null || session.getAttribute("username") == null) {
                throw new ServletException("用户未登录，禁止上传。");
            }
            String username = (String) session.getAttribute("username");

            // 2. 从请求中获取上传的文件部分
            Part filePart = request.getPart("file"); // "file" 对应前端 FormData 中的 key
            if (filePart == null || filePart.getSize() == 0) {
                throw new ServletException("没有文件被上传。");
            }
            String originalFileName = getFileName(filePart);

            // 3. 检查文件类型，与 PHP 版本保持一致
            String fileExt = "";
            int i = originalFileName.lastIndexOf('.');
            if (i > 0) {
                fileExt = originalFileName.substring(i + 1).toLowerCase();
            }
            List<String> allowedTypes = Arrays.asList("jpg", "jpeg", "png", "gif");
            if (fileExt.isEmpty() || !allowedTypes.contains(fileExt)) {
                throw new ServletException("无效的图片格式。只允许 JPG, JPEG, PNG, GIF。");
            }

            // 4. 生成唯一文件名
            String uniqueFileName = UUID.randomUUID().toString() + "." + fileExt;

            // 5. 定义并创建上传目录，然后保存文件
            // getServletContext().getRealPath("") 获取的是项目部署后的根目录
            String uploadPath = getServletContext().getRealPath("") + File.separator + "uploads";
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdir(); // 如果 uploads 目录不存在，则创建它
            }
            
            String filePath = uploadPath + File.separator + uniqueFileName;
            try (InputStream fileContent = filePart.getInputStream()) {
                Files.copy(fileContent, Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
            }

            // 6. 将文件信息存入数据库
            String relativeFilePath = "uploads/" + uniqueFileName; // 存入数据库的应是相对路径

            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            String sql = "INSERT INTO javatortoisepicture (userid, filepath) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, relativeFilePath);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("数据库插入失败，没有行被影响。");
            }
            stmt.close();

            // 7. 发送成功响应
            responseData.put("success", true);
            responseData.put("message", "文件上传成功并已记录到数据库。");
            responseData.put("filepath", relativeFilePath);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseData.put("success", false);
            responseData.put("error", e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // 忽略关闭连接时的错误
                }
            }
            out.print(new Gson().toJson(responseData));
            out.flush();
        }
    }

    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        for (String content : contentDisposition.split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
}