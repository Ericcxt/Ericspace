$(document).ready(function() {
    const topicContainer = $('#topic-list-container');
    const bottomPaginationContainer = $('#pagination-container-bottom');
    let currentSortBy = 'time'; // 设置一个默认值

    // 用于获取并显示主题的函数
    function loadTopics(page, sortBy) {
        currentSortBy = sortBy; // 更新当前的排序方式

        $.ajax({
            url: 'get_paged_topics.php',
            type: 'GET',
            data: {
                page: page,
                sort_by: sortBy
            },
            dataType: 'json',
            success: function(data) {
                if (data.status !== 'success') {
                    topicContainer.html('<p style="color: red;">トピックの読み込みに失敗しました。</p>');
                    return;
                }

                // --- 更新主题列表 ---
                topicContainer.empty();
                if (data.topics.length === 0) {
                    topicContainer.append('<p>トピックが見つかりませんでした。</p>');
                } else {
                    data.topics.forEach(function(topic) {
                        let lastPostInfo = topic.last_post_time ?
                            `最終更新: <span style="color: #666;">${topic.last_poster}</span> ${topic.last_post_time}` :
                            `作成日時: ${topic.createtime}`;

                        const topicHtml = `
                            <div style="display: flex; justify-content: space-between; align-items: flex-start; padding: 16px 8px; border-bottom: 1px solid #eee;">
                                <div>
                                    <p style="font-size: 18px; font-weight: bold; color: #222; margin: 0 0 8px 0;">
                                        <a href="TortoiseForumDiscussion.html?topic_id=${topic.id}" style="color: #222; text-decoration: none;">${topic.title}</a>
                                    </p>
                                    <p style="font-size: 12px; color: #999; margin: 0;">
                                        返信: ${topic.replies} &nbsp;&nbsp; 閲覧数: ${topic.views}
                                    </p>
                                </div>
                                <div style="text-align: right; flex-shrink: 0; padding-left: 16px;">
                                    <p style="font-size: 14px; color: #666; margin: 0 0 8px 0;">
                                        作成者: ${topic.author}
                                    </p>
                                    <p style="font-size: 14px; color: #999; margin: 0;">
                                        ${lastPostInfo}
                                    </p>
                                </div>
                            </div>`;
                        topicContainer.append(topicHtml);
                    });
                }

                // --- 更新分页 ---
                updatePagination(data.total_pages, data.current_page);
            },
            error: function(xhr, status, error) {
                console.error("エラー発生: " + status + " " + error);
                topicContainer.html('<p style="color: red;">トピックの取得中にエラーが発生しました。</p>');
            }
        });
    }

    // 更新分页链接的函数
    function updatePagination(totalPages, currentPage) {
        const paginationContainers = [bottomPaginationContainer];
        paginationContainers.forEach(container => {
            container.empty();
            if (totalPages > 1) {
                let paginationHtml = '';
                for (let i = 1; i <= totalPages; i++) {
                    // 注意：href现在是用于构建URL，data-page用于点击事件
                    paginationHtml += `<a href="?page=${i}&sort_by=${currentSortBy}" class="${i === currentPage ? 'active' : ''}" data-page="${i}">${i}</a>`;
                }
                container.html(paginationHtml);
            }
        });
    }

    // --- 事件处理 ---

    // 为分页链接设置事件代理
    $('.pagination-bar').on('click', '.pagination a', function(e) {
        e.preventDefault(); // 阻止链接默认的页面跳转行为
        const page = $(this).data('page');
        if (!$(this).hasClass('active')) {
            loadTopics(page, currentSortBy);
            // 使用history.pushState更新URL，而不重新加载页面
            const newUrl = `?page=${page}&sort_by=${currentSortBy}`;
            history.pushState({ page: page, sortBy: currentSortBy }, '', newUrl);
        }
    });

    // 排序功能
    $('#sort-by-time').on('click', function(e) {
        e.preventDefault();
        loadTopics(1, 'time');
        const newUrl = `?page=1&sort_by=time`;
        history.pushState({ page: 1, sortBy: 'time' }, '', newUrl);
        if (typeof toggleFilter === 'function') toggleFilter();
    });

    $('#sort-by-popularity').on('click', function(e) {
        e.preventDefault();
        loadTopics(1, 'popularity');
        const newUrl = `?page=1&sort_by=popularity`;
        history.pushState({ page: 1, sortBy: 'popularity' }, '', newUrl);
        if (typeof toggleFilter === 'function') toggleFilter();
    });

    // 监听浏览器的后退/前进事件
    $(window).on('popstate', function(event) {
        const state = event.originalEvent.state;
        if (state) {
            // 如果历史状态存在，则根据该状态加载内容
            loadTopics(state.page, state.sortBy);
        } else {
            // 否则，从URL中解析参数来加载
            const urlParams = new URLSearchParams(window.location.search);
            const page = parseInt(urlParams.get('page')) || 1;
            const sortBy = urlParams.get('sort_by') || 'time';
            loadTopics(page, sortBy);
        }
    });

    // --- 初始加载 ---
    // 页面首次加载时，从URL中获取参数
    const urlParams = new URLSearchParams(window.location.search);
    const initialPage = parseInt(urlParams.get('page')) || 1;
    const initialSort = urlParams.get('sort_by') || 'time';
    loadTopics(initialPage, initialSort);
});