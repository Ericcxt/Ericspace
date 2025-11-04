<?php
session_start();
header('Content-Type: application/json');

// 检查用户是否已登录
if (!isset($_SESSION['username'])) {
    echo json_encode(['status' => 'error', 'message' => 'ログインしていません。再度ログインしてください。']);
    exit;
}

// 数据库连接
$conn = new mysqli('localhost', 'root', '', 'test');
if ($conn->connect_error) {
    echo json_encode(['status' => 'error', 'message' => 'データベース接続に失敗しました。']);
    exit;
}

// 获取POST数据
$currentPassword = $_POST['current_password'] ?? '';
$newPassword = $_POST['new_password'] ?? '';
$confirmPassword = $_POST['confirm_new_password'] ?? '';
$username = $_SESSION['username'];

// 验证输入是否为空
if (empty($currentPassword) || empty($newPassword) || empty($confirmPassword)) {
    echo json_encode(['status' => 'error', 'message' => 'すべてのパスワードフィールドは必須です。']);
    exit;
}

// 验证新密码和确认密码是否一致
if ($newPassword !== $confirmPassword) {
    echo json_encode(['status' => 'error', 'message' => '新しいパスワードと確認用パスワードが一致しません。']);
    exit;
}

// 从数据库获取当前用户的密码
$stmt = $conn->prepare("SELECT password FROM tortoiseuser WHERE username = ?");
$stmt->bind_param('s', $username);
$stmt->execute();
$result = $stmt->get_result();
$user = $result->fetch_assoc();
$stmt->close();

if (!$user) {
    echo json_encode(['status' => 'error', 'message' => 'ユーザーが見つかりません。']);
    $conn->close();
    exit;
}

// --- 核心安全升级 (1/2): 使用 password_verify 验证当前密码 ---
// $user['password'] 现在是数据库里存储的哈希值
if (!password_verify($currentPassword, $user['password'])) {
    echo json_encode(['status' => 'error', 'message' => '現在のパスワードが正しくありません。']);
    $conn->close();
    exit;
}

// --- 核心安全升级 (2/2): 哈希新密码后再存入数据库 ---
$newPasswordHash = password_hash($newPassword, PASSWORD_DEFAULT);

// 更新数据库中的密码
$stmt = $conn->prepare("UPDATE tortoiseuser SET password = ? WHERE username = ?");
$stmt->bind_param('ss', $newPasswordHash, $username);

if ($stmt->execute()) {
    echo json_encode(['status' => 'success', 'message' => 'パスワードが正常に更新されました。']);
} else {
    echo json_encode(['status' => 'error', 'message' => 'パスワードの更新に失敗しました。']);
}

$stmt->close();
$conn->close();
?>