$(document).ready(function() {
    const urlParams = new URLSearchParams(window.location.search);
    const topicId = urlParams.get('topic_id');

    if (topicId) {
        $('#topic_id_field').val(topicId);
    } else {
        $('#discussion-container').html('<p>エラー: トピックIDがありません。</p>');
        return;
    }

    function loadDiscussionDetails() {
        $.ajax({
            url: 'get_discussion_details',
            type: 'GET',
            data: { topic_id: topicId },
            dataType: 'json',
            success: function(response) {
                if (response.error) {
                    alert('読み込みに失敗しました: ' + response.error);
                    return;
                }

                const topic = response.topic;
                document.title = topic.title;
                $('#discussion-title').text(topic.title);

                const postsContainer = $('#posts-container');
                postsContainer.empty();

                // --- 还原原始的 HTML 结构和 CSS 类名 ---
                const mainPost = response.topic;
                const originalPoster = mainPost.author;

                const mainAvatarHtml = mainPost.profilepic
                    ? `<img src="${mainPost.profilepic}" alt="${mainPost.author}'s avatar">`
                    : ''; // 保持和 PHP 版本一致，无头像则为空

                const mainPostHtml = `
                    <div class="post-container main-post">
                        <div class="post-author-info">
                            <div class="avatar">${mainAvatarHtml}</div>
                            <div class="username">${mainPost.author}</div>
                        </div>
                        <div class="post-body">
                            <div class="post-main-content">
                                <p>${mainPost.content}</p>
                            </div>
                            <div class="post-timestamp">${new Date(mainPost.createtime).toLocaleString()}</div>
                        </div>
                    </div>
                `;
                postsContainer.append(mainPostHtml);

                if (response.replies && response.replies.length > 0) {
                    response.replies.forEach(reply => {
                        // 【修复】为回复的容器也加上 post-container 类
                        const authorClass = (reply.author === originalPoster) ? 'main-post' : '';

                        const replyAvatarHtml = reply.profilepic
                            ? `<img src="${reply.profilepic}" alt="${reply.author}'s avatar">`
                            : '';

                        const replyHtml = `
                            <div class="post-container ${authorClass}">
                                <div class="post-author-info">
                                    <div class="avatar">${replyAvatarHtml}</div>
                                    <div class="username">${reply.author}</div>
                                </div>
                                <div class="post-body">
                                    <div class="post-main-content">
                                        <p>${reply.content}</p>
                                    </div>
                                    <div class="post-timestamp">${new Date(reply.createtime).toLocaleString()}</div>
                                </div>
                            </div>
                        `;
                        postsContainer.append(replyHtml);
                    });
                }
            },
            error: function() {
                alert('投稿の読み込み中にネットワークエラーが発生しました。');
            }
        });
    }

    if (topicId) {
        loadDiscussionDetails();
    }

    // 检查登录状态的逻辑保持不变
    $.ajax({
        url: 'check_session', // 注意：这里也需要确认 Servlet 地址
        type: 'GET',
        dataType: 'json',
        success: function(sessionData) {
            if (sessionData.status !== 'loggedin') {
                $('#reply-content').prop('disabled', true).attr('placeholder', '返信を投稿するにはログインしてください。');
            }
        },
        error: function() {
            $('#reply-content').prop('disabled', true).attr('placeholder', 'ログイン状態を確認できませんでした。');
        }
    });
});