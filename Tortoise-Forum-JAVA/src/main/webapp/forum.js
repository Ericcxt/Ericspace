$(document).ready(function() {
    function checkSession() {
        $.ajax({
            url: '/tortoise-java/check_session',
            type: 'GET',
            dataType: 'json',
            success: function(response) {
                if (response.status === 'loggedin') {
                    // 已登录：显示欢迎信息、My Page 和注销按钮 (修正版)
                    $('.user-actions').html(
                        '<span style="color: white; margin-right: 15px; vertical-align: middle;">Welcome, ' + response.username + '</span>' +
                        '<input type="button" value="マイページ" onclick="location.href=\'TortoiseForumMypage.html\'" style="margin-left: 10px;">' +
                        '<input type="button" value="ログアウト" id="logout-button" style="margin-left: 10px;">'
                    );
                } else {
                    // --- 最终修正案 ---
                    // 1. 获取文件名，并处理根路径的特殊情况
                    var filename = window.location.pathname.split('/').pop();
                    if (filename === '' || filename === 'index.html') {
                        filename = 'TortoiseForum.html';
                    }
                    
                    // 2.【核心修正】将文件名和 URL 查询参数拼接起来
                    var redirectTarget = filename + window.location.search;
                    
                    // 3. 构建包含完整重定向目标的 URL
                    var loginUrl = 'TortoiseForumLog.html?redirect=' + encodeURIComponent(redirectTarget);
                    var registerUrl = 'TortoiseForumSign.html';

                    // 4.【保持正确语法】使用您文件中已有的、正确的 \' 转义方式
                    $('.user-actions').html(
                        '<input type="button" value="ログイン" onclick="location.href=\'' + loginUrl + '\'">' +
                        '<input type="button" value="新規登録" onclick="location.href=\'' + registerUrl + '\'" style="margin-left: 10px;">'
                    );
                }
            },
            error: function(xhr, status, error) {
                console.error("check_session AJAX Error:", status, error, xhr.responseText);
                // 即使出错，也回退显示登录/注册按钮
                var loginUrl = 'TortoiseForumLog.html';
                var registerUrl = 'TortoiseForumSign.html';
                $('.user-actions').html(
                    '<input type="button" value="ログイン" onclick="location.href=\'' + loginUrl + '\'">' +
                    '<input type="button" value="新規登録" onclick="location.href=\'' + registerUrl + '\'" style="margin-left: 10px;">'
                );
            }
        });
    }

    checkSession();

    // 为注销按钮绑定事件 (与 PHP 版本体验一致)
    $(document).on('click', '#logout-button', function(e) {
        e.preventDefault();
        
        $.ajax({
            url: '/tortoise-java/LogoutServlet', // 指向我们新的 Servlet
            type: 'GET',
            dataType: 'json',
            success: function(response) {
                // 不论成功与否，都直接刷新页面
                // 后端的 session 已经销毁，刷新后 checkSession() 会自动更新 UI
                location.reload();
            },
            error: function(xhr, status, error) {
                console.error("Logout AJAX Error:", status, error, xhr.responseText);
                // 即使 AJAX 请求失败，也尝试刷新页面，让用户看到变化
                location.reload();
            }
        });
    });
});