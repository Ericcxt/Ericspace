$(document).ready(function() {
    // 监听注册表单的提交事件
    $('#register-form').on('submit', function(e) {
        // 阻止表单的默认提交行为，不让页面整页刷新，而是局部更新
        e.preventDefault();

        // 清空之前的错误信息
        $('#error-message').text('');

        // 获取密码和确认密码的值
        var password = $('#password').val();
        var confirmPassword = $('#confirm-password').val();

        // 检查两次输入的密码是否一致
        if (password !== confirmPassword) {
            $('#error-message').text('入力されたパスワードが一致しません。もう一度お試しください。').css('color', 'red');
            return; // 如果不匹配，则停止执行
        }

        // 创建一个 FormData 对象，用于收集表单数据，包括文件
        var formData = new FormData(this);

        // 使用 jQuery 的 AJAX 发送表单数据
        $.ajax({
            url: 'Sign.php',          // 请求后端的 URL
            type: 'POST',             // 请求方法
            data: formData,           // 发送的数据
            processData: false,       // 告诉 jQuery 不要处理数据
            contentType: false,       // 告诉 jQuery 不要设置 Content-Type 请求头
            dataType: 'json',         // 期望从服务器返回 json 格式的数据
            success: function(response) { // 请求成功时的回调函数
                // 检查后端返回的状态是否为 'success'
                if (response.status === 'success') {
                    // 注册成功，显示绿色成功信息
                    $('#error-message').text('登録が成功しました！ログインページに移動します。').css('color', 'green');
                    // 2秒后跳转到登录页面
                    setTimeout(function() {
                        window.location.href = 'TortoiseForumLog.html';
                    }, 2000);
                } else {
                    // 如果注册失败，则在页面上显示红色错误信息
                    $('#error-message').text(response.message).css('color', 'red');
                }
            },
            error: function(xhr, status, error) { // 请求失败时的回调函数
                // 也在页面上显示红色通用错误信息
                $('#error-message').text('登録中にエラーが発生しました。しばらくしてからもう一度お試しください。').css('color', 'red');
            }
        });
    });

    // 监听表单输入框的输入事件，当用户开始输入时清空错误信息
    $('#register-form input').on('input', function() {
        $('#error-message').text('');
    });

    // 监听文件上传输入框的变化事件，用于头像上传部分功能显示所选文件名
    $('#file-upload').on('change', function() {
        // 获取文件名
        var fileName = $(this).val().split('\\').pop();
        // 在页面上显示文件名，如果未选择文件则显示 '未选择文件'
        $('#file-name').text(fileName || '選択されていません');
    });
});