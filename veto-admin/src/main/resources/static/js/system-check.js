// 系统启动检查JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // 检查是否存在系统检查模态框
    const systemCheckModal = document.querySelector('.system-check-modal');
    if (!systemCheckModal) {
        return;
    }

    // 初始化表单验证
    initializeFormValidation();
    
    // 添加键盘事件监听
    document.addEventListener('keydown', function(e) {
        // 阻止ESC键关闭模态框
        if (e.key === 'Escape') {
            e.preventDefault();
            e.stopPropagation();
        }
    });
    
    // 阻止点击模态框外部关闭
    const modalOverlay = document.querySelector('.modal-overlay');
    if (modalOverlay) {
        modalOverlay.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
        });
    }
});

// 初始化表单验证
function initializeFormValidation() {
    const form = document.getElementById('systemCheckForm');
    if (!form) return;
    
    const inputs = form.querySelectorAll('input, select');
    inputs.forEach(input => {
        // 添加实时验证
        input.addEventListener('input', function() {
            validateField(this);
        });
        
        input.addEventListener('blur', function() {
            validateField(this);
        });
    });
}

// 验证单个字段
function validateField(field) {
    const value = field.value.trim();
    const isValid = validateFieldValue(field, value);
    
    // 更新字段样式
    field.classList.remove('error', 'success');
    if (value.length > 0) {
        field.classList.add(isValid ? 'success' : 'error');
    }
    
    return isValid;
}

// 验证字段值
function validateFieldValue(field, value) {
    if (!value) return false;
    
    switch (field.type) {
        case 'url':
            return isValidUrl(value);
        case 'text':
            return value.length >= 2;
        default:
            return value.length > 0;
    }
}

// 验证URL格式
function isValidUrl(string) {
    try {
        new URL(string);
        return true;
    } catch (_) {
        return false;
    }
}

// 验证整个表单
function validateForm() {
    const form = document.getElementById('systemCheckForm');
    if (!form) return false;
    
    const inputs = form.querySelectorAll('input[required], select[required]');
    let isValid = true;
    
    inputs.forEach(input => {
        const fieldValid = validateField(input);
        if (!fieldValid) {
            isValid = false;
        }
    });
    
    return isValid;
}

// 提交系统检查
window.submitSystemCheck = function(event) {
    console.log('submitSystemCheck 被调用', event);
    
    // 阻止默认的表单提交行为
    if (event) {
        event.preventDefault();
    }
    
    const submitBtn = document.getElementById('submitSystemCheck');
    const modalContent = document.querySelector('.modal-content');
    
    console.log('提交按钮:', submitBtn);
    console.log('模态框内容:', modalContent);
    
    // 验证表单
    if (!validateForm()) {
        console.log('表单验证失败');
        showError('请填写所有必填项');
        return false;
    }
    
    console.log('表单验证通过');
    
    // 显示加载状态
    setLoadingState(true);
    
    // 收集表单数据
    const formData = collectFormData();
    if ('alchemyAuthToken' in formData){// 要更新钱包
        fetch('/init_system_two', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(formData),
        }).then(response => response.json()).then(data => {
            if (data.code === "1"){
                showSuccess('系统配置完成，正在重新加载...');
                setTimeout(() => {
                    window.location.reload();
                }, 2000);
            }else {
                showError(data.code || '配置保存失败，请重试');
                setLoadingState(false);
            }
        }).catch(error => {
            console.error('系统检查提交失败:', error);
            showError('网络错误，请检查连接后重试');
            setLoadingState(false);
        });
    }else {
        // 发送请求
        fetch('/init_system_one', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(formData),
        })
            .then(response => {
                console.log('收到响应:', response);
                return response.json();
            })
            .then(data => {
                console.log('响应数据:', data);
                if (data.code === '1') {
                    // 成功处理
                    showSuccess('系统配置完成，正在重新加载...');
                    setTimeout(() => {
                        window.location.reload();
                    }, 2000);
                } else {
                    showError(data.code || '配置保存失败，请重试');
                    setLoadingState(false);
                }
            })
            .catch(error => {
                console.error('系统检查提交失败:', error);
                showError('网络错误，请检查连接后重试');
                setLoadingState(false);
            });
    }
    
    return false;
};

// 收集表单数据
function collectFormData() {
    const form = document.getElementById('systemCheckForm');
    const formData = {};
    
    const inputs = form.querySelectorAll('input, select');
    inputs.forEach(input => {
        if (input.name && input.value) {
            formData[input.name] = input.value.trim();
        }
    });
    
    console.log('收集到的表单数据:', formData);
    return formData;
}

// 设置加载状态
function setLoadingState(loading) {
    const submitBtn = document.getElementById('submitSystemCheck');
    const modalContent = document.querySelector('.modal-content');
    
    if (loading) {
        submitBtn.disabled = true;
        submitBtn.classList.add('loading');
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 配置中...';
        modalContent.classList.add('loading');
    } else {
        submitBtn.disabled = false;
        submitBtn.classList.remove('loading');
        submitBtn.innerHTML = '<i class="fas fa-check"></i> 完成配置';
        modalContent.classList.remove('loading');
    }
}

// 显示成功消息
function showSuccess(message) {
    const modalContent = document.querySelector('.modal-content');
    modalContent.classList.add('success');
    
    // 更新按钮文本
    const submitBtn = document.getElementById('submitSystemCheck');
    submitBtn.innerHTML = '<i class="fas fa-check"></i> 配置完成';
    submitBtn.disabled = true;
    
    // 显示成功提示
    showNotification(message, 'success');
}

// 显示错误消息
function showError(message) {
    showNotification(message, 'error');
}

// 显示通知
function showNotification(message, type) {
    // 移除现有通知
    const existingNotification = document.querySelector('.system-check-notification');
    if (existingNotification) {
        existingNotification.remove();
    }
    
    // 创建新通知
    const notification = document.createElement('div');
    notification.className = `system-check-notification ${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'}"></i>
            <span>${message}</span>
        </div>
    `;
    
    // 添加样式
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${type === 'success' ? '#28a745' : '#dc3545'};
        color: white;
        padding: 16px 20px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        z-index: 10001;
        animation: slideIn 0.3s ease;
        max-width: 400px;
    `;
    
    document.body.appendChild(notification);
    
    // 自动移除
    setTimeout(() => {
        if (notification.parentNode) {
            notification.remove();
        }
    }, 5000);
}

// 添加CSS动画
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    .system-check-notification {
        display: flex;
        align-items: center;
        gap: 12px;
    }
    
    .system-check-notification i {
        font-size: 18px;
    }
`;
document.head.appendChild(style);
