/**
 * API 接口封装 - 调用后端 Spring Boot 服务
 */

const API_BASE_URL = 'http://localhost:8080';

// 获取 Token
function getToken() {
    return localStorage.getItem('token');
}

// 通用请求方法
async function request(url, options = {}) {
    const token = getToken();
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(`${API_BASE_URL}${url}`, {
            ...options,
            headers
        });

        const data = await response.json();

        if (data.code === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
        }

        return data;
    } catch (error) {
        console.error('API请求失败:', error);
        // 返回更详细的错误信息
        return {
            code: 500,
            message: error.message || '网络请求失败，请检查：\n1. 后端服务是否启动\n2. 网络连接是否正常'
        };
    }
}

// ==================== 用户相关 API ====================
const userAPI = {
    register: (username, password, email,code) => {
        return request('/user/register', {
            method: 'POST',
            body: JSON.stringify({ username, password, email,code })
        });
    },
    login: (username, password) => {
        return request('/user/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
    },
    updateProfile: (data) => {
        return request('/user/update', {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },
    sendRegisterCode: (email) => {
        return request(`/user/send-code?email=${encodeURIComponent(email)}`, {
            method: 'POST'
        });
    },

    // 发送找回密码验证码
    sendResetCode: (email) => {
        return request(`/user/send-reset-code?email=${encodeURIComponent(email)}`, {
            method: 'POST'
        });
    },

    // 重置密码
    resetPassword: (email, code, newPassword) => {
        return request('/user/reset-password', {
            method: 'POST',
            body: JSON.stringify({ email, code, newPassword })
        });
    },

    updateUsername: (newUsername) => {
        return request(`/user/update-username?newUsername=${encodeURIComponent(newUsername)}`, {
            method: 'PUT'
        });
    },

    // 发送修改邮箱验证码
    sendEmailCode: (newEmail) => {
        return request(`/user/send-email-code?newEmail=${encodeURIComponent(newEmail)}`, {
            method: 'POST'
        });
    },

    // 修改邮箱
    updateEmail: (newEmail, code) => {
        return request('/user/update-email', {
            method: 'PUT',
            body: JSON.stringify({ newEmail, code })
        });
    },

    // 修改密码
    updatePassword: (oldPassword, newPassword) => {
        return request('/user/update-password', {
            method: 'PUT',
            body: JSON.stringify({ oldPassword, newPassword })
        });
    }
};

// ==================== 电影相关 API ====================
const movieAPI = {
    getAllMovies: () => {
        return request('/movie/list');
    },
    searchMovies: (params) => {
        const queryString = new URLSearchParams(params).toString();
        return request(`/movie/search?${queryString}`);
    },
    addMovie: (movieData) => {
        return request('/movie/add', {
            method: 'POST',
            body: JSON.stringify(movieData)
        });
    },
    updateMovie: (collectionId, movieData) => {
        return request(`/movie/update/${collectionId}`, {
            method: 'PUT',
            body: JSON.stringify(movieData)
        });
    },
    deleteMovie: (collectionId) => {
        return request(`/movie/delete/${collectionId}`, {
            method: 'DELETE'
        });
    },
    // 获取用户的所有分类
    getCategories: () => {
        return request('/movie/categories');
    },

    // 创建新分类
    createCategory: (categoryName) => {
        return request('/movie/categories', {
            method: 'POST',
            body: JSON.stringify({ categoryName })
        });
    },

    // 删除分类
    deleteCategory: (categoryId) => {
        return request(`/movie/categories/${categoryId}`, {
            method: 'DELETE'
        });
    },

    // 移动单个收藏到分类
    moveToCategory: (collectionId, categoryId) => {
        return request(`/movie/collections/${collectionId}/move-to/${categoryId}`, {
            method: 'PUT'
        });
    },

    // 批量移动收藏到分类
    batchMoveToCategory: (collectionIds, categoryId) => {
        return request(`/movie/collections/batch-move-to/${categoryId}`, {
            method: 'POST',
            body: JSON.stringify(collectionIds)
        });
    },

    updateCategory: (categoryId, categoryName) => {
        return request(`/movie/categories/${categoryId}`, {
            method: 'PUT',
            body: JSON.stringify({ categoryName })
        });
    },

    getMoviesByCategory: (categoryId) => {
        return request(`/movie/collections/by-category/${categoryId}`);
    }

};

// ==================== 评论相关 API ====================
const commentAPI = {
    getCommentsByMovie: (movieId) => {
        return request(`/comment/list/${movieId}`);
    },
    getCommentsByTMDBId: (tmdbId) => {
        return request(`/comment/listByTmdb/${tmdbId}`);
    },
    getAverageRating: (movieId) => {
        return request(`/comment/average/${movieId}`);
    },
    getAverageRatingByTMDBId: (tmdbId) => {
        return request(`/comment/averageByTmdb/${tmdbId}`);
    },
    addComment: (commentData) => {
        return request('/comment/add', {
            method: 'POST',
            body: JSON.stringify(commentData)
        });
    },
    addCommentByTMDB: (commentData) => {
        return request('/comment/addByTmdb', {
            method: 'POST',
            body: JSON.stringify(commentData)
        });
    },
    updateComment: (commentId, commentData) => {
        return request(`/comment/update/${commentId}`, {
            method: 'PUT',
            body: JSON.stringify(commentData)
        });
    },
    deleteComment: (commentId) => {
        return request(`/comment/delete/${commentId}`, {
            method: 'DELETE'
        });
    },
    getMyComments: () => {
        return request('/comment/my');
    }
};

const reviewAPI = {
    // 获取我的所有评价（公开+私人）
    getAllReviews: (type = 'all') => {
        return request(`/review/my/all?type=${type}`);
    },

    // 获取评价统计
    getReviewStats: () => {
        return request('/review/my/stats');
    },

    // 更新私人评价
    updatePrivateReview: (tmdbId, privateReview) => {
        return request('/review/private', {
            method: 'PUT',
            body: JSON.stringify({ tmdbId, privateReview })
        });
    }
};

