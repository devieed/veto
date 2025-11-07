// 错误页面交互逻辑
document.addEventListener('DOMContentLoaded', function () {
    // 返回上一页
    window.goBack = function() {
        if (window.history.length > 1) {
            window.history.back();
        } else {
            // 如果没有历史记录，跳转到首页
            window.location.href = '/admin';
        }
    };

    // 返回首页
    window.goHome = function() {
        window.location.href = '/admin';
    };

    // 键盘快捷键支持
    document.addEventListener('keydown', function(e) {
        // ESC 键返回上一页
        if (e.key === 'Escape') {
            goBack();
        }
        
        // Enter 键返回首页
        if (e.key === 'Enter' && e.ctrlKey) {
            goHome();
        }
    });

    // 添加页面加载动画
    const container = document.querySelector('.error-container');
    if (container) {
        container.style.opacity = '0';
        container.style.transform = 'translateY(30px)';
        
        setTimeout(() => {
            container.style.transition = 'all 0.6s ease-out';
            container.style.opacity = '1';
            container.style.transform = 'translateY(0)';
        }, 100);
    }

    // 自动聚焦到返回按钮（可访问性）
    const backButton = document.querySelector('.btn-primary');
    if (backButton) {
        backButton.focus();
    }
});
