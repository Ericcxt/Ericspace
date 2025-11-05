<?php
session_start();

header('Content-Type: application/json');

if (!isset($_SESSION['username'])) {
    echo json_encode(['status' => 'error', 'message' => 'ログインしていません。']);
    exit();
}

$username = $_SESSION['username'];

// 数据库连接配置
$servername = "localhost";
$db_username = "root";
$db_password = "";
$dbname = "test";

$conn = new mysqli($servername, $db_username, $db_password, $dbname);
$conn->set_charset("utf8");

// 检查连接
if ($conn->connect_error) {
    error_log("データベース接続エラー: " . $conn->connect_error);
    die(json_encode(['status' => 'error', 'message' => 'データベース接続に失敗しました。']));
}

$sql = "SELECT email, country, profilepic FROM tortoiseuser WHERE username = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $username);

$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    $user = $result->fetch_assoc();
    echo json_encode([
        'status' => 'success',
        'username' => $username,
        'email' => $user['email'],
        'country' => $user['country'],
        'profilepic' => $user['profilepic']
    ]);
} else {
    echo json_encode(['status' => 'error', 'message' => 'ユーザーが見つかりません。']);
}

$stmt->close();
$conn->close();
?>