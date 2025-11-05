<?php
// 开启输出缓冲，这必须是脚本的第一件事
ob_start();

// 强制显示所有 PHP 错误，方便调试
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// 开启 session
session_start();

// 设置响应头为 JSON
header('Content-Type: application/json');

// 初始化响应数组
$response = [];

try {
    // --- 检查用户登录状态 ---
    if (!isset($_SESSION['username'])) {
        throw new Exception('用户未登录，禁止上传。');
    }

    // --- 检查文件上传 ---
    if (!isset($_FILES['file']) || $_FILES['file']['error'] !== UPLOAD_ERR_OK) {
        $error_message = '文件上传失败。';
        if (isset($_FILES['file']['error'])) {
            switch ($_FILES['file']['error']) {
                case UPLOAD_ERR_INI_SIZE:
                    $error_message .= ' 文件大小超过了 php.ini 的限制。';
                    break;
                case UPLOAD_ERR_FORM_SIZE:
                    $error_message .= ' 文件大小超过了 HTML 表单的限制。';
                    break;
                case UPLOAD_ERR_PARTIAL:
                    $error_message .= ' 文件只有部分被上传。';
                    break;
                case UPLOAD_ERR_NO_FILE:
                    $error_message .= ' 没有文件被上传。';
                    break;
                case UPLOAD_ERR_NO_TMP_DIR:
                    $error_message .= ' 找不到临时文件夹。';
                    break;
                case UPLOAD_ERR_CANT_WRITE:
                    $error_message .= ' 文件写入失败。';
                    break;
                default:
                    $error_message .= ' 发生未知错误。';
                    break;
            }
        }
        throw new Exception($error_message);
    }

    // --- 处理文件存储 ---
    $upload_dir = 'uploads/';
    if (!file_exists($upload_dir)) {
        if (!mkdir($upload_dir, 0777, true)) { // 0777 权限设置，确保目录可写，创建目录
            throw new Exception('创建上传目录失败，请检查服务器权限。');
        }
    }

    $file_name = basename($_FILES['file']['name']);
    $file_ext = strtolower(pathinfo($file_name, PATHINFO_EXTENSION));
    $unique_file_name = uniqid() . '.' . $file_ext;
    $target_path = $upload_dir . $unique_file_name;

    $allowed_types = ['jpg', 'jpeg', 'png', 'gif'];
    if (!in_array($file_ext, $allowed_types)) {
        throw new Exception('无效的图片格式。只允许 JPG, JPEG, PNG, GIF。');
    }

    if (!move_uploaded_file($_FILES['file']['tmp_name'], $target_path)) { // 移动上传文件
        throw new Exception('保存上传文件失败。');
    }

    // --- 将图片信息存入数据库 ---
    $db_host = 'localhost';
    $db_user = 'root';
    $db_pass = '';
    $db_name = 'test'; // 

    $conn = new mysqli($db_host, $db_user, $db_pass, $db_name);
    $conn->set_charset("utf8");

    if ($conn->connect_error) {
        die("Connection failed: " . $conn->connect_error);
        // 在生产环境中，不应暴露详细的连接错误
        throw new Exception('数据库连接失败，请联系管理员。');
    }

    $username = $_SESSION['username'];
    $filepath = $target_path; 

    $stmt = $conn->prepare("INSERT INTO tortoisepicture (userid, filepath) VALUES (?, ?)");
    if ($stmt === false) {
        // 准备语句失败，很可能是 SQL 语法错误或表/列名不匹配
        throw new Exception('数据库准备语句失败: ' . $conn->error);
    }
    
    // 'ss' 表示两个参数都是字符串类型
    $stmt->bind_param("ss", $username, $filepath);

    if (!$stmt->execute()) {
        // 执行失败
        throw new Exception('数据库插入失败: ' . $stmt->error);
    }

    $stmt->close();
    $conn->close();

    // 如果一切顺利，设置成功响应
    $response = ['success' => true, 'message' => '文件上传成功并已记录到数据库。', 'filepath' => $filepath];

} catch (Exception $e) {
    // 捕获任何异常，并设置错误响应
    // http_response_code(500); // 可选：设置 HTTP 状态码为 500
    $response = ['success' => false, 'error' => $e->getMessage()];
}

// 清理之前的所有输出（包括可能的 PHP 警告/错误）
ob_clean();

// 输出最终的 JSON 响应
echo json_encode($response);

// 发送所有输出到浏览器
ob_end_flush();

?>