// 钱包编辑页面交互逻辑
document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('walletEditForm');
    const submitBtn = document.getElementById('submitBtn');
    const balanceInput = document.getElementById('balance');
    const statusSelect = document.getElementById('status');

    // 表单提交处理
    window.submitForm = function(event) {
        event.preventDefault();
        
        // 验证表单
        if (!validateForm()) {
            return;
        }
        
        // 显示加载状态
        showLoading();
        
        // 收集表单数据
        const formData = {
            balance: parseFloat(balanceInput.value),
            status: statusSelect.value
        };
        
        // 发送更新请求
        fetch('/wallet/update', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(formData)
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('网络请求失败');
            }
            return response.json();
        })
        .then(data => {
            hideLoading();
            if (data.success) {
                if (window.showNotification) {
                    window.showNotification('钱包更新成功', 'success');
                }
                // 延迟跳转，让用户看到成功消息
                setTimeout(() => {
                    goBack();
                }, 1500);
            } else {
                throw new Error(data.message || '更新失败');
            }
        })
        .catch(error => {
            hideLoading();
            console.error('更新钱包失败:', error);
            if (window.showNotification) {
                window.showNotification(error.message || '更新失败，请重试', 'error');
            }
        });
    };

    // 表单验证
    function validateForm() {
        const balance = parseFloat(balanceInput.value);
        
        // 验证余额
        if (isNaN(balance) || balance < 0) {
            if (window.showNotification) {
                window.showNotification('请输入有效的余额金额', 'error');
            }
            balanceInput.focus();
            return false;
        }
        
        // 验证状态
        if (!statusSelect.value) {
            if (window.showNotification) {
                window.showNotification('请选择钱包状态', 'error');
            }
            statusSelect.focus();
            return false;
        }
        
        return true;
    }

    // 显示加载状态
    function showLoading() {
        submitBtn.classList.add('loading');
        submitBtn.disabled = true;
    }

    // 隐藏加载状态
    function hideLoading() {
        submitBtn.classList.remove('loading');
        submitBtn.disabled = false;
    }

    // 返回上一页
    window.goBack = function() {
        if (window.history.length > 1) {
            window.history.back();
        } else {
            window.location.href = '/admin/wallet';
        }
    };

    // 余额输入格式化
    if (balanceInput) {
        balanceInput.addEventListener('input', function() {
            // 限制小数点后最多2位
            let value = this.value;
            if (value.includes('.')) {
                const parts = value.split('.');
                if (parts[1] && parts[1].length > 2) {
                    this.value = parts[0] + '.' + parts[1].substring(0, 2);
                }
            }
        });

        // 只允许数字和小数点
        balanceInput.addEventListener('keypress', function(e) {
            const char = String.fromCharCode(e.which);
            if (!/[0-9.]/.test(char) && e.which !== 8 && e.which !== 0) {
                e.preventDefault();
            }
        });

        // 粘贴时验证
        balanceInput.addEventListener('paste', function(e) {
            setTimeout(() => {
                const value = this.value;
                if (!/^\d*\.?\d{0,2}$/.test(value)) {
                    this.value = value.replace(/[^\d.]/g, '');
                    const parts = this.value.split('.');
                    if (parts[1] && parts[1].length > 2) {
                        this.value = parts[0] + '.' + parts[1].substring(0, 2);
                    }
                }
            }, 0);
        });
    }

    // 状态变化时的提示
    if (statusSelect) {
        statusSelect.addEventListener('change', function() {
            const status = this.value;
            const balance = parseFloat(balanceInput.value);
            
            // 如果状态是冻结且余额大于0，给出提示
            if (status === 'FROZEN' && balance > 0) {
                if (window.showNotification) {
                    window.showNotification('注意：冻结钱包后用户将无法使用余额', 'warning');
                }
            }
            
            // 如果状态是禁用，给出提示
            if (status === 'DISABLED') {
                if (window.showNotification) {
                    window.showNotification('注意：禁用钱包后用户将无法进行任何操作', 'warning');
                }
            }
        });
    }

    // 键盘快捷键
    document.addEventListener('keydown', function(e) {
        // Ctrl+S 保存
        if (e.ctrlKey && e.key === 's') {
            e.preventDefault();
            if (form.checkValidity()) {
                submitForm(e);
            }
        }
        
        // ESC 返回
        if (e.key === 'Escape') {
            goBack();
        }
    });

    // 表单变化检测
    let originalData = {
        balance: balanceInput.value,
        status: statusSelect.value
    };

    function checkFormChanges() {
        const currentData = {
            balance: balanceInput.value,
            status: statusSelect.value
        };
        
        const hasChanges = JSON.stringify(originalData) !== JSON.stringify(currentData);
        
        // 可以在这里添加页面离开时的确认逻辑
        if (hasChanges) {
            // 页面有未保存的更改
            console.log('表单有未保存的更改');
        }
    }

    // 监听表单变化
    if (form) {
        form.addEventListener('input', checkFormChanges);
        form.addEventListener('change', checkFormChanges);
    }

    // 页面加载完成后的初始化
    console.log('钱包编辑页面加载完成');
});
