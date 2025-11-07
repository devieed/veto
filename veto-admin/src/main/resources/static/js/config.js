// 系统配置页面交互逻辑
document.addEventListener('DOMContentLoaded', function () {
    const navTabs = document.querySelectorAll('.nav-tab');
    const configSections = document.querySelectorAll('.config-section');
    
    let originalConfigs = {};
    let currentCategory = 'system';
    
    // 延迟初始化，确保Thymeleaf渲染完成
    setTimeout(function() {
        initializeConfigs();
    }, 100);

    // 初始化配置数据
    function initializeConfigs() {
        // 收集所有配置项的原始值
        const configItems = document.querySelectorAll('.config-item');
        
        configItems.forEach((item, index) => {
            // 先检查是否有复选框组
            const checkboxGroup = item.querySelector('.checkbox-group');
            if (checkboxGroup) {
                const key = 'veto_config_transaction_records_dimensions'; // 固定key
                const type = 'multi_checkbox';
                const value = getControlValue(checkboxGroup, type);
                
                originalConfigs[key] = {
                    value: value,
                    type: type,
                    element: checkboxGroup
                };
                
                // 初始化复选框组
                initializeCheckboxGroup(checkboxGroup, key);
                return;
            }
            
            // 处理其他类型的控件
            const controlElement = item.querySelector('.form-control, .switch input');
            
            if (controlElement) {
                const key = controlElement.getAttribute('name');
                
                if (key) {
                    const type = controlElement.getAttribute('data-type') || getControlType(controlElement);
                    const value = getControlValue(controlElement, type);
                    
                    originalConfigs[key] = {
                        value: value,
                        type: type,
                        element: controlElement
                    };
                    
                    // 添加变化监听
                    addChangeListener(controlElement, key);
                    
                    // 特殊处理枚举类型
                    if (key === 'veto_config_system_coin_type' && type === 'enum_select') {
                        initializeEnumSelect(controlElement);
                    }
                }
            }
        });
    }

    // 初始化枚举选择器
    function initializeEnumSelect(selectElement) {
        const currentValue = selectElement.value;
        const options = selectElement.querySelectorAll('option');
        
        // 如果当前有值，禁用其他选项
        if (currentValue) {
            options.forEach(option => {
                if (option.value && option.value !== currentValue) {
                    option.disabled = true;
                }
            });
        }
    }

    // 初始化复选框组
    function initializeCheckboxGroup(element, key) {
        const checkboxGroup = element.closest ? element.closest('.checkbox-group') : element;
        const checkboxes = checkboxGroup.querySelectorAll('input[type="checkbox"]');
        
        // 为每个复选框添加变化监听
        checkboxes.forEach(checkbox => {
            checkbox.addEventListener('change', function() {
                handleConfigChange(key, checkboxGroup);
            });
        });
    }

    // 获取控件类型
    function getControlType(element) {
        if (element.type === 'checkbox') return 'boolean';
        if (element.type === 'number') return 'number';
        if (element.tagName === 'SELECT') {
            const dataType = element.getAttribute('data-type');
            if (dataType === 'enum_select') return 'enum_select';
            if (dataType === 'multi_select') return 'multi_select';
            return 'select';
        }
        // 检查是否是复选框组
        if (element.closest('.checkbox-group')) return 'multi_checkbox';
        return 'string';
    }

    // 获取控件值
    function getControlValue(element, type) {
        switch (type) {
            case 'boolean':
                return element.checked;
            case 'number':
                return parseFloat(element.value) || 0;
            case 'select':
            case 'enum_select':
                return element.value;
            case 'multi_select':
                return Array.from(element.selectedOptions).map(option => option.value);
            case 'multi_checkbox':
                const checkboxGroup = element.closest ? element.closest('.checkbox-group') : element;
                const checkedBoxes = checkboxGroup.querySelectorAll('input[type="checkbox"]:checked');
                return Array.from(checkedBoxes).map(checkbox => checkbox.value);
            default:
                return element.value;
        }
    }

    // 设置控件值
    function setControlValue(element, value, type) {
        switch (type) {
            case 'boolean':
                element.checked = Boolean(value);
                break;
            case 'number':
                element.value = Number(value);
                break;
            case 'select':
            case 'enum_select':
                element.value = String(value);
                break;
            case 'multi_select':
                // 清除所有选择
                Array.from(element.options).forEach(option => option.selected = false);
                // 设置选中的选项
                if (Array.isArray(value)) {
                    value.forEach(val => {
                        const option = element.querySelector(`option[value="${val}"]`);
                        if (option) option.selected = true;
                    });
                }
                break;
            case 'multi_checkbox':
                const checkboxGroup = element.closest ? element.closest('.checkbox-group') : element;
                const checkboxes = checkboxGroup.querySelectorAll('input[type="checkbox"]');
                // 清除所有选择
                checkboxes.forEach(checkbox => checkbox.checked = false);
                // 设置选中的复选框
                if (Array.isArray(value)) {
                    value.forEach(val => {
                        const checkbox = checkboxGroup.querySelector(`input[value="${val}"]`);
                        if (checkbox) checkbox.checked = true;
                    });
                }
                break;
            default:
                element.value = String(value);
        }
    }

    // 添加变化监听
    function addChangeListener(element, key) {
        element.addEventListener('input', function() {
            handleConfigChange(key, element);
        });
        
        element.addEventListener('change', function() {
            handleConfigChange(key, element);
        });
        
        // 为checkbox添加click事件监听
        if (element.type === 'checkbox') {
            element.addEventListener('click', function() {
                handleConfigChange(key, element);
            });
        }
    }

    // 处理配置变化
    function handleConfigChange(key, element) {
        const type = element.getAttribute('data-type') || getControlType(element);
        const newValue = getControlValue(element, type);
        const originalValue = originalConfigs[key] ? originalConfigs[key].value : null;
        
        console.log('配置变化检测:', {
            key: key,
            type: type,
            newValue: newValue,
            originalValue: originalValue,
            hasOriginal: !!originalConfigs[key]
        });
        
        // 验证值
        if (!validateConfigValue(key, newValue, type)) {
            markConfigAsInvalid(key);
            return;
        } else {
            markConfigAsValid(key);
        }
        
        // 检查是否有变化
        const hasChanged = originalValue !== null && JSON.stringify(newValue) !== JSON.stringify(originalValue);
        console.log('是否有变化:', hasChanged);
        
        toggleConfigButtons(key, hasChanged);
        
        if (hasChanged) {
            markConfigAsModified(key);
        } else {
            markConfigAsUnmodified(key);
        }
    }

    // 验证配置值
    function validateConfigValue(key, value, type) {
        switch (type) {
            case 'number':
                return !isNaN(value) && isFinite(value);
            case 'string':
                return typeof value === 'string' && value.trim().length > 0;
            case 'boolean':
                return typeof value === 'boolean';
            case 'select':
            case 'enum_select':
                return value !== '';
            case 'multi_select':
            case 'multi_checkbox':
                return Array.isArray(value) && value.length > 0;
            default:
                return true;
        }
    }

    // 标记配置为已修改
    function markConfigAsModified(key) {
        const item = findConfigItemByKey(key);
        if (item) {
            item.classList.add('modified');
        }
    }

    // 标记配置为未修改
    function markConfigAsUnmodified(key) {
        const item = findConfigItemByKey(key);
        if (item) {
            item.classList.remove('modified');
        }
    }

    // 标记配置为有效
    function markConfigAsValid(key) {
        const item = findConfigItemByKey(key);
        if (item) {
            item.classList.remove('error');
            item.classList.add('valid');
        }
    }

    // 标记配置为无效
    function markConfigAsInvalid(key) {
        const item = findConfigItemByKey(key);
        if (item) {
            item.classList.remove('valid');
            item.classList.add('error');
        }
    }

    // 查找配置项
    function findConfigItemByKey(key) {
        // 特殊处理复选框组
        if (key === 'veto_config_transaction_records_dimensions') {
            const checkboxGroup = document.querySelector('.checkbox-group');
            if (checkboxGroup) {
                return checkboxGroup.closest('.config-item');
            }
        }
        
        const controlElement = document.querySelector(`[name="${key}"]`);
        if (controlElement) {
            return controlElement.closest('.config-item');
        }
        return null;
    }

    // 切换配置项按钮显示
    function toggleConfigButtons(key, show) {
        console.log('切换按钮显示:', key, show);
        const item = findConfigItemByKey(key);
        console.log('找到的配置项:', item);
        
        if (item) {
            const saveBtn = item.querySelector('.save-btn');
            console.log('找到的保存按钮:', saveBtn);
            
            if (saveBtn) {
                if (show) {
                    saveBtn.style.display = 'inline-flex';
                    console.log('显示保存按钮');
                } else {
                    saveBtn.style.display = 'none';
                    console.log('隐藏保存按钮');
                }
            } else {
                console.log('未找到保存按钮');
            }
        } else {
            console.log('未找到配置项');
        }
    }

    // 切换配置分类
    window.switchCategory = function(category) {
        // 更新导航标签
        navTabs.forEach(tab => {
            if (tab.getAttribute('data-category') === category) {
                tab.classList.add('active');
            } else {
                tab.classList.remove('active');
            }
        });
        
        // 更新配置内容
        configSections.forEach(section => {
            if (section.id === category + '-section') {
                section.classList.add('active');
            } else {
                section.classList.remove('active');
            }
        });
        
        currentCategory = category;
    };

    // 处理枚举类型变化
    window.handleEnumChange = function(selectElement) {
        const key = selectElement.getAttribute('name');
        if (key === 'veto_config_system_coin_type') {
            // 特殊处理系统币种类型
            const currentValue = selectElement.value;
            const options = selectElement.querySelectorAll('option');
            
            // 如果选择了值，禁用其他选项
            if (currentValue) {
                options.forEach(option => {
                    if (option.value && option.value !== currentValue) {
                        option.disabled = true;
                    }
                });
            } else {
                // 如果没有选择，启用所有选项
                options.forEach(option => {
                    option.disabled = false;
                });
            }
        }
        
        // 触发配置变化检测
        const configItem = selectElement.closest('.config-item');
        if (configItem) {
            const keyElement = configItem.querySelector('.config-key');
            if (keyElement) {
                const key = keyElement.textContent;
                handleConfigChange(key, selectElement);
            }
        }
    };

    // 处理多选类型变化
    window.handleMultiSelectChange = function(selectElement) {
        // 触发配置变化检测
        const configItem = selectElement.closest('.config-item');
        if (configItem) {
            const keyElement = configItem.querySelector('.config-key');
            if (keyElement) {
                const key = keyElement.textContent;
                handleConfigChange(key, selectElement);
            }
        }
    };

    // 保存配置
    window.saveConfig = function(buttonElement) {
        const configItem = buttonElement.closest('.config-item');
        
        // 检查是否是复选框组
        const checkboxGroup = configItem.querySelector('.checkbox-group');
        let controlElement, key, type, value;
        
        if (checkboxGroup) {
            controlElement = checkboxGroup;
            key = 'veto_config_transaction_records_dimensions';
            type = 'multi_checkbox';
            value = getControlValue(controlElement, type);
        } else {
            controlElement = configItem.querySelector('.form-control, .switch input');
            key = controlElement.getAttribute('name');
            type = controlElement.getAttribute('data-type') || getControlType(controlElement);
            value = getControlValue(controlElement, type);
        }
        
        // 显示加载状态
        const originalText = buttonElement.innerHTML;
        buttonElement.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 保存中...';
        buttonElement.disabled = true;

        let formData = new FormData();
        formData.append('key', key);
        
        // 处理多选值
        if ((type === 'multi_select' || type === 'multi_checkbox') && Array.isArray(value)) {
            // 将数组转换为逗号分隔的字符串
            formData.append('val', value.join(','));
        } else {
            formData.append('val', value);
        }

        // 发送保存请求
        fetch('/update_settings', {
            method: 'POST',
            body:formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.code === "1") {
                // 更新原始值
                originalConfigs[key].value = value;
                
                // 隐藏保存按钮
                buttonElement.style.display = 'none';
                
                // 移除修改状态
                markConfigAsUnmodified(key);
                
                // 显示成功提示
                showSaveSuccess(key);
            } else {
                showError('保存失败: ' + (data.message || '未知错误'));
            }
        })
        .catch(error => {
            console.error('保存配置失败:', error);
            showError('保存失败: ' + error.message);
        })
        .finally(() => {
            // 恢复按钮状态
            buttonElement.innerHTML = originalText;
            buttonElement.disabled = false;
        });
    };

    // 显示保存成功提示
    function showSaveSuccess(key) {
        const item = findConfigItemByKey(key);
        if (item) {
            const successDiv = document.createElement('div');
            successDiv.className = 'save-success';
            successDiv.innerHTML = '<i class="fas fa-check"></i> 配置保存成功';
            document.body.appendChild(successDiv);
            
            setTimeout(() => {
                successDiv.remove();
            }, 3000);
        }
    }

    // 显示错误信息
    function showError(message) {
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.innerHTML = '<i class="fas fa-exclamation-triangle"></i> ' + message;
        document.body.appendChild(errorDiv);
        
        setTimeout(() => {
            errorDiv.remove();
        }, 5000);
    }

    // 导出配置
    window.exportConfigs = function() {
        const configs = {};
        Object.keys(originalConfigs).forEach(key => {
            const element = originalConfigs[key].element;
            const type = originalConfigs[key].type;
            configs[key] = getControlValue(element, type);
        });
        
        const dataStr = JSON.stringify(configs, null, 2);
        const dataBlob = new Blob([dataStr], {type: 'application/json'});
        const url = URL.createObjectURL(dataBlob);
        
        const link = document.createElement('a');
        link.href = url;
        link.download = 'veto-configs.json';
        link.click();
        
        URL.revokeObjectURL(url);
    };

    // 导入配置
    window.importConfigs = function() {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = '.json';
        input.onchange = function(e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    try {
                        const configs = JSON.parse(e.target.result);
                        
                        // 更新配置值
                        Object.keys(configs).forEach(key => {
                            if (originalConfigs[key]) {
                                const element = originalConfigs[key].element;
                                const type = originalConfigs[key].type;
                                setControlValue(element, configs[key], type);
                                
                                // 更新原始值
                                originalConfigs[key].value = configs[key];
                                
                                // 触发变化检测
                                handleConfigChange(key, element);
                            }
                        });
                        
                        showSaveSuccess('配置导入成功');
                    } catch (error) {
                        showError('配置文件格式错误');
                    }
                };
                reader.readAsText(file);
            }
        };
        input.click();
    };

    // 键盘快捷键
    document.addEventListener('keydown', function(e) {
        // Ctrl+S 保存当前聚焦的配置项
        if (e.ctrlKey && e.key === 's') {
            e.preventDefault();
            const focusedElement = document.activeElement;
            if (focusedElement && focusedElement.closest('.config-item')) {
                const configItem = focusedElement.closest('.config-item');
                const saveBtn = configItem.querySelector('.save-btn');
                if (saveBtn && saveBtn.style.display !== 'none') {
                    saveBtn.click();
                }
            }
        }
    });
});
