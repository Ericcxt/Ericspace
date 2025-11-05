$(document).ready(function() {
    $('#new-thread-form').on('submit', function(e) {
        e.preventDefault(); // 阻止表单默认的同步提交行为

        var title = $('#thread-title').val();
        var content = $('#thread-content').val();

        if (!title.trim() || !content.trim()) {
            alert('タイトルと内容は空にできません。');
            return;
        }

        $.ajax({
            url: 'add_topic.php',
            type: 'POST',
            data: {
                title: title,
                content: content
            },
            dataType: 'json',
            success: function(response) {
                if (response.status === 'success') {
                    alert('スレッドは正常に作成されました。');
                    // 跳转到新创建的帖子页面
                    window.location.href = 'TortoiseForumDiscussion.html?topic_id=' + response.topic_id;
                } else {
                    alert('エラー: ' + response.message);
                }
            },
            error: function(xhr, status, error) {
                // 处理 AJAX 请求本身的错误
                alert('スレッドの作成中にエラーが発生しました。もう一度お試しください。詳細: ' + error);
            }
        });
    });
});