$(function(){
    $('form').on('submit', function(e){
        e.preventDefault(); // 阻止表单默认提交行为，可以无刷新登录

        $.ajax({
            type: 'POST',
            url: 'Log.php',
            dataType: 'json',
            data: {
                'username': $('#username').val(),
                'password': $('#password').val()
            }
        })
        .done(function(data){
            if (data.status === 'success') {
                // 登录成功后，检查是否有重定向URL
                var urlParams = new URLSearchParams(window.location.search);
                var redirectUrl = urlParams.get('redirect');

                $('#res').css('color', 'green').text(data.message);
                setTimeout(function() { // 登录成功后，1秒后跳转    
                    if (redirectUrl) {
                        window.location.href = redirectUrl;
                    } else {
                        window.location.href = 'TortoiseForum.html'; // 没有重定向URL，默认跳转首页
                    }
                }, 1000); // 延迟1秒后跳转
            } else {
                $('#res').css('color', 'red').text(data.message); // 登录失败，显示错误信息
            }
        })
        .fail(function(xhr, status, error){ 
            $('#res').css('color', 'red').text('通信エラーが発生しました。');
        });
    });
});