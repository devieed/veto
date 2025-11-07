// 仪表板首页特定逻辑
document.addEventListener('DOMContentLoaded', function () {
    // 加载统计数据
    function loadStats() {
        // 动画更新数字
        animateNumber('totalUsers', document.getElementById("totalUsers").textContent);
        animateNumber('totalEvents', document.getElementById("totalEvents").textContent);
        animateNumber('totalTeams', document.getElementById("totalTeams").textContent);
        animateNumber('todayBets', document.getElementById("todayBets").textContent);
    }

    // 数字动画
    function animateNumber(elementId, targetValue) {
        const element = document.getElementById(elementId);
        if (!element) return;
        
        const startValue = 0;
        const duration = 2000;
        const startTime = performance.now();
        
        function updateNumber(currentTime) {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            
            const currentValue = Math.floor(startValue + (targetValue - startValue) * progress);
            element.textContent = currentValue.toLocaleString();
            
            if (progress < 1) {
                requestAnimationFrame(updateNumber);
            }
        }
        
        requestAnimationFrame(updateNumber);
    }

    // 初始化
    function init() {
        // 加载统计数据
        loadStats();
        
        // 页面加载动画
        const welcomeCard = document.querySelector('.dashboard-welcome');
        if (welcomeCard) {
            welcomeCard.classList.add('fade-in');
        }
        
        // 显示欢迎通知
        if (window.showNotification) {
            window.showNotification('欢迎使用 Veto 管理后台', 'success');
        }
    }

    // 启动应用
    init();
});