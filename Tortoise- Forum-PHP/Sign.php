<?php
// 响应头设置为 JSON
header('Content-Type: application/json');

// 允许跨域请求（在开发阶段）
// 生产环境中，请根据实际需求配置允许的域名
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With');

// 数据库连接信息
$db_host = 'localhost';
$db_user = 'root';
$db_pass = '';
$db_name = 'test';

// 创建数据库连接
$conn = new mysqli($db_host, $db_user, $db_pass, $db_name);
$conn->set_charset("utf8");

if ($conn->connect_error) {
    die(json_encode(['success' => false, 'message' => '数据库连接失败: ' . $conn->connect_error]));
    echo json_encode(['status' => 'error', 'message' => '登録中にエラーが発生しました。']);
    exit();
}

// 检查请求方法是否为 POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['status' => 'error', 'message' => '登録中にエラーが発生しました。']);
    exit();
}

// 获取并清理表单数据
$username = trim($_POST['username'] ?? ''); //trim()函数用于移除字符串两侧的空白字符或其他预定义字符
$email = trim($_POST['email'] ?? '');
$country = trim($_POST['country'] ?? '');
$password = $_POST['password'] ?? '';
$confirm_password = $_POST['confirm-password'] ?? '';

// 验证密码是否一致
if ($password !== $confirm_password) {
    echo json_encode(['status' => 'error', 'message' => '入力されたパスワードが一致しません。']);
    exit();
}

// 检查用户名是否已存在
$stmt = $conn->prepare('SELECT username FROM tortoiseuser WHERE username = ?'); // 准备查询模版
$stmt->bind_param('s', $username); // 绑定参数
$stmt->execute(); // 执行查询
$stmt->store_result(); // 存储结果集
if ($stmt->num_rows > 0) {
    echo json_encode(['status' => 'error', 'message' => 'このユーザー名はすでに登録されています。']);
    $stmt->close();
    exit();
}
$stmt->close();

// 检查邮箱是否已存在
$stmt = $conn->prepare('SELECT email FROM tortoiseuser WHERE email = ?');
$stmt->bind_param('s', $email);
$stmt->execute();
$stmt->store_result();
if ($stmt->num_rows > 0) {
    echo json_encode(['status' => 'error', 'message' => 'このメールアドレスはすでに登録されています。']);
    $stmt->close();
    exit();
}
$stmt->close();

// 处理文件上传
$profilepic_path = '';
if (isset($_FILES['profilepic']) && $_FILES['profilepic']['error'] === UPLOAD_ERR_OK) {
    $upload_dir = 'avatar/';
    if (!is_dir($upload_dir)) {
        mkdir($upload_dir, 0777, true);
    }
    $file_extension = pathinfo($_FILES['profilepic']['name'], PATHINFO_EXTENSION);
    $profilepic_filename = uniqid('profilepic_', true) . '.' . $file_extension;
    $profilepic_path = $upload_dir . $profilepic_filename;

    if (!move_uploaded_file($_FILES['profilepic']['tmp_name'], $profilepic_path)) {
        echo json_encode(['status' => 'error', 'message' => '登録中にエラーが発生しました。']);
        exit();
    }
}

// 在插入数据库前对密码进行哈希处理
$hashed_password = password_hash($password, PASSWORD_DEFAULT);

// 将新用户信息插入数据库
$stmt = $conn->prepare('INSERT INTO tortoiseuser (username, email, country, password, profilepic) VALUES (?, ?, ?, ?, ?)');
// 绑定的是哈希后的密码
$stmt->bind_param('sssss', $username, $email, $country, $hashed_password, $profilepic_path);

if ($stmt->execute()) {
    echo json_encode(['status' => 'success', 'message' => '登録に成功しました！']);
} else {
    echo json_encode(['status' => 'error', 'message' => '登録中にエラーが発生しました。']);
}

// 关闭数据库连接
$stmt->close();
$conn->close();
?>