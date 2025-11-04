<?php
session_start();
header('Content-Type: application/json');

// 检查用户是否已登录
if (!isset($_SESSION['username'])) {
    echo json_encode(['status' => 'error', 'message' => 'User not logged in.']);
    exit;
}

$username = $_SESSION['username'];

// --- 数据库连接 ---
$servername = "localhost";
$db_username = "root";
$db_password = "";
$dbname = "test";

$conn = new mysqli($servername, $db_username, $db_password, $dbname);

if ($conn->connect_error) {
    echo json_encode(['status' => 'error', 'message' => 'Database connection failed: ' . $conn->connect_error]);
    exit;
}

$response = ['status' => 'success', 'posts' => []];

try {
    // 1. 获取用户创建的所有主题（作为帖子）
    // 我们需要连接 tortoisetopic 和 tortoisepost 来获取主题的标题和内容
    $sql_topics = "
        SELECT 
            t.id AS topic_id,
            p.id AS post_id,
            t.title,
            p.content,
            p.createtime,
            'topic' AS type
        FROM tortoisetopic t
        JOIN tortoisepost p ON t.id = p.topicid
        WHERE t.userid = ? AND p.id = (
            -- 仅获取每个主题的第一个帖子，即主题帖本身
            SELECT MIN(id) FROM tortoisepost WHERE topicid = t.id
        )
    ";

    $stmt_topics = $conn->prepare($sql_topics);
    $stmt_topics->bind_param("s", $username);
    $stmt_topics->execute();
    $result_topics = $stmt_topics->get_result();

    while ($row = $result_topics->fetch_assoc()) {
        $response['posts'][] = $row;
    }
    $stmt_topics->close();

    // 2. 获取用户的所有回复
    // 我们需要排除掉每个主题的第一个帖子，因为它们是主题帖，已经在上面获取过了
    $sql_replies = "
        SELECT 
            p.topicid AS topic_id,
            p.id AS post_id,
            t.title,
            p.content,
            p.createtime,
            'reply' AS type
        FROM tortoisepost p
        JOIN tortoisetopic t ON p.topicid = t.id
        WHERE p.userid = ? AND p.id NOT IN (
            -- 排除每个主题的第一个帖子
            SELECT MIN(id) FROM tortoisepost GROUP BY topicid
        )
    ";

    $stmt_replies = $conn->prepare($sql_replies);
    $stmt_replies->bind_param("s", $username);
    $stmt_replies->execute();
    $result_replies = $stmt_replies->get_result();

    while ($row = $result_replies->fetch_assoc()) {
        $response['posts'][] = $row;
    }
    $stmt_replies->close();

    // 按创建时间降序排序所有帖子和回复
    usort($response['posts'], function($a, $b) {
        return strtotime($b['createtime']) - strtotime($a['createtime']);
    });

    echo json_encode($response);

} catch (Exception $e) {
    echo json_encode(['status' => 'error', 'message' => 'An error occurred: ' . $e->getMessage()]);
}

$conn->close();
?>