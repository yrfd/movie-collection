// js/rating-sync.js - 评分同步模块

const RatingSync = {
    // 存储当前页面需要监听的电影ID
    watchedMovies: new Set(),

    // 初始化
    init() {
        // 监听评分更新事件
        window.addEventListener('rating-updated', (event) => {
            const { tmdbId, movieId, newRating, ratingCount } = event.detail;
            this.refreshRating(tmdbId || movieId, newRating, ratingCount);
        });

        // 监听页面可见性变化，恢复时刷新
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) {
                this.refreshAllVisibleRatings();
            }
        });
    },

    // 刷新单个电影的评分显示
    async refreshRating(movieId, newRating, ratingCount) {
        // 刷新发现电影页面的卡片
        const cardRatingElement = document.getElementById(`rating-${movieId}`);
        if (cardRatingElement) {
            if (newRating !== undefined) {
                this.updateRatingDisplay(cardRatingElement, newRating, ratingCount);
            } else {
                // 重新从服务器获取
                await this.loadAndUpdateRating(movieId, cardRatingElement);
            }
        }

        // 刷新排行榜页面（如果当前在排行榜页面）
        if (window.location.pathname.includes('top-rated.html')) {
            if (window.refreshRankings) {
                window.refreshRankings();
            }
        }

        // 刷新电影详情模态框（如果打开）
        const detailRatingSpan = document.getElementById('platformRatingDetail');
        if (detailRatingSpan && detailRatingSpan.closest('.modal.show')) {
            await this.loadRatingForDetail(movieId);
        }
    },

    // 加载并更新评分显示
    async loadAndUpdateRating(tmdbId, element) {
        try {
            const response = await fetch(`${API_BASE_URL}/comment/averageByTmdb/${tmdbId}`);
            const result = await response.json();
            if (result.code === 200 && result.data) {
                this.updateRatingDisplay(element, result.data.avgRating, result.data.count);
            }
        } catch (error) {
            console.error('刷新评分失败:', error);
        }
    },

    // 更新评分显示元素
    updateRatingDisplay(element, avgRating, count) {
        if (avgRating > 0) {
            element.innerHTML = `<span class="label">本平台评分：</span><span class="value">⭐ ${avgRating.toFixed(1)} 分 (${count}人评价)</span>`;
        } else {
            element.innerHTML = `<span class="label">本平台评分：</span><span class="value">暂无评分</span>`;
        }
    },

    // 为详情页加载评分
    async loadRatingForDetail(tmdbId) {
        const ratingSpan = document.getElementById('platformRatingDetail');
        if (!ratingSpan) return;

        try {
            const response = await fetch(`${API_BASE_URL}/comment/averageByTmdb/${tmdbId}`);
            const result = await response.json();
            if (result.code === 200 && result.data && result.data.avgRating > 0) {
                ratingSpan.innerHTML = `⭐ ${result.data.avgRating.toFixed(1)} 分 (${result.data.count}人评价)`;
            } else {
                ratingSpan.innerHTML = '暂无评分';
            }
        } catch (error) {
            ratingSpan.innerHTML = '加载失败';
        }
    },

    // 刷新所有可见的评分
    refreshAllVisibleRatings() {
        document.querySelectorAll('[id^="rating-"]').forEach(element => {
            const tmdbId = element.id.replace('rating-', '');
            this.loadAndUpdateRating(tmdbId, element);
        });
    }
};

// 触发评分更新事件
function emitRatingUpdated(tmdbId, newRating, ratingCount) {
    const event = new CustomEvent('rating-updated', {
        detail: { tmdbId, newRating, ratingCount }
    });
    window.dispatchEvent(event);
}

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', () => {
    RatingSync.init();
});