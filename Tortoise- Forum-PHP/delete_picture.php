<?php
session_start();
header('Content-Type: application/json');

// 检查用户是否已登录（使用 'username'）
if (!isset($_SESSION['username'])) {
    echo json_encode(['success' => false, 'message' => 'ログインしていません。']);
    exit;
}

if (!isset($_POST['id'])) {
    echo json_encode(['success' => false, 'message' => '画像IDがありません。']);
    exit;
}

// 使用 'username' 作为用户ID
$userId = $_SESSION['username'];
$pictureId = $_POST['id'];

$servername = "localhost";
$username_db = "root"; 
$password = "";
$dbname = "test";

// 数据库连接
$conn = new mysqli($servername, $username_db, $password, $dbname);
$conn->set_charset("utf8");

// 检查连接
if ($conn->connect_error) {
    echo json_encode(['success' => false, 'message' => 'データベース接続に失敗しました。: ' . $conn->connect_error]);
    exit;
}

$conn->begin_transaction();

try {
    // 确认是用户拥有的图片，并获取文件路径
    $sql = "SELECT filepath FROM tortoisepicture WHERE id = ? AND userid = ?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("is", $pictureId, $userId);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows === 0) {
        throw new Exception('画像が見つからないか、削除する権限がありません。');
    }
    
    $row = $result->fetch_assoc();
    $filepath = $row['filepath'];

    // 从数据库中删除记录
    $sql = "DELETE FROM tortoisepicture WHERE id = ?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $pictureId);
    $stmt->execute();

    if ($stmt->affected_rows > 0) {
        // 1. 提交事务 2. 删除文件 3. 发送响应
        $conn->commit();
        
        if (file_exists($filepath)) {
            unlink($filepath);
        }
        
        echo json_encode(['success' => true, 'message' => '画像が正常に削除されました。']);
    } else {
        throw new Exception('データベースからの画像の削除に失敗しました。');
    }

} catch (Exception $e) {
    $conn->rollback();
    echo json_encode(['success' => false, 'message' => $e->getMessage()]);
}

$stmt->close();
$conn->close();
?>