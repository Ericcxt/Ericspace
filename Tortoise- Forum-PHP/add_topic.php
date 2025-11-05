<?php
session_start();

header('Content-Type: application/json');

// 检查用户是否登录
if (!isset($_SESSION['username'])) {
    echo json_encode(['status' => 'error', 'message' => '用户未登录']);
    exit;
}

// 检查标题和内容是否已提供
if (!isset($_POST['title']) || !isset($_POST['content'])) {
    echo json_encode(['status' => 'error', 'message' => '标题和内容为必填项']);
    exit;
}

$title = $_POST['title'];
$content = $_POST['content'];
$username = $_SESSION['username'];

// --- 数据库连接 ---
$servername = "localhost";
$db_username = "root";
$db_password = ""; // 如果您设置了数据库密码，请填写
$dbname = "test";

$conn = new mysqli($servername, $db_username, $db_password, $dbname);
$conn->set_charset("utf8");

// 检查连接
if ($conn->connect_error) {
    echo json_encode(['status' => 'error', 'message' => '数据库连接失败: ' . $conn->connect_error]);
    exit;
}

// --- 事务开始 ---
$conn->begin_transaction();

try {
    // 在 tortoisetopic 中插入数据
    $stmt_topic = $conn->prepare("INSERT INTO tortoisetopic (title, userid, createtime, lastreplytime) VALUES (?, ?, NOW(), NOW())");
    $stmt_topic->bind_param("ss", $title, $username);
    $stmt_topic->execute();

    // 获取新主题的 ID
    $new_topic_id = $conn->insert_id;

    // 在 tortoisepost 中插入第一条帖子
    $stmt_post = $conn->prepare("INSERT INTO tortoisepost (topicid, userid, content, createtime) VALUES (?, ?, ?, NOW())");
    $stmt_post->bind_param("iss", $new_topic_id, $username, $content);
    $stmt_post->execute();

    // 如果两个查询都成功，则提交事务
    $conn->commit();

    echo json_encode(['status' => 'success', 'message' => '主题创建成功', 'topic_id' => $new_topic_id]);

} catch (mysqli_sql_exception $exception) {
    // 如果有任何查询失败，则回滚事务
    $conn->rollback();
    echo json_encode(['status' => 'error', 'message' => '创建主题失败: ' . $exception->getMessage()]);
}

// --- 清理 ---
$stmt_topic->close();
$stmt_post->close();
$conn->close();

?>