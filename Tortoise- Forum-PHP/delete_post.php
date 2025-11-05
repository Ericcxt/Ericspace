<?php
session_start();

// --- 数据库连接设置 ---
$servername = "localhost";
$username = "root";      // 数据库用户名
$password = "";          // 数据库密码
$dbname = "test";        // 数据库名

// 创建数据库连接
$conn = new mysqli($servername, $username, $password, $dbname);
$conn->set_charset("utf8");

// 检查连接
if ($conn->connect_error) {
    echo json_encode(['success' => false, 'message' => 'データベース接続に失敗しました: ' . $conn->connect_error]);
    exit;
}
// --- 数据库连接结束 ---

header('Content-Type: application/json');

// 1. 检查会话中是否存在 'username'，这是唯一的用户凭证
if (!isset($_SESSION['username'])) {
    echo json_encode(['success' => false, 'message' => 'ログインしていません。']);
    exit;
}

// 2. 直接从会话中获取当前登录用户的 username
$loggedInUsername = $_SESSION['username'];

// 检查请求方法是否为 POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['success' => false, 'message' => '無効なリクエストです。']);
    exit;
}

$data = json_decode(file_get_contents('php://input'), true);
$type = $data['type'] ?? null;
$id = $data['id'] ?? null;

// 检查是否缺少必要的参数
if (!$type || !$id) {
    echo json_encode(['success' => false, 'message' => 'typeまたはidパラメータがありません。']);
    exit;
}

// 开启数据库事务
$conn->begin_transaction();

try {
    // 如果删除的是主题帖
    if ($type === 'topic') {
        $topicId = $id;

        // 3. 验证用户是否是该主题帖的所有者
        $stmt = $conn->prepare("SELECT userid FROM tortoisetopic WHERE id = ?");
        $stmt->bind_param("i", $topicId);
        $stmt->execute();
        $result = $stmt->get_result();
        $topic = $result->fetch_assoc();

        // 比较从帖子中获取的作者名(userid字段)和当前登录的用户名
        if (!$topic || $topic['userid'] !== $loggedInUsername) {
            throw new Exception('権限がありません、またはトピックが存在しません。');
        }

        // 删除该主题帖下的所有回复
        $stmt = $conn->prepare("DELETE FROM tortoisepost WHERE topicid = ?");
        $stmt->bind_param("i", $topicId);
        $stmt->execute();

        // 删除主题帖本身
        $stmt = $conn->prepare("DELETE FROM tortoisetopic WHERE id = ?");
        $stmt->bind_param("i", $topicId);
        $stmt->execute();

    // 如果删除的是回复
    } elseif ($type === 'reply') {
        $replyId = $id;

        // 3. 验证用户是否是该回复的所有者
        $stmt = $conn->prepare("SELECT userid FROM tortoisepost WHERE id = ?");
        $stmt->bind_param("i", $replyId);
        $stmt->execute();
        $result = $stmt->get_result();
        $reply = $result->fetch_assoc();

        // 比较从回复中获取的作者名(userid字段)和当前登录的用户名
        if (!$reply || $reply['userid'] !== $loggedInUsername) {
            throw new Exception('権限がありません、または返信が存在しません。');
        }

        // 删除该回复
        $stmt = $conn->prepare("DELETE FROM tortoisepost WHERE id = ?");
        $stmt->bind_param("i", $replyId);
        $stmt->execute();

    } else {
        throw new Exception('指定された投稿タイプが無効です。');
    }

    // 提交事务
    $conn->commit();
    echo json_encode(['success' => true]);

} catch (Exception $e) {
    // 回滚事务
    $conn->rollback();
    echo json_encode(['success' => false, 'message' => 'データベース操作に失敗しました: ' . $e->getMessage()]);
}

// 关闭数据库连接
$conn->close();
?>