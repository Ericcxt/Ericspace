package com.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import javax.servlet.annotation.WebServlet; // 确保导入

@WebServlet("/GetImagesServlet") // <--- 添加此行
public class GetImagesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // 数据库连接信息 (与之前保持一致)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        List<Map<String, String>> images = new ArrayList<>();
        Map<String, Object> responseData = new HashMap<>();
        Connection conn = null;

        try {
            // 加载数据库驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // 从新的 javatortoisepicture 表中查询数据
            String sql = "SELECT userid, filepath FROM javatortoisepicture ORDER BY id ASC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> image = new HashMap<>();
                image.put("userid", rs.getString("userid"));
                image.put("filepath", rs.getString("filepath"));
                images.add(image);
            }

            rs.close();
            stmt.close();

            responseData.put("success", true);
            responseData.put("images", images);

        } catch (ClassNotFoundException e) {
            responseData.put("success", false);
            responseData.put("error", "数据库驱动加载失败: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (SQLException e) {
            responseData.put("success", false);
            responseData.put("error", "数据库操作异常: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // 忽略关闭连接时的错误
                }
            }
        }

        // 使用 Gson 将 Map 转换为 JSON 字符串并发送
        PrintWriter out = response.getWriter();
        out.print(new Gson().toJson(responseData));
        out.flush();
    }
}