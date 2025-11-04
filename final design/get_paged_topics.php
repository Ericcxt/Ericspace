<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);
header('Content-Type: application/json');

// --- 数据库连接 ---
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "test";
$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    echo json_encode(['status' => 'error', 'message' => '数据库连接失败: ' . $conn->connect_error]);
    exit;
}

// --- 分页参数 ---
$page = isset($_GET['page']) ? (int)$_GET['page'] : 1; // 获取当前页码，默认第一页
$limit = 10; // 每页显示10条
$offset = ($page - 1) * $limit; // 计算需要跳过多少记录

// --- 排序参数 ---
$sort_by = isset($_GET['sort_by']) && $_GET['sort_by'] === 'popularity' ? 'popularity' : 'time';

// --- 查询总数 ---
$total_sql = "SELECT COUNT(*) as total FROM tortoisetopic";
$total_result = $conn->query($total_sql);
$total_row = $total_result->fetch_assoc();
$total_topics = $total_row['total'];
$total_pages = ceil($total_topics / $limit); // 计算总页数

// --- 根据排序参数确定 ORDER BY 子句 ---
$order_by_clause = 'last_post_time_sort DESC'; // 默认按时间
if ($sort_by === 'popularity') {
    // 当按热度排序时，将最新活动时间作为次要排序条件
    $order_by_clause = 'views DESC, last_post_time_sort DESC';
}

// --- 查询当前页数据 ---
$sql = "
    SELECT 
        t.id, 
        t.title, 
        t.userid AS author,
        t.createtime,
        t.views,
        (SELECT COUNT(*) FROM tortoisepost WHERE topicid = t.id) AS replies,
        COALESCE((SELECT MAX(createtime) FROM tortoisepost WHERE topicid = t.id), t.createtime) AS last_post_time_sort,
        (SELECT userid FROM tortoisepost WHERE topicid = t.id ORDER BY createtime DESC LIMIT 1) AS last_poster,
        (SELECT MAX(createtime) FROM tortoisepost WHERE topicid = t.id) AS last_post_time
    FROM 
        tortoisetopic t
    ORDER BY 
        $order_by_clause
    LIMIT ? OFFSET ?
";

$stmt = $conn->prepare($sql);
$stmt->bind_param("ii", $limit, $offset);
$stmt->execute();
$result = $stmt->get_result();

$topics = [];
if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $topics[] = $row;
    }
}

echo json_encode([
    'status' => 'success',
    'total_pages' => $total_pages,
    'current_page' => $page,
    'topics' => $topics
]);

$stmt->close();
$conn->close();
?>