// 用户编辑页面交互逻辑
document.addEventListener('DOMContentLoaded', function () {
    // 显示加载状态
    function showLoading() {
        const submitBtn = editUserForm.querySelector('button[type="submit"]');
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 保存中...';
        submitBtn.disabled = true;
        
        // 存储原始文本以便恢复
        submitBtn.dataset.originalText = originalText;
    }

    // 隐藏加载状态
    function hideLoading() {
        const submitBtn = editUserForm.querySelector('button[type="submit"]');
        submitBtn.innerHTML = submitBtn.dataset.originalText;
        submitBtn.disabled = false;
    }


});

// 切换密码显示/隐藏
function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    const button = input.parentElement.querySelector('.btn-toggle-password');
    const icon = button.querySelector('i');
    
    if (input.type === 'password') {
        input.type = 'text';
        icon.className = 'fas fa-eye-slash';
    } else {
        input.type = 'password';
        icon.className = 'fas fa-eye';
    }
}

// 返回上一页
function goBack() {
    window.history.back();
}

// 全局暴露函数
window.togglePassword = togglePassword;
window.goBack = goBack;
