<?php
session_start();

// 检查登录
if (!isset($_SESSION['username'])) {
    die('Error: User not logged in.');
}

// 确保是 POST 请求
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    die('Error: Invalid request method.');
}

// 获取并验证输入数据
$topic_id = isset($_POST['topic_id']) ? (int)$_POST['topic_id'] : 0;
$content = isset($_POST['content']) ? trim($_POST['content']) : '';
// 使用会话中的 'username' 作为用户ID
$user_id = $_SESSION['username'];

if ($topic_id <= 0 || empty($content)) {
    die('Error: Invalid input. Topic ID and content are required.');
}

// 连接数据库
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "test";

$conn = new mysqli($servername, $username, $password, $dbname);
$conn->set_charset("utf8");

// 检查连接
if ($conn->connect_error) {
    die('Database connection failed: ' . $conn->connect_error);
}

// 开启事务
$conn->begin_transaction();

try {
    // 插入新回复
    $stmt_insert = $conn->prepare("INSERT INTO tortoisepost (topicid, userid, content, createtime) VALUES (?, ?, ?, NOW())");
    $stmt_insert->bind_param("iss", $topic_id, $user_id, $content);
    $stmt_insert->execute();
    $stmt_insert->close();

    // 更新主贴回复时间
    $stmt_update = $conn->prepare("UPDATE tortoisetopic SET lastreplytime = NOW() WHERE id = ?");
    $stmt_update->bind_param("i", $topic_id);
    $stmt_update->execute();
    $stmt_update->close();

    // 提交事务
    $conn->commit();

    // 成功后，重定向回帖子页面
    header("Location: TortoiseForumDiscussion.html?topic_id=" . $topic_id);
    exit();

} catch (Exception $e) {
    $conn->rollback();
    die('An error occurred: ' . $e->getMessage());
}

$conn->close();
?>