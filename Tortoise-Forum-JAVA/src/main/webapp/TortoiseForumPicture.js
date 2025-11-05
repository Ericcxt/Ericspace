$(document).ready(function() {
    const grid = $('#pictures-grid');
    const loadingIndicator = $('<p>ユーザーの画像を読み込み中...</p>');
    grid.append(loadingIndicator);

    // 使用 AJAX 从 GetImagesServlet 获取图片数据
    $.ajax({
        url: 'GetImagesServlet',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            loadingIndicator.remove(); // 移除加载提示
            if (data.images.length === 0) {
                grid.append($('<p>アップロードされた画像は見つかりませんでした。</p>'));
            } else {
                // 遍历返回的图片数组
                data.images.forEach(function(image) {
                    const imageWrapper = $('<div class="image-wrapper"></div>');
                    const imageContainer = $('<div class="image-container"></div>').css({
                        'background-image': 'url(' + image.filepath + ')'
                    });
                    const imageTooltip = $('<div class="image-tooltip"></div>').text('投稿者: ' + image.userid); // 图片提示框

                    imageWrapper.append(imageContainer).append(imageTooltip); // 图片容器和提示框添加到包装器
                    grid.append(imageWrapper); 
                });
            }
        },
        error: function(xhr, status, error) {
            loadingIndicator.remove(); // 移除加载提示
            // 如果获取图片失败，在页面上显示错误信息
            const errorMsg = $('<p style="color: red;">画像の読み込みに失敗しました。後でもう一度お試しください。</p>');
            grid.append(errorMsg);
            console.error("画像の取得中にエラーが発生しました:", error, xhr.responseText);
        }
    });
});