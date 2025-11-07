// Debug页面交互逻辑 - 高度安全的私密信息展示
document.addEventListener('DOMContentLoaded', function () {
    const passwordVerification = document.getElementById('passwordVerification');
    const walletInfoContainer = document.getElementById('walletInfoContainer');
    const verificationForm = document.getElementById('verificationForm');
    const adminPassword = document.getElementById('adminPassword');
    const verifyBtn = document.getElementById('verifyBtn');
    
    let isAuthenticated = false;
    let sessionTimeout = null;
    const SESSION_DURATION = 10 * 60 * 1000; // 10分钟会话超时

    // 密码验证
    window.verifyPassword = function(event) {
        event.preventDefault();
        
        const password = adminPassword.value.trim();
        if (!password) {
            if (window.showNotification) {
                window.showNotification('请输入管理员密码', 'error');
            }
            return;
        }
        
        showLoading();

        adminPassword.value = '';
        adminPassword.focus();


        if (password != document.getElementById('currentPassword').value) {
            window.showNotification('密码错误', 'error');
        }else {
            isAuthenticated = true;
            passwordVerification.style.display = 'none';
            walletInfoContainer.style.display = 'block';

            // 启动会话超时
            startSessionTimeout();

            if (window.showNotification) {
                window.showNotification('身份验证成功', 'success');
            }
        }
        hideLoading();
    };

    // 显示/隐藏私密信息
    window.toggleVisibility = function(itemId) {
        if (!isAuthenticated) {
            if (window.showNotification) {
                window.showNotification('请先验证身份', 'error');
            }
            return;
        }
        
        const content = document.getElementById(itemId + 'Content');
        const toggle = document.getElementById(itemId + 'Toggle');
        const hiddenPlaceholder = content.querySelector('.hidden-placeholder');
        const visibleContent = content.querySelector('.visible-content');
        
        if (hiddenPlaceholder.style.display !== 'none') {
            // 显示内容
            hiddenPlaceholder.style.display = 'none';
            visibleContent.style.display = 'block';
            toggle.innerHTML = '<i class="fas fa-eye-slash"></i> 隐藏';
            toggle.classList.remove('btn-secondary');
            toggle.classList.add('btn-danger');
            
            // 记录查看日志
            logAccess('SENSITIVE_INFO_VIEW', itemId);
        } else {
            // 隐藏内容
            hiddenPlaceholder.style.display = 'flex';
            visibleContent.style.display = 'none';
            toggle.innerHTML = '<i class="fas fa-eye"></i> 显示';
            toggle.classList.remove('btn-danger');
            toggle.classList.add('btn-secondary');
        }
    };

    // 复制到剪贴板
    window.copyToClipboard = function(itemId) {
        if (!isAuthenticated) {
            if (window.showNotification) {
                window.showNotification('请先验证身份', 'error');
            }
            return;
        }
        
        const content = document.getElementById(itemId + 'Content');
        const visibleContent = content.querySelector('.visible-content');
        
        if (visibleContent.style.display === 'none') {
            if (window.showNotification) {
                window.showNotification('请先显示内容', 'warning');
            }
            return;
        }
        
        const textElement = visibleContent.querySelector('.mnemonic-words, .key-value, .address-value');
        if (!textElement) {
            if (window.showNotification) {
                window.showNotification('没有可复制的内容', 'error');
            }
            return;
        }
        
        const text = textElement.textContent.trim();
        
        // 使用现代剪贴板API
        if (navigator.clipboard && window.isSecureContext) {
            navigator.clipboard.writeText(text).then(() => {
                showCopySuccess();
                logAccess('SENSITIVE_INFO_COPY', itemId);
            }).catch(err => {
                console.error('复制失败:', err);
                fallbackCopyTextToClipboard(text);
            });
        } else {
            fallbackCopyTextToClipboard(text);
        }
    };

    // 备用复制方法
    function fallbackCopyTextToClipboard(text) {
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        textArea.style.top = '-999999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        
        try {
            const successful = document.execCommand('copy');
            if (successful) {
                showCopySuccess();
                logAccess('SENSITIVE_INFO_COPY', 'fallback');
            } else {
                throw new Error('复制命令失败');
            }
        } catch (err) {
            console.error('复制失败:', err);
            if (window.showNotification) {
                window.showNotification('复制失败，请手动复制', 'error');
            }
        } finally {
            document.body.removeChild(textArea);
        }
    }

    // 显示复制成功提示
    function showCopySuccess() {
        const notification = document.createElement('div');
        notification.className = 'copy-success';
        notification.innerHTML = '<i class="fas fa-check"></i> 已复制到剪贴板';
        document.body.appendChild(notification);
        
        setTimeout(() => {
            if (document.body.contains(notification)) {
                document.body.removeChild(notification);
            }
        }, 2000);
    }

    // 隐藏所有私密信息
    window.hideAllSensitiveInfo = function() {
        const sensitiveItems = ['mnemonic', 'privateKey', 'publicKey', 'address'];
        sensitiveItems.forEach(itemId => {
            const content = document.getElementById(itemId + 'Content');
            const toggle = document.getElementById(itemId + 'Toggle');
            const hiddenPlaceholder = content.querySelector('.hidden-placeholder');
            const visibleContent = content.querySelector('.visible-content');
            
            hiddenPlaceholder.style.display = 'flex';
            visibleContent.style.display = 'none';
            toggle.innerHTML = '<i class="fas fa-eye"></i> 显示';
            toggle.classList.remove('btn-danger');
            toggle.classList.add('btn-secondary');
        });
        
        if (window.showNotification) {
            window.showNotification('所有私密信息已隐藏', 'success');
        }
    };

    // 锁定页面
    window.lockDebugPage = function() {
        if (confirm('确定要锁定页面吗？锁定后需要重新验证身份才能查看私密信息。')) {
            isAuthenticated = false;
            passwordVerification.style.display = 'block';
            walletInfoContainer.style.display = 'none';
            
            // 清空密码输入
            adminPassword.value = '';
            
            // 清除会话超时
            if (sessionTimeout) {
                clearTimeout(sessionTimeout);
                sessionTimeout = null;
            }
            
            // 隐藏所有私密信息
            hideAllSensitiveInfo();
            
            if (window.showNotification) {
                window.showNotification('页面已锁定', 'success');
            }
        }
    };

    // 刷新调试信息
    window.refreshDebugInfo = function() {
        if (!isAuthenticated) {
            if (window.showNotification) {
                window.showNotification('请先验证身份', 'error');
            }
            return;
        }
        
        window.location.reload();
    };

    // 密码显示/隐藏切换
    window.togglePasswordVisibility = function(inputId) {
        const input = document.getElementById(inputId);
        const button = input.nextElementSibling;
        const icon = button.querySelector('i');
        
        if (input.type === 'password') {
            input.type = 'text';
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
        } else {
            input.type = 'password';
            icon.classList.remove('fa-eye-slash');
            icon.classList.add('fa-eye');
        }
    };

    // 显示加载状态
    function showLoading() {
        verifyBtn.classList.add('loading');
        verifyBtn.disabled = true;
    }

    // 隐藏加载状态
    function hideLoading() {
        verifyBtn.classList.remove('loading');
        verifyBtn.disabled = false;
    }

    // 启动会话超时
    function startSessionTimeout() {
        if (sessionTimeout) {
            clearTimeout(sessionTimeout);
        }
        
        sessionTimeout = setTimeout(() => {
            if (isAuthenticated) {
                lockDebugPage();
                if (window.showNotification) {
                    window.showNotification('会话已超时，页面已自动锁定', 'warning');
                }
            }
        }, SESSION_DURATION);
    }

    // 记录访问日志
    function logAccess(action, details = '') {
        const logData = {
            action: action,
            details: details,
            timestamp: new Date().toISOString(),
            userAgent: navigator.userAgent,
            url: window.location.href
        };
        
        // 发送日志到服务器
        fetch('/admin/debug/log', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(logData)
        }).catch(err => {
            console.error('日志记录失败:', err);
        });
    }

    // 页面离开确认
    window.addEventListener('beforeunload', function(e) {
        if (isAuthenticated) {
            e.preventDefault();
            e.returnValue = '您正在查看私密信息，确定要离开吗？';
            return e.returnValue;
        }
    });

    // 键盘快捷键
    document.addEventListener('keydown', function(e) {
        // ESC键锁定页面
        if (e.key === 'Escape' && isAuthenticated) {
            lockDebugPage();
        }
        
        // Ctrl+L锁定页面
        if (e.ctrlKey && e.key === 'l' && isAuthenticated) {
            e.preventDefault();
            lockDebugPage();
        }
    });

    // 检测页面可见性变化
    document.addEventListener('visibilitychange', function() {
        if (document.hidden && isAuthenticated) {
            // 页面隐藏时自动锁定
            setTimeout(() => {
                if (document.hidden && isAuthenticated) {
                    lockDebugPage();
                    if (window.showNotification) {
                        window.showNotification('页面已隐藏，自动锁定保护', 'warning');
                    }
                }
            }, 30000); // 30秒后锁定
        }
    });

    // 检测开发者工具
    let devtools = false;
    setInterval(() => {
        if (isAuthenticated) {
            if (window.outerHeight - window.innerHeight > 200 || window.outerWidth - window.innerWidth > 200) {
                if (!devtools) {
                    devtools = true;
                    if (window.showNotification) {
                        window.showNotification('检测到开发者工具，请注意安全', 'warning');
                    }
                    logAccess('DEVTOOLS_DETECTED');
                }
            } else {
                devtools = false;
            }
        }
    }, 1000);

    // 页面加载完成后的初始化
    console.log('Debug页面加载完成 - 安全模式已启用');
});
