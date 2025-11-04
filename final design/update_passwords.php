<?php
// --- 数据库连接信息 ---
$db_host = 'localhost';
$db_user = 'root';
$db_pass = '';
$db_name = 'test';

// 创建数据库连接
$conn = new mysqli($db_host, $db_user, $db_pass, $db_name);

// 检查连接
if ($conn->connect_error) {
    die("数据库连接失败: " . $conn->connect_error);
}

echo "<h1>旧密码迁移脚本 (V2 - 修正版)</h1>";

// --- 从数据库获取所有用户 ---
$sql = "SELECT username, password FROM tortoiseuser";
$result = $conn->query($sql);

if ($result->num_rows > 0) {
    $updated_count = 0;
    $skipped_count = 0;

    // --- 准备更新语句 ---
    $update_stmt = $conn->prepare("UPDATE tortoiseuser SET password = ? WHERE username = ?");

    // --- 遍历所有用户 ---
    while($row = $result->fetch_assoc()) {
        $username = $row['username'];
        $current_password = (string) $row['password'];

        echo "<hr>";
        echo "<b>处理用户: [" . htmlspecialchars($username) . "]</b><br>";
        echo "从数据库读取的密码是: <code>" . htmlspecialchars($current_password) . "</code><br>";

        // --- 检查密码是否已经是哈希格式 ---
        $info = password_get_info($current_password);
        
        // --- 核心修正 ---
        // 旧的判断 if ($info['algo'] === 0) 不够可靠。
        // 新的判断检查 algoName 是否为 'unknown'，这更准确。
        if ($info['algoName'] === 'unknown') {
            // 额外检查，如果密码为空或仅有空格，我们不应该哈希它
            if (empty(trim($current_password))) {
                echo "<span style='color:orange;'>判断: 密码为空，跳过更新。</span><br>";
                $skipped_count++;
                continue; // 跳到下一个用户
            }

            echo "判断: 这是一个明文密码。<b>正在尝试更新...</b> ";
            
            // 将明文密码哈希化
            $new_hash = password_hash($current_password, PASSWORD_DEFAULT);

            // 更新数据库
            $update_stmt->bind_param('ss', $new_hash, $username);
            if ($update_stmt->execute()) {
                echo "<span style='color:green; font-weight:bold;'>成功</span><br>";
                $updated_count++;
            } else {
                echo "<span style='color:red; font-weight:bold;'>失败: " . $update_stmt->error . "</span><br>";
            }
        } else {
            // 如果已经是哈希格式，则跳过
            echo "判断: 这是一个哈希密码 (算法: " . htmlspecialchars($info['algoName']) . ")。跳过。<br>";
            $skipped_count++;
        }
    }

    $update_stmt->close();

    echo "<hr><h2>处理完毕</h2>";
    echo "成功更新了 " . $updated_count . " 个用户的密码。<br>";
    echo "跳过了 " . $skipped_count . " 个用户 (已有哈希密码或密码为空)。<br>";
    echo "<p style='color:red; font-weight:bold;'>重要提示：迁移成功后，请立即删除此脚本 (update_passwords.php)！</p>";

} else {
    echo "数据库中没有找到任何用户。";
}

$conn->close();
?>