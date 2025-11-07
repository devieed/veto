// 用户列表页面交互逻辑
document.addEventListener('DOMContentLoaded', function () {
    const usernameSearch = document.getElementById('usernameSearch');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');

    // 搜索功能
    if (usernameSearch) {
        usernameSearch.addEventListener('input', function() {
            // 延迟搜索，避免频繁请求
            clearTimeout(this.searchTimeout);
            this.searchTimeout = setTimeout(() => {
                performSearch();
            }, 500);
        });

        usernameSearch.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                performSearch();
            }
        });
    }

    // 执行搜索
    function performSearch() {
        const searchTerm = usernameSearch.value.trim();
        const currentUrl = new URL(window.location);
        
        if (searchTerm) {
            currentUrl.searchParams.set('search', searchTerm);
        } else {
            currentUrl.searchParams.delete('search');
        }
        
        // 重置到第一页
        currentUrl.searchParams.set('page', '0');
        
        window.location.href = currentUrl.toString();
    }

    // 分页按钮事件
    if (prevBtn && nextBtn) {
        prevBtn.addEventListener('click', function() {
            if (!this.disabled) {
                changePage(-1);
            }
        });

        nextBtn.addEventListener('click', function() {
            if (!this.disabled) {
                changePage(1);
            }
        });
    }

    // 切换页面
    function changePage(direction) {
        const currentUrl = new URL(window.location);
        const currentPage = parseInt(currentUrl.searchParams.get('page') || '0');
        const newPage = Math.max(0, currentPage + direction);
        
        currentUrl.searchParams.set('page', newPage.toString());
        window.location.href = currentUrl.toString();
    }
});

// 编辑用户
function editUser(userId) {
    console.log(userId);
    window.location.href = `/user/edit/${userId}`;
}

// 删除用户
function deleteUser(userId) {
    if (confirm('确定要删除该用户吗？')) {
        fetch(`/admin/users/${userId}`, {
            method: 'DELETE'
        }).then(response => {
            if (response.ok) {
                if (window.showNotification) {
                    window.showNotification('用户删除成功', 'success');
                }
                // 刷新页面
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                if (window.showNotification) {
                    window.showNotification('删除失败，请重试', 'error');
                }
            }
        }).catch(() => {
            if (window.showNotification) {
                window.showNotification('网络错误，请重试', 'error');
            }
        });
    }
}