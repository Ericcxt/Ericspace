<?php
header('Content-Type: application/json');

// --- 数据库连接 ---
$servername = "localhost";
$db_username = "root";
$db_password = ""; 
$dbname = "test";

$conn = new mysqli($servername, $db_username, $db_password, $dbname);
$conn->set_charset("utf8");

// Check connection
if ($conn->connect_error) {
    echo json_encode(['status' => 'error', 'message' => '数据库连接失败: ' . $conn->connect_error]);
    exit;
}

// --- 查询数据 ---
// 获取主题的各种信息，包括：
// t.id, t.title: 主题的ID和标题
// t.userid: 主题创建者的用户名
// t.createtime: 主题创建时间
// (SELECT COUNT(*) ...): 计算该主题下的总回帖数
// (SELECT userid ...): 找出最后一个回帖的用户名
// (SELECT MAX(createtime) ...): 找出最后一个回帖的时间
$sql = "
    SELECT 
        t.id, 
        t.title, 
        t.userid AS author,
        t.createtime,
        t.views,
        (SELECT COUNT(*) FROM tortoisepost WHERE topicid = t.id) AS replies,
        (SELECT userid FROM tortoisepost WHERE topicid = t.id ORDER BY createtime DESC LIMIT 1) AS last_poster,
        (SELECT MAX(createtime) FROM tortoisepost WHERE topicid = t.id) AS last_post_time
    FROM 
        tortoisetopic t
    ORDER BY 
        t.createtime DESC
    LIMIT 6
";

$result = $conn->query($sql);

$topics = [];
if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $topics[] = $row;
    }
}

echo json_encode(['status' => 'success', 'topics' => $topics]);

$conn->close();
?>