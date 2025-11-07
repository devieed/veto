// 球队列表页面交互逻辑
document.addEventListener('DOMContentLoaded', function () {
    const teamSearch = document.getElementById('teamSearch');
    const statusFilter = document.getElementById('statusFilter');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');

    // 搜索处理
    window.handleSearch = function() {
        const keyword = teamSearch.value.trim();
        const status = statusFilter.value;
        
        // 构建查询参数
        const params = new URLSearchParams();
        if (keyword) {
            params.append('search', keyword);
        }
        if (status) {
            params.append('status', status);
        }
        
        // 跳转到搜索结果
        const url = '/admin/teams' + (params.toString() ? '?' + params.toString() : '');
        window.location.href = url;
    };

    // 清空搜索
    window.clearSearch = function() {
        teamSearch.value = '';
        statusFilter.value = '';
        window.location.href = '/admin/teams';
    };

    // 刷新球队列表
    window.refreshTeamList = function() {
        window.location.reload();
    };

    // 添加球队
    window.addTeam = function() {
        window.location.href = '/admin/teams/add';
    };

    // 分页处理
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
        const currentPage = parseInt(url.searchParams.get('page') || '1');
        const newPage = Math.max(0, currentPage + direction);
        url.searchParams.set('page', newPage);
        window.location.href = url.toString();
    }

    // 回车键搜索
    if (teamSearch) {
        teamSearch.addEventListener('keypress', function(e) {
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
            // 如果点击的是按钮，不触发行点击
            if (e.target.closest('button')) {
                return;
            }
            
            // 高亮选中行
            tableRows.forEach(r => r.classList.remove('selected'));
            this.classList.add('selected');
        });
    });

    // 球队图标点击放大
    const teamLogos = document.querySelectorAll('.logo-image');
    teamLogos.forEach(logo => {
        logo.addEventListener('click', function(e) {
            e.stopPropagation();
            showImageModal(this.src, this.alt);
        });
    });

    // 图片模态框
    function showImageModal(src, alt) {
        const modal = document.createElement('div');
        modal.className = 'image-modal';
        modal.innerHTML = `
            <div class="modal-overlay" onclick="closeImageModal()">
                <div class="modal-content" onclick="event.stopPropagation()">
                    <button class="modal-close" onclick="closeImageModal()">
                        <i class="fas fa-times"></i>
                    </button>
                    <img src="${src}" alt="${alt}" class="modal-image">
                    <div class="modal-caption">${alt}</div>
                </div>
            </div>
        `;
        
        // 添加样式
        modal.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 10000;
            display: flex;
            align-items: center;
            justify-content: center;
        `;
        
        const style = document.createElement('style');
        style.textContent = `
            .image-modal .modal-overlay {
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.8);
                display: flex;
                align-items: center;
                justify-content: center;
                animation: fadeIn 0.3s ease;
            }
            
            .image-modal .modal-content {
                position: relative;
                max-width: 90vw;
                max-height: 90vh;
                background: white;
                border-radius: 12px;
                padding: 20px;
                animation: scaleIn 0.3s ease;
            }
            
            .image-modal .modal-close {
                position: absolute;
                top: 10px;
                right: 10px;
                width: 40px;
                height: 40px;
                border-radius: 50%;
                background: #dc3545;
                color: white;
                border: none;
                cursor: pointer;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 16px;
                z-index: 10001;
            }
            
            .image-modal .modal-image {
                max-width: 100%;
                max-height: 70vh;
                border-radius: 8px;
                display: block;
            }
            
            .image-modal .modal-caption {
                text-align: center;
                margin-top: 15px;
                font-size: 16px;
                color: #2c3e50;
                font-weight: 600;
            }
            
            @keyframes fadeIn {
                from { opacity: 0; }
                to { opacity: 1; }
            }
            
            @keyframes scaleIn {
                from { transform: scale(0.8); opacity: 0; }
                to { transform: scale(1); opacity: 1; }
            }
        `;
        
        document.head.appendChild(style);
        document.body.appendChild(modal);
        
        // 全局函数
        window.closeImageModal = function() {
            document.body.removeChild(modal);
            document.head.removeChild(style);
            delete window.closeImageModal;
        };
        
        // ESC键关闭
        const handleEsc = function(e) {
            if (e.key === 'Escape') {
                closeImageModal();
                document.removeEventListener('keydown', handleEsc);
            }
        };
        document.addEventListener('keydown', handleEsc);
    }

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
    console.log('球队列表页面加载完成');
});
