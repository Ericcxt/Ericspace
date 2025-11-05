$(document).ready(function() {
    $.ajax({
        url: 'get_topics.php',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            var topicsContainer = $('#topics-container'); // 主题容器
            topicsContainer.empty();

            if (data.status !== 'success' || !data.topics || data.topics.length === 0) {
                topicsContainer.append('<p>トピックが見つかりません。</p>');
                return;
            }

            data.topics.forEach(function(topic) {
                var topicHtml = `
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
                                作成日時: ${topic.createtime}
                            </p>
                        </div>
                    </div>
                `;
                topicsContainer.append(topicHtml);
            });
        },
        error: function(xhr, status, error) {
            console.error("An error occurred while fetching topics: " + status + " " + error);
            $('#topics-container').html('<p style="color: red;">トピックの取得中にエラーが発生しました。</p>');
        }
    });
});