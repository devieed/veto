// 球队编辑页面交互逻辑
document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('teamEditForm');
    const submitBtn = document.getElementById('submitBtn');
    const nameCnInput = document.getElementById('nameCn');
    const nameEnInput = document.getElementById('nameEn');
    const statusSelect = document.getElementById('status');
    const logoInput = document.getElementById('logo');
    const uploadPlaceholder = document.getElementById('uploadPlaceholder');
    const imagePreview = document.getElementById('imagePreview');
    const previewImage = document.getElementById('previewImage');

    // 表单提交处理
    window.submitForm = function(event) {
        event.preventDefault();
        
        // 验证表单
        if (!validateForm()) {
            return;
        }
        
        // 显示加载状态
        showLoading();
        
        // 创建FormData对象
        const formData = new FormData();
        formData.append('nameCn', nameCnInput.value.trim());
        formData.append('nameEn', nameEnInput.value.trim());
        formData.append('status', statusSelect.value === 'true');
        
        // 如果有新上传的图片，添加到FormData
        if (logoInput.files && logoInput.files[0]) {
            formData.append('logo', logoInput.files[0]);
        }
        
        // 发送更新请求
        fetch('/admin/teams/save', {
            method: 'POST',
            body: formData
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
                    window.showNotification('球队保存成功', 'success');
                }
                // 延迟跳转，让用户看到成功消息
                setTimeout(() => {
                    goBack();
                }, 1500);
            } else {
                throw new Error(data.message || '保存失败');
            }
        })
        .catch(error => {
            hideLoading();
            console.error('保存球队失败:', error);
            if (window.showNotification) {
                window.showNotification(error.message || '保存失败，请重试', 'error');
            }
        });
    };

    // 表单验证
    function validateForm() {
        const nameCn = nameCnInput.value.trim();
        const nameEn = nameEnInput.value.trim();
        
        // 验证中文名称
        if (!nameCn) {
            if (window.showNotification) {
                window.showNotification('请输入球队中文名称', 'error');
            }
            nameCnInput.focus();
            return false;
        }
        
        if (nameCn.length > 20) {
            if (window.showNotification) {
                window.showNotification('中文名称不能超过20个字符', 'error');
            }
            nameCnInput.focus();
            return false;
        }
        
        // 验证英文名称
        if (!nameEn) {
            if (window.showNotification) {
                window.showNotification('请输入球队英文名称', 'error');
            }
            nameEnInput.focus();
            return false;
        }
        
        if (nameEn.length > 50) {
            if (window.showNotification) {
                window.showNotification('英文名称不能超过50个字符', 'error');
            }
            nameEnInput.focus();
            return false;
        }
        
        // 验证状态
        if (!statusSelect.value) {
            if (window.showNotification) {
                window.showNotification('请选择球队状态', 'error');
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
            window.location.href = '/admin/teams';
        }
    };

    // 文件选择处理
    window.handleFileSelect = function(event) {
        const file = event.target.files[0];
        if (file) {
            // 验证文件类型
            if (!file.type.startsWith('image/')) {
                if (window.showNotification) {
                    window.showNotification('请选择图片文件', 'error');
                }
                return;
            }
            
            // 验证文件大小（5MB限制）
            if (file.size > 5 * 1024 * 1024) {
                if (window.showNotification) {
                    window.showNotification('图片文件大小不能超过5MB', 'error');
                }
                return;
            }
            
            // 显示预览
            const reader = new FileReader();
            reader.onload = function(e) {
                previewImage.src = e.target.result;
                uploadPlaceholder.style.display = 'none';
                imagePreview.style.display = 'block';
            };
            reader.readAsDataURL(file);
        }
    };

    // 移除图片
    window.removeImage = function() {
        logoInput.value = '';
        uploadPlaceholder.style.display = 'flex';
        imagePreview.style.display = 'none';
    };

    // 移除当前图片
    window.removeCurrentImage = function() {
        if (confirm('确定要删除当前图标吗？')) {
            // 这里可以发送请求删除服务器上的图片
            console.log('删除当前图标');
            if (window.showNotification) {
                window.showNotification('当前图标已删除', 'success');
            }
        }
    };

    // 拖拽上传
    if (uploadPlaceholder) {
        uploadPlaceholder.addEventListener('dragover', function(e) {
            e.preventDefault();
            e.stopPropagation();
            this.closest('.file-upload-area').classList.add('dragover');
        });

        uploadPlaceholder.addEventListener('dragleave', function(e) {
            e.preventDefault();
            e.stopPropagation();
            this.closest('.file-upload-area').classList.remove('dragover');
        });

        uploadPlaceholder.addEventListener('drop', function(e) {
            e.preventDefault();
            e.stopPropagation();
            this.closest('.file-upload-area').classList.remove('dragover');
            
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                logoInput.files = files;
                handleFileSelect({ target: { files: files } });
            }
        });
    }

    // 输入字符限制
    if (nameCnInput) {
        nameCnInput.addEventListener('input', function() {
            if (this.value.length > 20) {
                this.value = this.value.substring(0, 20);
                if (window.showNotification) {
                    window.showNotification('中文名称不能超过20个字符', 'warning');
                }
            }
        });
    }

    if (nameEnInput) {
        nameEnInput.addEventListener('input', function() {
            if (this.value.length > 50) {
                this.value = this.value.substring(0, 50);
                if (window.showNotification) {
                    window.showNotification('英文名称不能超过50个字符', 'warning');
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
        nameCn: nameCnInput.value,
        nameEn: nameEnInput.value,
        status: statusSelect.value
    };

    function checkFormChanges() {
        const currentData = {
            nameCn: nameCnInput.value,
            nameEn: nameEnInput.value,
            status: statusSelect.value
        };
        
        const hasChanges = JSON.stringify(originalData) !== JSON.stringify(currentData) || 
                          (logoInput.files && logoInput.files.length > 0);
        
        if (hasChanges) {
            console.log('表单有未保存的更改');
        }
    }

    // 监听表单变化
    if (form) {
        form.addEventListener('input', checkFormChanges);
        form.addEventListener('change', checkFormChanges);
    }

    // 页面离开确认
    window.addEventListener('beforeunload', function(e) {
        const currentData = {
            nameCn: nameCnInput.value,
            nameEn: nameEnInput.value,
            status: statusSelect.value
        };
        
        const hasChanges = JSON.stringify(originalData) !== JSON.stringify(currentData) || 
                          (logoInput.files && logoInput.files.length > 0);
        
        if (hasChanges) {
            e.preventDefault();
            e.returnValue = '您有未保存的更改，确定要离开吗？';
            return e.returnValue;
        }
    });

    // 页面加载完成后的初始化
    console.log('球队编辑页面加载完成');
});
