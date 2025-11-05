$(document).ready(function() {
    const urlParams = new URLSearchParams(window.location.search);
    const topicId = urlParams.get('topic_id');

    // 讨论详情页，获取讨论ID
    if (topicId) {
        $('#topic_id_field').val(topicId);
    } else {
        $('#discussion-container').html('<p>エラー: トピックIDがありません。</p>');
        return;
    }

    if (topicId) {
        $.ajax({
            url: 'get_discussion_details.php',
            type: 'GET',
            data: { topic_id: topicId },
            dataType: 'json',
            success: function(response) {
                if (response.status === 'redirect') {
                    window.location.href = response.location;
                    return;
                }

                if (response.error) {
                    alert('読み込みに失敗しました: ' + response.error);
                    return;
                }

                // --- 修正的标题更新逻辑 ---
                const topic = response.topic;
                document.title = topic.title; // 更新浏览器标签页标题
                $('#discussion-title').text(topic.title); // 使用正确的ID "discussion-title"

                const postsContainer = $('#posts-container');
                postsContainer.empty();

                // --- 渲染主帖子 ---
                const mainPost = response.topic;
                const originalPoster = mainPost.author;

                // 根据profilepic路径生成主贴头像HTML
                const mainAvatarHtml = mainPost.profilepic
                    ? `<img src="${mainPost.profilepic}" alt="${mainPost.author}'s avatar">`
                    : ''; // 如果没有头像，则为空，CSS会显示灰色背景

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
                            <div class="post-timestamp">${mainPost.createtime}</div>
                        </div>
                    </div>
                `;
                postsContainer.append(mainPostHtml);

                // --- 渲染回复 ---
                if (response.replies && response.replies.length > 0) {
                    response.replies.forEach(reply => {
                        const authorClass = (reply.author === originalPoster) ? 'main-post' : '';

                        // 根据profilepic路径生成回复者头像HTML
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
                                    <div class="post-timestamp">${reply.createtime}</div>
                                </div>
                            </div>
                        `;
                        postsContainer.append(replyHtml);
                    });
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                alert('投稿の読み込み中にネットワークエラーが発生しました。');
            }
        });
    } else {
        $('#posts-container').html('<p>トピックIDが指定されていません。</p>');
    }

    // 检查登录状态以确定是否启用回复功能
    $.ajax({
        url: 'check_session.php',
        type: 'GET',
        dataType: 'json',
        success: function(sessionData) {
            if (sessionData.status !== 'loggedin') {
                // 如果未登录，只禁用回复框
                $('#reply-content').prop('disabled', true).attr('placeholder', '返信を投稿するにはログインしてください。');
            }
            // 对按钮不做任何操作
        },
        error: function() {
            // 如果会话检查失败，为安全起见也禁用回复框
            $('#reply-content').prop('disabled', true).attr('placeholder', 'ログイン状態を確認できませんでした。後でもう一度お試しください。');
        }
    });
});