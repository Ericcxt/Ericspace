<?php
session_start();
header('Content-Type: application/json');

if (!isset($_SESSION['username'])) { // 修改: 检查 'username'
    echo json_encode(['error' => 'ログインしていません。']);
    exit;
}

$username = $_SESSION['username']; // 修改: 使用 'username'

$servername = "localhost";
$username_db = "root"; // 避免变量名冲突
$password = "";
$dbname = "test";

// データベース接続
$conn = new mysqli($servername, $username_db, $password, $dbname);

if ($conn->connect_error) {
    echo json_encode(['error' => 'データベース接続に失敗しました。: ' . $conn->connect_error]);
    exit;
}

$sql = "SELECT id, filepath FROM tortoisepicture WHERE userid = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $username); // 修改: 使用 $username
$stmt->execute();
$result = $stmt->get_result();

$pictures = [];
while ($row = $result->fetch_assoc()) {
    $pictures[] = $row;
}

echo json_encode($pictures);

$stmt->close();
$conn->close();
?>