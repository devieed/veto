// 通用布局交互逻辑
document.addEventListener('DOMContentLoaded', function () {
    const sidebar = document.querySelector('.sidebar');
    const sidebarToggle = document.getElementById('sidebarToggle');
    const menuToggle = document.getElementById('menuToggle');
    const userMenuBtn = document.getElementById('userMenuBtn');
    const userDropdown = document.getElementById('userDropdown');

    // 检查必要元素是否存在
    if (!sidebar) {
        console.warn('Sidebar element not found');
        return;
    }

    // 侧边栏切换
    function toggleSidebar() {
        if (sidebar) {
            sidebar.classList.toggle('collapsed');
            localStorage.setItem('sidebarCollapsed', sidebar.classList.contains('collapsed'));
        }
    }

    // 初始化侧边栏状态
    const sidebarCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
    if (sidebar && sidebarCollapsed) {
        sidebar.classList.add('collapsed');
    }

    // 事件监听
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', toggleSidebar);
    }
    if (menuToggle) {
        menuToggle.addEventListener('click', toggleSidebar);
    }

    // 用户菜单切换
    if (userMenuBtn) {
        userMenuBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            userDropdown.classList.toggle('show');
        });
    }

    // 点击其他地方关闭用户菜单
    document.addEventListener('click', function () {
        if (userDropdown) {
            userDropdown.classList.remove('show');
        }
    });

    // 用户菜单操作
    document.querySelectorAll('.user-dropdown a').forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            const action = this.getAttribute('data-action');
            
            switch (action) {
                case 'profile':
                    showNotification('个人信息功能开发中...', 'info');
                    break;
                case 'settings':
                    showNotification('设置功能开发中...', 'info');
                    break;
                case 'logout':
                    handleLogout();
                    break;
            }
            
            if (userDropdown) {
                userDropdown.classList.remove('show');
            }
        });
    });

    // 导航菜单处理
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', function (e) {
            const submenuId = this.getAttribute('data-submenu');
            const pageId = this.getAttribute('data-page');
            
            if (submenuId) {
                e.preventDefault();
                // 处理有子菜单的项目
                const navItem = this.closest('.nav-item');
                const isActive = navItem.classList.contains('active');
                
                // 关闭所有其他菜单
                document.querySelectorAll('.nav-item').forEach(item => {
                    item.classList.remove('active');
                    const arrow = item.querySelector('.submenu-arrow');
                    if (arrow) {
                        arrow.classList.remove('fa-chevron-down');
                        arrow.classList.add('fa-chevron-right');
                    }
                    const submenu = item.querySelector('.submenu');
                    if (submenu) {
                        submenu.style.maxHeight = '0';
                    }
                });
                
                // 切换当前菜单
                if (!isActive) {
                    navItem.classList.add('active');
                    const arrow = this.querySelector('.submenu-arrow');
                    if (arrow) {
                        arrow.classList.remove('fa-chevron-right');
                        arrow.classList.add('fa-chevron-down');
                    }
                    const submenu = navItem.querySelector('.submenu');
                    if (submenu) {
                        submenu.style.maxHeight = '200px';
                    }
                }
            } else if (pageId) {
                // 处理直接页面
                e.preventDefault();
                loadPage(pageId);
            }
            // 如果没有submenuId和pageId，让链接正常跳转
        });
    });

    // 子菜单点击处理
    document.querySelectorAll('.submenu a').forEach(link => {
        link.addEventListener('click', function (e) {
            const pageId = this.getAttribute('data-page');
            if (pageId) {
                e.preventDefault();
                loadPage(pageId);
                
                // 更新活动状态
                document.querySelectorAll('.nav-link, .submenu a').forEach(l => {
                    l.classList.remove('active');
                });
                this.classList.add('active');
            }
            // 如果没有data-page属性，让链接正常跳转
        });
    });

    // 加载页面 - 跳转到对应页面
    function loadPage(pageId) {
        const pageUrls = {
            'user-list': '/user/get',
            'user-wallet': '/wallet/get',
            'event-list': '/admin/event-list',
            'betting-orders': '/admin/betting-orders',
            'team-management': '/team/get',
            'config': '/system_properties',
            'wallet': '/admin/wallet',
            'debug': '/admin/debug',
            'release': '/admin/release'
        };
        
        const url = pageUrls[pageId];
        if (url) {
            window.location.href = url;
        } else {
            if (window.showNotification) {
                window.showNotification('页面不存在', 'error');
            }
        }
    }

    // 处理退出登录
    function handleLogout() {
        if (confirm('确定要退出登录吗？')) {
            fetch('/admin/logout', {
                method: 'POST'
            }).then(() => {
                window.location.href = '/login';
            }).catch(() => {
                // 即使请求失败也跳转到登录页
                window.location.href = '/login';
            });
        }
    }

    // 显示通知
    function showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <i class="fas fa-${getNotificationIcon(type)}"></i>
            <span>${message}</span>
        `;
        
        // 添加样式
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: ${getNotificationColor(type)};
            color: white;
            padding: 15px 20px;
            border-radius: 8px;
            box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
            z-index: 10000;
            display: flex;
            align-items: center;
            gap: 10px;
            transform: translateX(100%);
            transition: transform 0.3s ease;
        `;
        
        document.body.appendChild(notification);
        
        // 显示动画
        setTimeout(() => {
            notification.style.transform = 'translateX(0)';
        }, 100);
        
        // 自动隐藏
        setTimeout(() => {
            notification.style.transform = 'translateX(100%)';
            setTimeout(() => {
                if (document.body.contains(notification)) {
                    document.body.removeChild(notification);
                }
            }, 300);
        }, 3000);
    }

    // 获取通知图标
    function getNotificationIcon(type) {
        const icons = {
            'success': 'check-circle',
            'error': 'exclamation-circle',
            'warning': 'exclamation-triangle',
            'info': 'info-circle'
        };
        return icons[type] || 'info-circle';
    }

    // 获取通知颜色
    function getNotificationColor(type) {
        const colors = {
            'success': '#27ae60',
            'error': '#e74c3c',
            'warning': '#f39c12',
            'info': '#3498db'
        };
        return colors[type] || '#3498db';
    }

    // 响应式处理
    function handleResize() {
        if (window.innerWidth <= 768) {
            sidebar.classList.add('collapsed');
        } else {
            const shouldCollapse = localStorage.getItem('sidebarCollapsed') === 'true';
            sidebar.classList.toggle('collapsed', shouldCollapse);
        }
    }

    // 处理窗口大小变化
    window.addEventListener('resize', handleResize);
    handleResize();

    // 全局暴露函数
    window.showNotification = showNotification;
    window.handleLogout = handleLogout;
});
