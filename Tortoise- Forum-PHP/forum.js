$(document).ready(function() {
    $.ajax({
        url: 'check_session.php',
        type: 'get',
        dataType: 'json',
        success: function(data) {
            if (data.status === 'loggedin') {
                // 如果已登录，显示欢迎信息、My Page按钮和注销按钮
                // --- 修改：为span和按钮之间添加了间距 ---
                $('.user-actions').html(
                    '<span style="color: white; margin-right: 15px; vertical-align: middle;">Welcome, ' + data.username + '</span>' +
                    '<input type="button" value="マイページ" onclick="location.href=\'TortoiseForumMypage.html\'" style="margin-left: 10px;">' +
                    '<input type="button" value="ログアウト" id="logout-button" style="margin-left: 10px;">'
                );
            } else {
                // 如果未登录，动态生成登录和注册按钮
                var filename = window.location.pathname.split('/').pop();
                if (filename === '' || filename === 'index.html') {
                    filename = 'TortoiseForum.html';
                }
                var redirectTarget = filename + window.location.search;
                var loginUrl = 'TortoiseForumLog.html?redirect=' + encodeURIComponent(redirectTarget);
                var registerUrl = 'TortoiseForumSign.html';
                // --- 修改：为两个按钮之间添加了间距 ---
                $('.user-actions').html(
                    '<input type="button" value="ログイン" onclick="location.href=\'' + loginUrl + '\'">' +
                    '<input type="button" value="新規登録" onclick="location.href=\'' + registerUrl + '\'" style="margin-left: 10px;">'
                );
            }
        },
        error: function(xhr, status, error) { // 处理检查会话失败的情况，显示错误内容
            console.error("An error occurred: " + status + " " + error);
        }
    });

    // log out的事件处理程序
    $(document).on('click', '#logout-button', function(e) {
        e.preventDefault();
        $.ajax({
            url: 'logout.php',
            type: 'get',
            dataType: 'json',
            success: function(response) {
                if (response.status === 'loggedout') {
                    location.reload();
                }
            },
            error: function(xhr, status, error) { // 处理注销失败的情况，显示错误内容
                console.error("Logout failed: " + status + " " + error);
                location.reload();
            }
        });
    });
});