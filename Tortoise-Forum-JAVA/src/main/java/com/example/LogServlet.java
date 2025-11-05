package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/log")
public class LogServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String requestBody = sb.toString();
        
        Gson gson = new Gson();
        JsonObject requestData = gson.fromJson(requestBody, JsonObject.class);
        
        String username = requestData.get("username").getAsString();
        String password = requestData.get("password").getAsString();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC", "root", "");

            String sql = "SELECT username, password FROM javatortoiseuser WHERE username = ?"; // <--- 修改这里
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String hashedPasswordFromDB = rs.getString("password");
                
                if (BCrypt.checkpw(password, hashedPasswordFromDB)) {
                    // 密码匹配，登录成功
                    HttpSession session = request.getSession();
                    session.setAttribute("username", rs.getString("username"));
                    
                    jsonResponse.addProperty("status", "success");
                    jsonResponse.addProperty("message", "ログインに成功しました！");
                } else {
                    // 密码不匹配
                    jsonResponse.addProperty("status", "error");
                    jsonResponse.addProperty("message", "ユーザー名またはパスワードが一致しません。");
                }
            } else {
                // 用户名不存在
                jsonResponse.addProperty("status", "error");
                jsonResponse.addProperty("message", "ユーザー名またはパスワードが一致しません。");
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            jsonResponse.addProperty("status", "error");
            jsonResponse.addProperty("message", "サーバーエラーが発生しました。(DB Driver)");
        } catch (SQLException e) {
            e.printStackTrace();
            jsonResponse.addProperty("status", "error");
            jsonResponse.addProperty("message", "サーバーエラーが発生しました。(SQL)");
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            out.print(gson.toJson(jsonResponse));
            out.flush();
        }
    }
}