document.addEventListener('DOMContentLoaded', function () { // 确保页面加载完成后执行
    // --- 获取所有需要的 HTML 元素 ---
    const chooseFileLabel = document.getElementById('choose-file-label'); // 获取“选择文件”按钮
    const fileInput = document.getElementById('file-input'); // 获取文件选择输入框
    const imagePreviewContainer = document.getElementById('image-preview-container'); // 获取图片预览容器
    const imagePreview = document.getElementById('image-preview'); // 获取图片预览元素
    const uploadButton = document.getElementById('upload-btn'); // 获取上传按钮
    
    // 获取用于显示文件名的 span 元素
    const fileNameSpan = document.getElementById('file-name');

    let selectedFile = null;

    // 监听“选择文件”按钮的点击
    chooseFileLabel.addEventListener('click', function (event) {
        event.preventDefault();

        // 检查登录状态
        fetch('check_session')
            .then(response => response.json())
            .then(data => {
                if (data.status === 'loggedin') {
                    fileInput.click(); // 已登录，触发文件选择
                } else {
                    alert('まずログインしてください！'); // 未登录，弹出提示
                }
            })
            .catch(error => {
                alert('检查登录状态失败，请稍后重试。');
            });
    });

    // 当用户选择了文件时
    fileInput.addEventListener('change', function (event) {
        selectedFile = event.target.files[0];
        if (selectedFile) {
            // 更新显示的文件名
            fileNameSpan.textContent = selectedFile.name;

            const reader = new FileReader();
            reader.onload = function (e) {
                imagePreview.src = e.target.result;
                imagePreviewContainer.style.display = 'block';
                uploadButton.style.display = 'inline-block';
            };
            reader.readAsDataURL(selectedFile);
        } else {
            // 如果用户取消了文件选择，恢复默认状态
            fileNameSpan.textContent = 'ファイルが選択されていません';
            selectedFile = null;
        }
    });

    // 当点击上传按钮时
    uploadButton.addEventListener('click', function () {
        if (!selectedFile) {
            alert('まずファイルを選択してください。');
            return;
        }

        const formData = new FormData();
        formData.append('file', selectedFile);

        // 发送文件到后端
        fetch('UploadServlet', {
            method: 'POST',
            // 添加 credentials: 'same-origin' 来确保 session cookie 被发送
            credentials: 'same-origin', 
            body: formData
        })
        .then(response => {
            // 如果响应不成功（例如 500 错误），我们读取文本并抛出错误
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(`服务器错误 (状态 ${response.status})。响应内容:\n${text}`);
                });
            }
            // 如果响应成功，我们解析 JSON。如果解析失败，也会自动进入 .catch
            return response.json();
        })
        .then(data => {
            if (data.success) {
                alert('アップロードに成功しました！');
                location.reload(); // 重新加载页面以显示新图片
            } else {
                // 如果后端返回 {success: false}, 则将其错误信息作为错误抛出，由 .catch 统一处理
                throw new Error(data.error || '服务器返回了未知错误。');
            }
        })
        .catch(error => {
            // 将统一捕获的详细错误信息显示在 alert 中
            alert('アップロードに失敗しました: ' + error.message);
        });
    });
});