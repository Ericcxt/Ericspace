<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json');

// 数据库连接
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "test";
$conn = new mysqli($servername, $username, $password, $dbname);
$conn->set_charset("utf8");

if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
    echo json_encode(['status' => 'redirect', 'location' => 'TortoiseForumTopic.html']);
    exit();
}

// 讨论详情页，获取帖子ID
$topic_id = isset($_GET['topic_id']) ? (int)$_GET['topic_id'] : 0;

if ($topic_id <= 0) {
    echo json_encode(['status' => 'redirect', 'location' => 'TortoiseForumTopic.html']);
    exit();
}

// 讨论详情页，更新帖子访问量
$update_views_sql = "UPDATE tortoisetopic SET views = views + 1 WHERE id = ?";
$stmt = $conn->prepare($update_views_sql);
if ($stmt === false) {
    echo json_encode(['status' => 'redirect', 'location' => 'TortoiseForumTopic.html']);
    exit();
}
$stmt->bind_param("i", $topic_id);
$stmt->execute();
$stmt->close();

// 讨论详情页，获取帖子标题
$topic_sql = "SELECT title FROM tortoisetopic WHERE id = ?";
$stmt = $conn->prepare($topic_sql);
if ($stmt === false) {
    echo json_encode(['status' => 'redirect', 'location' => 'TortoiseForumTopic.html']);
    exit();
}
$stmt->bind_param("i", $topic_id);
$stmt->execute();
$topic_result = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$topic_result) {
    echo json_encode(['status' => 'redirect', 'location' => 'TortoiseForumTopic.html']);
    exit();
}

// 讨论详情页，获取帖子内容，包括作者一开始的帖子
$posts_sql = "SELECT p.content, p.userid AS author, p.createtime, u.profilepic FROM tortoisepost p LEFT JOIN tortoiseuser u ON p.userid = u.username WHERE p.topicid = ? ORDER BY p.createtime ASC";
$stmt = $conn->prepare($posts_sql);
if ($stmt === false) {
    // 这种情况通常表示SQL语法错误，也引导用户离开
    echo json_encode(['status' => 'redirect', 'location' => 'TortoiseForumTopic.html']);
    exit();
}
$stmt->bind_param("i", $topic_id);
$stmt->execute();
$all_posts = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

// 讨论详情页，构建返回数据结构
if (empty($all_posts)) {
    // 讨论详情页，处理没有帖子的情况
    $response = [
        'topic' => [
            'title' => $topic_result['title'],
            'author' => 'N/A',
            'createtime' => 'N/A',
            'content' => 'No content found for this topic.'
        ],
        'replies' => []
    ];
} else {
    // 讨论详情页，获取帖子内容，第一个元素是主楼
    $main_post = array_shift($all_posts);
    
    // 剩下的就是回复
    $replies = $all_posts;

    $topic_data = [
        'title' => $topic_result['title'],
        'author' => $main_post['author'],
        'createtime' => $main_post['createtime'],
        'content' => $main_post['content'],
        'profilepic' => $main_post['profilepic']
    ];

    $response = [
        'topic' => $topic_data,
        'replies' => $replies
    ];
}

echo json_encode($response);

$conn->close();
?>