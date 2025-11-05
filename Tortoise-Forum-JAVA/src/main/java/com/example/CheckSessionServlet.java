package com.example;

// 全部使用 javax.* 包
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/check_session")
public class CheckSessionServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false); // false表示不创建新session
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (session != null && session.getAttribute("username") != null) {
            // 用户已登录
            String username = (String) session.getAttribute("username");
            // 构建 JSON 字符串
            String json = "{\"status\": \"loggedin\", \"username\": \"" + username + "\"}";
            response.getWriter().write(json);
        } else {
            // 用户未登录
            response.getWriter().write("{\"status\": \"loggedout\"}");
        }
    }
}