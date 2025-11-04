<?php
session_start(); 

// 登出所以清除所有 Session 变量
session_unset();

// 登出后销毁档案
session_destroy();

// 登出后返回 JSON 响应
header('Content-Type: application/json');
echo json_encode(['status' => 'loggedout']); //返回登出状态
exit;
?>