<?php
header('Content-Type: application/json');

$db_host = 'localhost';
$db_user = 'root';
$db_pass = '';
$db_name = 'test';

$conn = new mysqli($db_host, $db_user, $db_pass, $db_name);
$conn->set_charset("utf8");

if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
    echo json_encode(['success' => false, 'error' => '数据库连接失败: ' . $conn->connect_error]);
    exit();
}

// 排序图片，按用户ID升序，最早的会优先显示
$sql = "SELECT userid, filepath FROM tortoisepicture ORDER BY id ASC";
$result = $conn->query($sql);

$images = [];
if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
        $images[] = $row;
    }
}

$conn->close();

echo json_encode(['success' => true, 'images' => $images]);
?>