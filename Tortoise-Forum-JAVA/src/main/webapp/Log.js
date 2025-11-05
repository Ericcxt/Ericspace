$(function(){
    $('form').on('submit', function(e){
        e.preventDefault(); // 阻止表单默认提交行为

        var requestData = {
            'username': $('#username').val(),
            'password': $('#password').val()
        };

        $.ajax({
            type: 'POST',
            url: 'log',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            dataType: 'json'
        })
        .done(function(data){
            if (data.status === 'success') {
                // 登录成功后，检查是否有重定向URL
                var urlParams = new URLSearchParams(window.location.search);
                var redirectUrl = urlParams.get('redirect');

                $('#res').css('color', 'green').text(data.message);
                setTimeout(function() {
                    if (redirectUrl) {
                        window.location.href = redirectUrl;
                    } else {
                        window.location.href = 'TortoiseForum.html'; // 默认跳转首页
                    }
                }, 1000);
            } else {
                $('#res').css('color', 'red').text(data.message);
            }
        })
        .fail(function(xhr, status, error){ 
            $('#res').css('color', 'red').text('通信エラーが発生しました。');
        });
    });
});