package com.example;

import java.io.BufferedReader;
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
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

@WebServlet("/DeletePostServlet")
public class DeletePostServlet extends HttpServlet {
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

        String loggedInUsername = (String) session.getAttribute("username");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String type = null;
        int id = 0;
        try {
            JsonObject jsonObject = gson.fromJson(sb.toString(), JsonObject.class);
            if (jsonObject.has("type")) {
                type = jsonObject.get("type").getAsString();
            }
            if (jsonObject.has("id")) {
                id = jsonObject.get("id").getAsInt();
            }
        } catch (JsonSyntaxException | NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.put("success", false);
            jsonResponse.put("message", "無効なリクエストです。");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        if (type == null || id == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            jsonResponse.put("success", false);
            jsonResponse.put("message", "typeまたはidパラメータがありません。");
            response.getWriter().write(gson.toJson(jsonResponse));
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);

            try {
                if ("topic".equals(type)) {
                    // 验证用户是否是该主题帖的所有者
                    String checkTopicOwnerSql = "SELECT userid FROM javatortoisetopic WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(checkTopicOwnerSql)) {
                        stmt.setInt(1, id);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (!rs.next() || !loggedInUsername.equals(rs.getString("userid"))) {
                                throw new SecurityException("権限がありません、またはトピックが存在しません。");
                            }
                        }
                    }

                    // 删除该主题帖下的所有帖子
                    String deletePostsSql = "DELETE FROM javatortoisepost WHERE topicid = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deletePostsSql)) {
                        stmt.setInt(1, id);
                        stmt.executeUpdate();
                    }

                    // 删除主题帖本身
                    String deleteTopicSql = "DELETE FROM javatortoisetopic WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteTopicSql)) {
                        stmt.setInt(1, id);
                        stmt.executeUpdate();
                    }

                } else if ("reply".equals(type)) {
                    // 验证用户是否是该回复的所有者
                    String checkReplyOwnerSql = "SELECT userid FROM javatortoisepost WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(checkReplyOwnerSql)) {
                        stmt.setInt(1, id);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (!rs.next() || !loggedInUsername.equals(rs.getString("userid"))) {
                                throw new SecurityException("権限がありません、または返信が存在しません。");
                            }
                        }
                    }

                    // 删除该回复
                    String deleteReplySql = "DELETE FROM javatortoisepost WHERE id = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteReplySql)) {
                        stmt.setInt(1, id);
                        stmt.executeUpdate();
                    }
                } else {
                    throw new IllegalArgumentException("指定された投稿タイプが無効です。");
                }

                conn.commit();
                jsonResponse.put("success", true);

            } catch (SQLException | SecurityException | IllegalArgumentException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException | SecurityException | IllegalArgumentException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("success", false);
            jsonResponse.put("message", "データベース操作に失敗しました: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonResponse.put("success", false);
            jsonResponse.put("message", "予期せぬエラーが発生しました。");
        }

        response.getWriter().write(gson.toJson(jsonResponse));
    }
}