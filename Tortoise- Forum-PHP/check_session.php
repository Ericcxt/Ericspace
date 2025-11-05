<?php
session_start();

header('Content-Type: application/json'); // 确保返回的是 JSON 格式

if (isset($_SESSION['username'])) { // 检查用户名是否存在
    // 用户已经登录，返回用户名
    echo json_encode([
        'status' => 'loggedin',
        'username' => $_SESSION['username']
    ]);
} else {
    // 用户未登录，返回未登录状态
    echo json_encode([
        'status' => 'loggedout'
    ]);
}
?>