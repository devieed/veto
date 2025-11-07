// 钱包列表页面交互逻辑
document.addEventListener('DOMContentLoaded', function () {
    const userSearch = document.getElementById('userSearch');
    const statusFilter = document.getElementById('statusFilter');
    const prevBtn = document.querySelector('.pagination button:first-child');
    const nextBtn = document.querySelector('.pagination button:last-child');

    // 搜索处理
    window.handleSearch = function() {
        const userId = userSearch.value.trim();
        const status = statusFilter.value;
        
        // 构建查询参数
        const params = new URLSearchParams();
        if (userId) {
            params.append('userId', userId);
        }
        if (status) {
            params.append('status', status);
        }
        
        // 跳转到搜索结果
        const url = '/admin/wallet' + (params.toString() ? '?' + params.toString() : '');
        window.location.href = url;
    };

    // 清空搜索
    window.clearSearch = function() {
        userSearch.value = '';
        statusFilter.value = '';
        window.location.href = '/admin/wallet';
    };

    // 刷新钱包列表
    window.refreshWalletList = function() {
        window.location.reload();
    };

    // 分页处理
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');

    if (prevBtn) {
        prevBtn.addEventListener('click', function() {
            changePage(-1);
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener('click', function() {
            changePage(1);
        });
    }

    function changePage(direction) {
        const url = new URL(window.location);
        const currentPage = parseInt(url.searchParams.get('page') || '0');
        const newPage = Math.max(0, currentPage + direction);
        url.searchParams.set('page', newPage);
        window.location.href = url.toString();
    }

    // 回车键搜索
    if (userSearch) {
        userSearch.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                handleSearch();
            }
        });
    }

    // 状态筛选变化时自动搜索
    if (statusFilter) {
        statusFilter.addEventListener('change', function() {
            handleSearch();
        });
    }

    // 表格行点击效果
    const tableRows = document.querySelectorAll('.data-table tbody tr');
    tableRows.forEach(row => {
        row.addEventListener('click', function(e) {
            // 如果点击的是按钮或链接，不触发行点击
            if (e.target.closest('button') || e.target.closest('a')) {
                return;
            }
            
            // 高亮选中行
            tableRows.forEach(r => r.classList.remove('selected'));
            this.classList.add('selected');
        });
    });

    // 金额格式化显示
    const amountElements = document.querySelectorAll('.amount');
    amountElements.forEach(element => {
        const value = parseFloat(element.textContent);
        if (value < 0) {
            element.style.color = '#e74c3c';
        } else if (value > 0) {
            element.style.color = '#27ae60';
        } else {
            element.style.color = '#6c757d';
        }
    });

    // 状态标签动画
    const statusBadges = document.querySelectorAll('.status-badge');
    statusBadges.forEach(badge => {
        badge.addEventListener('mouseenter', function() {
            this.style.transform = 'scale(1.05)';
        });
        
        badge.addEventListener('mouseleave', function() {
            this.style.transform = 'scale(1)';
        });
    });

    // 用户链接点击统计
    const userLinks = document.querySelectorAll('.user-link');
    userLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            // 可以在这里添加点击统计逻辑
            console.log('用户链接被点击:', this.textContent);
        });
    });

    // 表格排序功能（可选）
    const tableHeaders = document.querySelectorAll('.data-table th');
    tableHeaders.forEach((header, index) => {
        // 排除操作列
        if (index < tableHeaders.length - 1) {
            header.style.cursor = 'pointer';
            header.addEventListener('click', function() {
                // 这里可以添加排序逻辑
                console.log('点击了列头:', this.textContent);
            });
        }
    });

    // 页面加载完成后的初始化
    console.log('钱包列表页面加载完成');
});
