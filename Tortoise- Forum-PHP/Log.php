<?php
session_start();

// 初始化一个用于存储返回结果的数组
$response = array(
    'status' => 'error',
    'message' => 'ログイン中にエラーが発生しました。' // 默认通用错误信息
);

// 检查是否收到了 POST 请求数据
if (isset($_POST["username"]) && isset($_POST["password"])) {
    $username = $_POST["username"];
    $password = $_POST["password"];

    // 连接数据库
    $link = mysqli_connect('localhost', 'root', '', 'test');
    if ($link) {
        mysqli_set_charset($link, "utf8");

        // 准备 SQL 查询，只根据用户名查询用户信息
        $sql = "SELECT username, password FROM tortoiseuser WHERE username = ?";
        $stmt = mysqli_prepare($link, $sql);
        
        if ($stmt) {
            mysqli_stmt_bind_param($stmt, "s", $username);
            mysqli_stmt_execute($stmt);
            mysqli_stmt_store_result($stmt);

            // 检查是否找到了用户
            if (mysqli_stmt_num_rows($stmt) === 1) {
                // 绑定结果变量以获取哈希密码
                mysqli_stmt_bind_result($stmt, $found_username, $db_password_hash);
                mysqli_stmt_fetch($stmt);
                
                // 使用 password_verify() 验证密码
                if (password_verify($password, $db_password_hash)) {
                    // 密码正确，登录成功
                    $_SESSION['username'] = $found_username;
                    $response['status'] = 'success';
                    $response['message'] = 'ログインに成功しました！';
                } else {
                    // 密码错误
                    $response['message'] = 'ユーザー名またはパスワードが一致しません。';
                }
            } else {
                // 用户不存在
                $response['message'] = 'ユーザー名またはパスワードが一致しません。';
            }
            mysqli_stmt_close($stmt);
        }
        // 如果 $stmt 创建失败，将使用默认的通用错误信息
        
        mysqli_close($link);
    }
    // 如果数据库连接失败，将使用默认的通用错误信息
}
// 如果未收到 POST 数据，将使用默认的通用错误信息

// 总是返回 JSON
header('Content-Type: application/json');
echo json_encode($response);
?>