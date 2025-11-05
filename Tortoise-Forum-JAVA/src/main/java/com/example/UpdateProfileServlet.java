package com.example;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

@WebServlet("/UpdateProfileServlet")
@MultipartConfig
public class UpdateProfileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private String getSubmittedFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                // For IE browser
                return fileName.substring(fileName.lastIndexOf('/') + 1).substring(fileName.lastIndexOf('\\') + 1);
            }
        }
        return null;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"status\": \"error\", \"message\": \"ログインしていません。\"}");
            return;
        }

        String username = (String) session.getAttribute("username");
        request.setCharacterEncoding("UTF-8");
        String email = request.getParameter("email");
        String country = request.getParameter("country");
        Part avatarPart = request.getPart("avatar");

        String oldAvatarPath = null;
        boolean newAvatarUploaded = false;

        List<String> updates = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=UTF-8", "root", "");

            if (avatarPart != null && avatarPart.getSize() > 0) {
                String submittedFileName = getSubmittedFileName(avatarPart);

                if (submittedFileName != null && !submittedFileName.isEmpty()) {
                    // --- 核心修正：使用正确的表名 javatortoiseuser ---
                    try (PreparedStatement stmtOld = conn.prepareStatement(
                            "SELECT profilepic FROM javatortoiseuser WHERE username = ?")) {
                        stmtOld.setString(1, username);
                        try (ResultSet rs = stmtOld.executeQuery()) {
                            if (rs.next()) {
                                oldAvatarPath = rs.getString("profilepic");
                            }
                        }
                    }

                    String fileExt = submittedFileName.substring(submittedFileName.lastIndexOf('.') + 1).toLowerCase();
                    List<String> allowedExt = new ArrayList<>();
                    allowedExt.add("jpg");
                    allowedExt.add("jpeg");
                    allowedExt.add("png");
                    allowedExt.add("gif");

                    if (allowedExt.contains(fileExt)) {
                        String newFileName = UUID.randomUUID().toString() + "." + fileExt;
                        String uploadPath = getServletContext().getRealPath("") + File.separator + "avatar";
                        File uploadDir = new File(uploadPath);
                        if (!uploadDir.exists()) {
                            uploadDir.mkdirs();
                        }
                        
                        try (InputStream input = avatarPart.getInputStream()) {
                            Files.copy(input, Paths.get(uploadPath, newFileName), StandardCopyOption.REPLACE_EXISTING);
                            String newAvatarPath = "avatar/" + newFileName;
                            updates.add("profilepic = ?");
                            params.add(newAvatarPath);
                            newAvatarUploaded = true;
                        }
                    } else {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.getWriter().write("{\"status\": \"error\", \"message\": \"無効なファイルタイプです。\"}");
                        return;
                    }
                }
            }

            if (email != null && !email.trim().isEmpty()) {
                // --- 使用正确的表名 javatortoiseuser ---
                try (PreparedStatement stmtCheck = conn.prepareStatement(
                        "SELECT email FROM javatortoiseuser WHERE email = ? AND username != ?")) {
                    stmtCheck.setString(1, email);
                    stmtCheck.setString(2, username);
                    try (ResultSet rs = stmtCheck.executeQuery()) {
                        if (rs.next()) {
                            response.setStatus(HttpServletResponse.SC_CONFLICT);
                            response.getWriter().write("{\"status\": \"error\", \"message\": \"このメールアドレスは既に他のユーザーによって登録されています。\"}");
                            return;
                        }
                    }
                }
                updates.add("email = ?");
                params.add(email);
            }

            if (country != null && !country.trim().isEmpty()) {
                updates.add("country = ?");
                params.add(country);
            }

            if (updates.isEmpty()) {
                response.getWriter().write("{\"status\": \"info\", \"message\": \"変更は送信されませんでした。\"}");
                return;
            }

            // --- 使用正确的表名 javatortoiseuser ---
            String sql = "UPDATE javatortoiseuser SET " + String.join(", ", updates) + " WHERE username = ?";
            params.add(username);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    if (newAvatarUploaded && oldAvatarPath != null && !oldAvatarPath.isEmpty()) {
                         File oldAvatarFile = new File(getServletContext().getRealPath("") + File.separator + oldAvatarPath);
                         if (oldAvatarFile.exists()) {
                             oldAvatarFile.delete();
                         }
                    }
                    response.getWriter().write("{\"status\": \"success\", \"message\": \"プロフィールは正常に更新されました。\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("{\"status\": \"error\", \"message\": \"プロフィールの更新に失敗しました。\"}");
                }
            }

        } catch (ClassNotFoundException | SQLException | IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\": \"error\", \"message\": \"データベースエラーまたはサーバーエラーが発生しました。: " + e.getMessage() + "\"}");
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