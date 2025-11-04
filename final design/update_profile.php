<?php
session_start();
header('Content-Type: application/json');

// 检查用户是否已登录
if (!isset($_SESSION['username'])) {
    echo json_encode(['status' => 'error', 'message' => 'ログインしていません。']);
    exit;
}

// 数据库连接
$conn = new mysqli('localhost', 'root', '', 'test');
if ($conn->connect_error) {
    echo json_encode(['status' => 'error', 'message' => 'データベース接続に失敗しました。']);
    exit;
}

// 获取POST数据和用户名
$email = trim($_POST['email'] ?? '');
$country = trim($_POST['country'] ?? '');
$username = $_SESSION['username'];

$updates = [];
$params = [];
$types = '';
$old_avatar_path = null;
$new_avatar_uploaded = false;

// --- 新增：处理头像上传 ---
if (isset($_FILES['avatar']) && $_FILES['avatar']['error'] === UPLOAD_ERR_OK) {
    // 1. 先获取旧头像的路径，以便后续删除
    $stmt_old = $conn->prepare("SELECT profilepic FROM tortoiseuser WHERE username = ?");
    $stmt_old->bind_param('s', $username);
    $stmt_old->execute();
    $stmt_old->bind_result($old_avatar_path);
    $stmt_old->fetch();
    $stmt_old->close();

    // 2. 处理新上传的图片
    $upload_dir = 'avatar/';
    // 确保目录存在且可写
    if (!is_dir($upload_dir)) {
        mkdir($upload_dir, 0755, true);
    }

    $file_tmp_name = $_FILES['avatar']['tmp_name'];
    $file_name = $_FILES['avatar']['name'];
    $file_ext = strtolower(pathinfo($file_name, PATHINFO_EXTENSION));
    $allowed_ext = ['jpg', 'jpeg', 'png', 'gif'];

    if (in_array($file_ext, $allowed_ext)) {
        // 创建一个唯一的文件名以避免冲突
        $new_file_name = uniqid('', true) . '.' . $file_ext;
        $upload_file_path = $upload_dir . $new_file_name;

        if (move_uploaded_file($file_tmp_name, $upload_file_path)) {
            $updates[] = "profilepic = ?";
            $params[] = $upload_file_path;
            $types .= 's';
            $new_avatar_uploaded = true;
        } else {
            echo json_encode(['status' => 'error', 'message' => 'アップロードされたファイルの移動に失敗しました。']);
            $conn->close();
            exit;
        }
    } else {
        echo json_encode(['status' => 'error', 'message' => '無効なファイルタイプです。JPG、PNG、GIFのみが許可されています。']);
        $conn->close();
        exit;
    }
}

// --- 修改：处理邮箱和国家更新 ---
// 处理邮箱更新
if (!empty($email)) {
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        echo json_encode(['status' => 'error', 'message' => '無効なメール形式です。']);
        exit;
    }
    $stmt = $conn->prepare("SELECT email FROM tortoiseuser WHERE email = ? AND username != ?");
    $stmt->bind_param('ss', $email, $username);
    $stmt->execute();
    $stmt->store_result();
    if ($stmt->num_rows > 0) {
        echo json_encode(['status' => 'error', 'message' => 'このメールアドレスは既に他のユーザーによって登録されています。']);
        $stmt->close();
        $conn->close();
        exit;
    }
    $stmt->close();
    $updates[] = "email = ?";
    $params[] = $email;
    $types .= 's';
}

// 处理国家更新
if (!empty($country)) {
    $updates[] = "country = ?";
    $params[] = $country;
    $types .= 's';
}

// 如果没有任何更新，则提示没有修改
if (empty($updates)) {
    echo json_encode(['status' => 'info', 'message' => '変更は送信されませんでした。']);
    exit;
}

// 准备并执行动态构建的SQL语句
$sql = "UPDATE tortoiseuser SET " . implode(', ', $updates) . " WHERE username = ?";
$params[] = $username;
$types .= 's';

$stmt = $conn->prepare($sql);
$stmt->bind_param($types, ...$params);

if ($stmt->execute()) {
    // --- 新增：如果新头像上传成功，则删除旧头像 ---
    if ($new_avatar_uploaded && !empty($old_avatar_path) && file_exists($old_avatar_path)) {
        unlink($old_avatar_path);
    }
    echo json_encode(['status' => 'success', 'message' => 'プロフィールは正常に更新されました。']);
} else {
    echo json_encode(['status' => 'error', 'message' => 'プロフィールの更新に失敗しました。']);
}

$stmt->close();
$conn->close();
?>