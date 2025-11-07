// 登录页面交互逻辑
document.addEventListener('DOMContentLoaded', function () {
    const loginForm = document.getElementById('loginForm');
    const loginBtn = document.getElementById('loginBtn');
    const btnText = document.querySelector('.btn-text');
    const btnLoading = document.querySelector('.btn-loading');
    const errorMessage = document.getElementById('errorMessage');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const captchaInput = document.getElementById('captcha');
    const captchaImage = document.getElementById('captchaImage');

    // 页面加载动画
    document.querySelector('.login-box').classList.add('fade-in');

    // 表单提交处理
    loginForm.addEventListener('submit', async function (e) {
        e.preventDefault();

        const username = usernameInput.value.trim();
        const password = passwordInput.value;
        const captcha = captchaInput.value.trim();

        // 基本验证
        if (!username || !password || !captcha) {
            showError('请输入用户名、密码和验证码');
            return;
        }

        if (username.length < 3) {
            showError('用户名至少需要3个字符');
            return;
        }

        if (password.length < 6) {
            showError('密码至少需要6个字符');
            return;
        }

        if (captcha.length !== 4) {
            showError('验证码必须是4位数字');
            return;
        }

        // 显示加载状态
        setLoadingState(true);
        hideError();

        try {
            // 这里应该调用后端登录接口
            // 暂时使用模拟请求
            fetch('/admin/login', {
                method: 'POST',
                body: new URLSearchParams({
                    username: username,
                    password: password,
                    captcha: captcha
                })
            }).then(res => res.json()).then(data => {
                if (data.code === "1") {
                    // 登录成功
                    showSuccess('登录成功，正在跳转...');

                    // 延迟跳转到主页面
                    setTimeout(() => {
                        window.location.href = '/';
                    }, 1000);
                } else {
                    showError(data.code || '登录失败，请检查用户名和密码');
                }
            })
        } catch (error) {
            console.error('登录错误:', error);
            showError('网络错误，请稍后重试');
        } finally {
            setLoadingState(false);
        }
    });

    // 设置加载状态
    function setLoadingState(loading) {
        loginBtn.disabled = loading;
        if (loading) {
            loginBtn.classList.add('loading');
        } else {
            loginBtn.classList.remove('loading');
        }
    }

    // 显示错误信息
    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
        errorMessage.scrollIntoView({behavior: 'smooth', block: 'nearest'});
    }

    // 隐藏错误信息
    function hideError() {
        errorMessage.style.display = 'none';
    }

    // 显示成功信息
    function showSuccess(message) {
        errorMessage.style.background = '#dfe';
        errorMessage.style.color = '#363';
        errorMessage.style.borderLeftColor = '#363';
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
    }

    // 刷新验证码
    function refreshCaptcha() {
        const captchaImage = document.getElementById('captchaImage');
        
        // 清空容器并创建新的placeholder
        captchaImage.innerHTML = '<span class="captcha-placeholder">加载中...</span>';
        const placeholder = captchaImage.querySelector('.captcha-placeholder');
        placeholder.style.fontSize = '12px';
        placeholder.style.color = '#999';

        fetch("/captcha")
            .then(response => response.json())
            .then(data => {
                if (data.code !== "1") {
                    // 获取验证码失败
                    placeholder.textContent = '加载失败，点击重试';
                    placeholder.style.color = '#f44336';
                } else {
                    // 成功获取验证码，显示图片
                    const img = document.createElement('img');
                    img.src = 'data:image/png;base64,' + data.data;
                    img.style.width = '100%';
                    img.style.height = '100%';
                    img.style.objectFit = 'cover';
                    img.style.borderRadius = '6px';
                    
                    // 替换为图片
                    captchaImage.innerHTML = '';
                    captchaImage.appendChild(img);
                    
                    // 清除验证码输入框
                    captchaInput.value = '';
                    captchaInput.focus();
                }
            })
            .catch(error => {
                console.error('获取验证码失败:', error);
                placeholder.textContent = '加载失败，点击重试';
                placeholder.style.color = '#f44336';
            });
    }

    // 全局函数，供HTML调用
    window.refreshCaptcha = refreshCaptcha;

    // 键盘事件处理
    document.addEventListener('keydown', function (e) {
        // Enter键提交表单
        if (e.key === 'Enter' && !loginBtn.disabled) {
            loginForm.dispatchEvent(new Event('submit'));
        }

        // Escape键清除错误信息
        if (e.key === 'Escape') {
            hideError();
        }
    });

    // 输入框实时验证
    usernameInput.addEventListener('input', function () {
        if (this.value.trim().length >= 3) {
            this.style.borderColor = '#4caf50';
        } else {
            this.style.borderColor = '#e1e5e9';
        }
    });

    passwordInput.addEventListener('input', function () {
        if (this.value.length >= 6) {
            this.style.borderColor = '#4caf50';
        } else {
            this.style.borderColor = '#e1e5e9';
        }
    });

    // 页面加载时初始化验证码
    refreshCaptcha();

    // 自动填充测试数据（开发环境）
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        // 双击页面标题可以自动填充测试数据
        document.querySelector('.login-header h1').addEventListener('dblclick', function () {
            usernameInput.value = 'admin';
            passwordInput.value = 'admin123';
            captchaInput.value = '1234';
            showError('已填充测试数据：admin / admin123 / 1234');
            setTimeout(hideError, 3000);
        });
    }
});
