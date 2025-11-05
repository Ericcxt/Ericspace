$(document).ready(function() {
    // 初始加载用户资料
    $.ajax({
        url: 'get_user_profile.php',
        type: 'GET',
        dataType: 'json',
        success: function(response) {
            if (response.status === 'success') {
                // 填充视图区域
                $('#profile-username').text(response.username);
                $('#profile-email').text(response.email);
                $('#profile-country').text(response.country || '未設定');

                // 填充编辑表单的默认值
                $('#email').val(response.email);
                $('#country').val(response.country); // 确保下拉菜单也被设置

                if(response.profilepic) {
                    $('#profile-avatar').html('<img src="' + response.profilepic + '">');
                    $('#avatar-preview').html('<img src="' + response.profilepic + '">');
                } else {
                    // 确保在没有头像时，容器是空的
                    $('#profile-avatar').empty();
                    $('#avatar-preview').empty();
                }

            } else {
                alert('マイページを表示するにはログインしてください');
                window.location.href = 'TortoiseForumLog.html';
            }
        },
        error: function(xhr, status, error) {
            alert('プロフィールの読み込み中にエラーが発生しました。');
        }
    });

    // 统一的左侧菜单点击事件
    $('.profile-menu-item').click(function(e) {
        e.preventDefault(); // 阻止 a 标签的默认跳转行为

        var target = $(this).data('target');

        // 更新 URL hash
        window.location.hash = target;

        // --- 修复菜单高亮 ---
        // 移除所有菜单项的 active 类
        $('.profile-menu-item').removeClass('active');
        // 为当前点击的菜单项添加 active 类
        $(this).addClass('active');

        // --- 修复内容区域切换 ---
        var target = $(this).data('target');
        // 隐藏所有 content-section
        $('.content-section').removeClass('active');
        // 只显示目标 content-section
        $('#' + target).addClass('active');

        // 如果点击的是 "My Posts"，则加载帖子数据
        if (target === 'my-posts') {
            loadUserPosts();
        }
        // 如果点击的是 "My Pictures"，则加载图片数据
        if (target === 'my-pictures') {
            loadUserPictures();
        }
    });

    // 新增：页面加载时检查 hash 并切换到对应的标签页
    function handleHashChange() {
        var hash = window.location.hash.substring(1); // 获取 hash 值并移除 '#'
        if (hash) {
            // 找到与 hash 对应的菜单项
            var targetMenuItem = $('.profile-menu-item[data-target="' + hash + '"]');
            if (targetMenuItem.length) {
                // 触发点击事件来切换标签页和内容
                targetMenuItem.click();
            }
        }
    }

    // 页面加载时立即处理一次 hash
    handleHashChange();

    // 监听 hash 变化事件（例如，用户点击浏览器的前进/后退按钮）
    $(window).on('hashchange', handleHashChange);


    /**
     * @function loadUserPictures
     * 加载并显示用户的图片
     */
    function loadUserPictures() {
        $.ajax({
            url: 'get_user_pictures.php', // 新的PHP文件
            type: 'GET',
            dataType: 'json',
            success: function(response) {
                var picturesContainer = $('#my-pictures-list');
                picturesContainer.empty(); // 清空现有内容

                // 修改: 直接检查 response 是否是一个数组并且长度大于 0
                if (Array.isArray(response) && response.length > 0) {
                    response.forEach(function(picture) {
                        // 注意：这里的filepath需要根据实际情况调整
                        var pictureHtml = `
                            <div class="picture-item" data-picture-id="${picture.id}">
                                <img src="${picture.filepath}" alt="User Picture">
                                <button class="delete-picture-btn" title="削除">&times;</button>
                            </div>
                        `;
                        picturesContainer.append(pictureHtml);
                    });
                } else {
                    picturesContainer.html('<p>まだ写真がアップロードされていません。</p>');
                }
            },
            error: function() {
                $('#my-pictures-list').html('<p>写真の読み込み中にエラーが発生しました。</p>');
            }
        });
    }

    /**
     * @function loadUserPosts
     * 加载并显示用户的帖子和回复
     */
    function loadUserPosts() {
        $.ajax({
            url: 'get_user_posts.php',
            type: 'GET',
            dataType: 'json',
            success: function(response) {
                if (response.status === 'success') {
                    var postsContainer = $('#my-posts-list');
                    postsContainer.empty(); // 清空现有内容

                    if (response.posts.length > 0) {
                        var topics = response.posts.filter(p => p.type === 'topic');
                        var replies = response.posts.filter(p => p.type === 'reply');

                        // 渲染主题帖
                        if (topics.length > 0) {
                            postsContainer.append('<div class="post-type-separator"><span>自分のトピック</span></div>');
                            topics.forEach(function(post) {
                                postsContainer.append(createPostHtml(post, 'トピック'));
                            });
                        }

                        // 如果同时有主题和回复，添加分隔
                        if (topics.length > 0 && replies.length > 0) {
                            postsContainer.append('<div class="post-type-separator" style="margin-top: 40px;"><span>自分の返信</span></div>');
                        } else if (replies.length > 0) {
                            postsContainer.append('<div class="post-type-separator"><span>自分の返信</span></div>');
                        }
                        
                        // 渲染回复
                        if (replies.length > 0) {
                            replies.forEach(function(post) {
                                postsContainer.append(createPostHtml(post, '返信'));
                            });
                        }

                    } else {
                        postsContainer.html('<p>まだ投稿や返信がありません。</p>');
                    }
                } else {
                    alert(response.message);
                }
            },
            error: function() {
                alert('投稿の読み込み中にエラーが発生しました。');
            }
        });
    }

    // 辅助函数：创建单个帖子的HTML
    function createPostHtml(post, postType) {
        return `
            <div class="user-post-item" data-post-id="${post.post_id}" data-topic-id="${post.topic_id}" data-type="${post.type}">
                <div class="post-header">
                    <span class="post-type">[${postType}]</span>
                    <strong class="post-title">
                        <a href="TortoiseForumDiscussion.html?topic_id=${post.topic_id}">${post.title}</a>
                    </strong>
                    <span class="post-time">${post.createtime}</span>
                    <button class="delete-post-btn">削除</button>
                </div>
                <div class="post-content">
                    ${post.content}
                </div>
            </div>
        `;
    }

    // 使用事件委托为动态生成的图片删除按钮绑定点击事件
    $('#my-pictures-list').on('click', '.delete-picture-btn', function() {
        var pictureItem = $(this).closest('.picture-item');
        var pictureId = pictureItem.data('picture-id');

        if (confirm('この写真を削除してもよろしいですか？')) {
            $.ajax({
                url: 'delete_picture.php',
                type: 'POST',
                data: { id: pictureId },
                dataType: 'json',
                success: function(response) {
                    // 统一处理响应，无论成功或失败
                    if (response.success) {
                        alert(response.message); // 显示成功的消息
                        pictureItem.fadeOut(500, function() {
                            $(this).remove();
                            if ($('#my-pictures-list').children().length === 0) {
                                $('#my-pictures-list').html('<p>まだ写真がアップロードされていません。</p>');
                            }
                        });
                    } else {
                        // 如果后端返回 success: false，也在这里处理
                        alert('削除に失敗しました: ' + response.message);
                    }
                },
                error: function() {
                    // 这个回调只在网络错误或服务器崩溃时触发
                    alert('写真の削除中にサーバーエラーが発生しました。');
                }
            });
        }
    });

    // 使用事件委托为动态生成的帖子删除按钮绑定点击事件
    $('#my-posts-list').on('click', '.delete-post-btn', function() {
        var postItem = $(this).closest('.user-post-item');
        var type = postItem.data('type');
        var postId = postItem.data('post-id');
        var topicId = postItem.data('topic-id'); // 这是可靠的主题ID

        var idToDelete;
        // 根据帖子类型，决定使用哪个ID进行删除
        if (type === 'topic') {
            // 如果是主题帖，使用 topicId
            idToDelete = topicId;
        } else { // type === 'reply'
            // 如果是回复，使用 postId (即回复自身的ID)
            idToDelete = postId;
        }

        var confirmationMessage = type === 'topic'
            ? 'このトピックとすべての返信を削除してもよろしいですか？この操作は元に戻せません。'
            : 'この返信を削除してもよろしいですか？';

        if (confirm(confirmationMessage)) {
            $.ajax({
                url: 'delete_post.php',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    type: type,
                    id: idToDelete // 发送正确决定的ID
                }),
                dataType: 'json',
                success: function(response) {
                    if (response.success) {
                        postItem.fadeOut(500, function() {
                            var postType = $(this).data('type');
                            var list = $('#my-posts-list');
                            $(this).remove();

                            // 检查并移除空的分类标题
                            if (list.find(`.user-post-item[data-type='${postType}']`).length === 0) {
                                list.find(`.post-type-separator:contains('My ${postType === 'topic' ? 'Topics' : 'Replies'}')`).remove();
                            }
                            
                            // 检查整个列表是否已空
                            if (list.children().length === 0) {
                                list.html('<p>まだ投稿や返信がありません。</p>');
                            }
                        });
                    } else {
                        alert('削除に失敗しました: ' + response.message);
                    }
                },
                error: function(xhr) {
                    alert('投稿の削除中にエラーが発生しました。');
                }
            });
        }
    });

    // 编辑个人资料的保存按钮事件
    $('#edit-profile .save-button').click(function() {
        var formData = new FormData();
        var email = $('#email').val();
        var country = $('#country').val();
        var avatarFile = $('#file-upload')[0].files[0];

        formData.append('email', email);
        formData.append('country', country);
        if (avatarFile) {
            formData.append('avatar', avatarFile);
        }

        $.ajax({
            url: 'update_profile.php',
            type: 'POST',
            data: formData,
            processData: false, // 告诉jQuery不要处理数据
            contentType: false, // 告诉jQuery不要设置contentType
            dataType: 'json',
            success: function(response) {
                alert(response.message);
                if (response.status === 'success') {
                    location.reload();
                }
            },
            error: function() {
                alert('プロフィールの更新中にエラーが発生しました。');
            }
        });
    });

    // 为“修改密码”的保存按钮绑定事件
    $('#password .save-button').click(function() {
        var currentPassword = $('#current-password').val();
        var newPassword = $('#new-password').val();
        var confirmNewPassword = $('#confirm-new-password').val();

        if (!currentPassword || !newPassword || !confirmNewPassword) {
            alert('すべてのパスワード欄を入力してください。');
            return;
        }

        if (newPassword !== confirmNewPassword) {
            alert('新しいパスワードと確認用パスワードが一致しません。');
            return;
        }

        $.ajax({
            url: 'change_password.php',
            type: 'POST',
            data: {
                current_password: currentPassword,
                new_password: newPassword,
                confirm_new_password: confirmNewPassword
            },
            dataType: 'json',
            success: function(response) {
                alert(response.message);
                if (response.status === 'success') {
                    $('#current-password').val('');
                    $('#new-password').val('');
                    $('#confirm-new-password').val('');
                }
            },
            error: function() {
                alert('パスワードの変更中にエラーが発生しました。');
            }
        });
    });

    // 移除冲突的 show() 调用
    // 初始状态由 HTML 中的 'active' 类控制
});