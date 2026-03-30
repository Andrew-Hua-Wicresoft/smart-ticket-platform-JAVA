-- Seed data for 智能工单系统
-- Password for all users: demo123
-- BCrypt hash of 'demo123' (verified): $2a$10$9UjGFrBHBtSHveVw7sTkuueG/uMz1Ed18fMOLtGnCXOdhclsA3552

-- Only insert if users table is empty (idempotent)
INSERT INTO users (username, password, name, role, department)
SELECT 'customer1', '$2a$10$9UjGFrBHBtSHveVw7sTkuueG/uMz1Ed18fMOLtGnCXOdhclsA3552', '李明', 'CUSTOMER', '市场部'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'customer1');

INSERT INTO users (username, password, name, role, department)
SELECT 'customer2', '$2a$10$9UjGFrBHBtSHveVw7sTkuueG/uMz1Ed18fMOLtGnCXOdhclsA3552', '赵丽', 'CUSTOMER', '销售部'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'customer2');

INSERT INTO users (username, password, name, role, department)
SELECT 'customer3', '$2a$10$9UjGFrBHBtSHveVw7sTkuueG/uMz1Ed18fMOLtGnCXOdhclsA3552', '张华', 'CUSTOMER', '财务部'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'customer3');

INSERT INTO users (username, password, name, role, department)
SELECT 'engineer1', '$2a$10$9UjGFrBHBtSHveVw7sTkuueG/uMz1Ed18fMOLtGnCXOdhclsA3552', '王工程师', 'ENGINEER', 'IT部'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'engineer1');

INSERT INTO users (username, password, name, role, department)
SELECT 'engineer2', '$2a$10$9UjGFrBHBtSHveVw7sTkuueG/uMz1Ed18fMOLtGnCXOdhclsA3552', '陈工程师', 'ENGINEER', 'IT部'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'engineer2');

INSERT INTO users (username, password, name, role, department)
SELECT 'admin1', '$2a$10$9UjGFrBHBtSHveVw7sTkuueG/uMz1Ed18fMOLtGnCXOdhclsA3552', '管理员', 'ADMIN', 'IT部'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin1');

-- ============================================================
-- Seed Knowledge Base Articles (PUBLISHED, no embedding yet)
-- These provide demo content for AI search/deflection
-- ============================================================

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '如何重置公司邮箱密码',
       '## 问题描述
员工忘记公司邮箱密码，无法登录邮箱。

## 解决步骤
1. 打开公司邮箱登录页面 (mail.company.com)
2. 点击「忘记密码」链接
3. 输入您的工号或注册手机号
4. 系统会发送验证码到您绑定的手机
5. 输入验证码后设置新密码
6. 新密码要求：8位以上，包含大小写字母和数字

## 注意事项
- 密码重置后，所有已登录的设备会自动退出
- 如果手机号已更换，请联系IT部门人工处理
- 密码有效期为90天，过期需重新设置',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '如何重置公司邮箱密码');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT 'VPN连接失败的排查方法',
       '## 问题描述
员工在远程办公时无法连接公司VPN。

## 常见原因及解决方法

### 1. 用户名或密码错误
- 确认使用的是域账号（工号@company.com）
- 检查大小写是否正确
- 尝试重置密码后重新连接

### 2. VPN客户端版本过旧
- 卸载当前VPN客户端
- 从内网下载页 (vpn.company.com/download) 下载最新版
- 安装后重新配置连接

### 3. 网络环境问题
- 检查本地网络是否正常（能否打开百度）
- 尝试更换网络（如从WiFi切换到手机热点）
- 部分酒店/咖啡店WiFi会屏蔽VPN端口

### 4. 防火墙/杀毒软件拦截
- 临时关闭本地防火墙测试
- 将VPN客户端加入杀毒软件白名单

## 如果以上方法都无效
请提交工单，附上VPN客户端的错误截图和日志文件。',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = 'VPN连接失败的排查方法');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '打印机无法打印的解决方案',
       '## 问题描述
办公室打印机无法正常打印文档。

## 排查步骤

### 第一步：检查物理连接
- 确认打印机电源已开启（指示灯亮起）
- 检查USB线或网线是否插好
- 网络打印机：确认打印机和电脑在同一网段

### 第二步：检查打印队列
1. 打开「控制面板」→「设备和打印机」
2. 右键点击打印机 →「查看打印队列」
3. 如有堵塞的任务，选择「取消所有文档」
4. 重新尝试打印

### 第三步：重启打印服务
1. 按 Win+R，输入 services.msc
2. 找到「Print Spooler」服务
3. 右键 →「重启」

### 第四步：重新安装驱动
1. 从公司内网下载页获取对应型号驱动
2. 卸载旧驱动，安装新驱动
3. 重新添加打印机

## 各楼层打印机型号
- 1F大厅：HP LaserJet M609（IP: 192.168.1.101）
- 2F办公区：Canon iR-ADV C5535（IP: 192.168.1.102）
- 3F办公区：HP LaserJet M609（IP: 192.168.1.103）',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '打印机无法打印的解决方案');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '新员工IT设备申请流程',
       '## 适用范围
新入职员工申请电脑、显示器、键鼠等IT设备。

## 申请流程
1. **入职当天**：HR会发送IT设备申请链接到新员工邮箱
2. **填写申请表**：选择所需设备配置（标准配置/开发配置/设计配置）
3. **部门主管审批**：主管在OA系统中审批
4. **IT部门配置**：审批通过后，IT部门在1-2个工作日内完成配置
5. **设备领取**：到IT部门（3楼301室）领取设备并签字确认

## 标准配置方案
| 岗位类型 | 电脑 | 显示器 | 其他 |
|---------|------|--------|------|
| 行政/销售 | ThinkPad T14s | 24寸 x1 | 键鼠套装 |
| 开发工程师 | MacBook Pro 14 | 27寸 x2 | 机械键盘 |
| 设计师 | MacBook Pro 16 | 27寸 4K x2 | 手绘板 |

## 注意事项
- 设备归公司所有，离职时需归还
- 人为损坏需按折旧价赔偿
- 设备故障请提交维修工单',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '新员工IT设备申请流程');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT 'Windows系统蓝屏的处理方法',
       '## 问题描述
电脑出现蓝屏（BSOD），显示错误代码后自动重启。

## 常见蓝屏错误代码

### KERNEL_DATA_INPAGE_ERROR
- 原因：硬盘坏道或内存故障
- 处理：运行 chkdsk /f 检查磁盘，联系IT检测硬件

### DRIVER_IRQL_NOT_LESS_OR_EQUAL
- 原因：驱动程序冲突
- 处理：进入安全模式，卸载最近安装的驱动

### CRITICAL_PROCESS_DIED
- 原因：系统关键进程异常
- 处理：尝试系统还原到最近的还原点

## 通用处理步骤
1. 记录蓝屏上的错误代码（拍照）
2. 重启电脑，看是否能正常进入系统
3. 如能进入系统：
   - 检查最近是否安装了新软件/驱动
   - 运行 Windows Update 更新系统
   - 使用「事件查看器」查看错误日志
4. 如无法进入系统：
   - 尝试进入安全模式（开机时按F8）
   - 如安全模式也进不去，请联系IT部门

## 预防措施
- 定期保存重要文件到公司网盘
- 不要随意安装非公司授权软件
- 保持系统和驱动更新',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = 'Windows系统蓝屏的处理方法');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '公司WiFi连接指南',
       '## 公司无线网络信息

### 办公网络
- SSID: Company-Office
- 认证方式: 802.1x（使用域账号登录）
- 用户名: 您的工号
- 密码: 域账号密码

### 访客网络
- SSID: Company-Guest
- 密码: 每周更新，请咨询前台
- 限制: 仅可访问互联网，无法访问内网资源

## 连接步骤（Windows）
1. 点击任务栏WiFi图标
2. 选择「Company-Office」
3. 输入工号和域密码
4. 勾选「自动连接」

## 连接步骤（Mac）
1. 点击菜单栏WiFi图标
2. 选择「Company-Office」
3. 用户名填工号，密码填域密码
4. 信任证书弹窗点击「继续」

## 常见问题
- **连不上**: 确认密码正确，尝试「忘记网络」后重新连接
- **网速慢**: 高峰期可能拥堵，建议使用有线网络
- **频繁断开**: 检查电脑的WiFi驱动是否最新',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '公司WiFi连接指南');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT 'OA系统请假审批流程',
       '## 请假类型
- 年假、事假、病假、婚假、产假、丧假

## 操作步骤
1. 登录OA系统 (oa.company.com)
2. 进入「人事」→「考勤管理」→「请假申请」
3. 选择请假类型和日期
4. 填写请假事由
5. 上传相关证明（病假需上传医院证明）
6. 提交审批

## 审批流程
- 1-3天: 直属主管审批
- 4-7天: 部门总监审批
- 7天以上: 人事总监审批

## 注意事项
- 请提前至少1天提交申请（紧急情况除外）
- 病假超过3天需提供三甲医院证明
- 年假需提前1周申请
- 请假期间保持手机畅通',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'admin1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = 'OA系统请假审批流程');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '企业微信安装与配置',
       '## 下载安装
- Windows/Mac: 访问 work.weixin.qq.com 下载桌面版
- 手机: 应用商店搜索「企业微信」

## 首次登录
1. 打开企业微信
2. 选择「企业登录」
3. 输入手机号获取验证码
4. 加入公司组织（公司名称：XX科技有限公司）
5. 等待管理员审批加入

## 常见功能
- **即时通讯**: 单聊、群聊、语音/视频会议
- **审批流程**: 请假、报销、用章等审批
- **打卡考勤**: 每日上下班打卡
- **文档协作**: 在线编辑和共享文档

## 常见问题
- **收不到消息**: 检查通知权限是否开启
- **无法打卡**: 确认已开启定位权限，在公司WiFi范围内
- **加入不了群**: 联系群主邀请或扫描群二维码',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '企业微信安装与配置');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '电脑运行速度慢的优化方法',
       '## 问题描述
电脑使用一段时间后变得卡顿，程序响应慢。

## 快速优化步骤

### 1. 关闭不必要的程序
- 按 Ctrl+Shift+Esc 打开任务管理器
- 查看CPU和内存占用高的程序
- 右键结束不需要的进程

### 2. 清理磁盘空间
- 确保C盘剩余空间 > 20GB
- 运行「磁盘清理」工具
- 清理浏览器缓存
- 将大文件移到公司网盘

### 3. 禁用不必要的开机启动项
1. 打开任务管理器 →「启动」选项卡
2. 禁用非必要的启动项
3. 保留：杀毒软件、企业微信、VPN客户端

### 4. 检查是否有病毒/恶意软件
- 运行公司统一部署的杀毒软件全盘扫描
- 不要关闭杀毒软件的实时防护

### 5. 硬件升级建议
如以上方法无效，可能需要硬件升级：
- 内存不足8GB：申请加装内存
- 机械硬盘：申请更换SSD
- 提交设备升级工单，IT部门评估后处理',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '电脑运行速度慢的优化方法');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '公司网盘使用说明',
       '## 访问方式
- 网页版: drive.company.com
- 客户端: 从内网下载页安装同步客户端
- 移动端: 企业微信内置「微盘」

## 存储配额
| 角色 | 个人空间 | 部门共享空间 |
|------|---------|------------|
| 普通员工 | 50GB | 共享 |
| 部门主管 | 100GB | 可管理 |
| 总监及以上 | 200GB | 可管理 |

## 文件分享规则
- 内部分享: 可设置查看/编辑权限
- 外部分享: 需部门主管审批，链接有效期最长7天
- 机密文件: 禁止外部分享

## 同步客户端设置
1. 安装客户端后登录域账号
2. 选择需要同步的文件夹
3. 建议只同步当前项目文件夹，避免占用过多本地空间

## 注意事项
- 重要文件请务必备份到网盘
- 不要在网盘存放个人非工作文件
- 离职时个人空间文件将转交部门主管',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '公司网盘使用说明');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '显示器不亮或无信号的排查',
       '## 问题描述
显示器黑屏、无信号或显示「No Signal」。

## 排查步骤

### 1. 检查基本连接
- 确认显示器电源线已插好
- 确认视频线（HDMI/DP/VGA）两端都插紧
- 尝试重新拔插视频线

### 2. 检查输入源
- 按显示器上的「Source/Input」按钮
- 切换到正确的输入源（HDMI1/HDMI2/DP）

### 3. 测试交叉排除
- 尝试换一根视频线
- 将显示器接到其他电脑测试
- 将其他显示器接到你的电脑测试

### 4. 笔记本外接显示器
- Windows: Win+P 选择「扩展」或「复制」
- Mac: 系统设置 → 显示器 → 排列

### 5. 检查显卡
- 如果是台式机，检查显卡是否插紧
- 尝试使用主板集成显卡输出
- 更新显卡驱动

## 如果以上都不行
可能是硬件故障，请提交工单并注明已尝试的排查步骤。',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '显示器不亮或无信号的排查');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '如何申请软件安装权限',
       '## 背景
公司电脑默认没有管理员权限，安装软件需要申请。

## 申请流程
1. 登录OA系统
2. 进入「IT服务」→「软件安装申请」
3. 填写需要安装的软件名称、版本、用途
4. 提交后由部门主管和IT部门双重审批

## 已授权软件列表（无需申请）
- Office 365 套件
- 企业微信
- 公司VPN客户端
- Chrome/Firefox 浏览器
- WinRAR/7-Zip
- 公司统一杀毒软件

## 常见被拒绝的软件
- 个人娱乐类软件（游戏等）
- 来源不明的破解软件
- 与工作无关的工具

## 紧急安装
如因工作紧急需要安装，可直接联系IT值班（内线8888），IT工程师远程操作安装。',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'admin1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '如何申请软件安装权限');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '邮件客户端Outlook配置方法',
       '## 自动配置（推荐）
1. 打开Outlook
2. 输入公司邮箱地址（工号@company.com）
3. 点击「连接」
4. 输入域密码
5. Outlook会自动检测服务器配置

## 手动配置
如自动配置失败，使用以下参数：

### 接收服务器（IMAP）
- 服务器: imap.company.com
- 端口: 993
- 加密: SSL/TLS

### 发送服务器（SMTP）
- 服务器: smtp.company.com
- 端口: 587
- 加密: STARTTLS

### 认证信息
- 用户名: 工号@company.com
- 密码: 域账号密码

## 手机端配置
- iPhone: 设置 → 邮件 → 添加账户 → Exchange
- Android: 使用Outlook App或自带邮件App

## 常见问题
- **发送失败**: 检查附件大小不超过25MB
- **收不到邮件**: 检查垃圾邮件文件夹
- **证书警告**: 信任公司SSL证书即可',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '邮件客户端Outlook配置方法');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '文件误删恢复方法',
       '## 情况一：从回收站恢复
1. 双击桌面「回收站」图标
2. 找到误删的文件
3. 右键 →「还原」

## 情况二：从网盘恢复
1. 登录公司网盘 drive.company.com
2. 进入「回收站」（左侧菜单）
3. 找到文件 → 点击「恢复」
4. 网盘回收站保留30天

## 情况三：从备份恢复
如果文件在本地且已清空回收站：
1. 联系IT部门
2. 提供文件路径、大致删除时间
3. IT部门从每日备份中恢复（最多保留30天）

## 情况四：使用文件历史版本
- Windows: 右键文件夹 →「属性」→「以前的版本」
- Office文件: 打开文件 →「文件」→「信息」→「版本历史记录」

## 预防建议
- 重要文件实时同步到公司网盘
- 开启Office自动保存功能
- 删除前仔细确认文件',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '文件误删恢复方法');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '会议室预约系统使用说明',
       '## 预约方式
1. 登录OA系统 → 「行政」→「会议室预约」
2. 或通过企业微信「日程」功能直接预约

## 会议室列表
| 会议室 | 位置 | 容量 | 设备 |
|--------|------|------|------|
| 创新厅 | 1F | 30人 | 投影仪+视频会议 |
| A301 | 3F | 10人 | 电视屏+白板 |
| A302 | 3F | 6人 | 电视屏 |
| B201 | 2F | 8人 | 投影仪 |
| B202 | 2F | 4人 | 电视屏 |

## 预约规则
- 每次最长预约2小时
- 提前15分钟未签到自动释放
- 可提前7天预约
- 跨天会议需特殊审批

## 视频会议设备使用
1. 开启会议室电视/投影仪
2. 按遥控器「信号源」切换到HDMI
3. 用USB-C/HDMI线连接笔记本
4. 视频会议: 使用桌上的会议电话或摄像头

## 问题反馈
设备故障请拨打行政前台（内线8000）。',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'admin1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '会议室预约系统使用说明');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '域账号被锁定的解决方法',
       '## 原因
连续5次输入错误密码，域账号会被自动锁定30分钟。

## 自助解锁
1. 访问 unlock.company.com
2. 输入工号
3. 通过绑定手机接收验证码
4. 验证成功后自动解锁

## 无法自助解锁时
联系IT服务台：
- 内线: 8888
- 企业微信: IT服务群
- 服务时间: 工作日 8:30-18:00

## 密码策略
- 最小长度: 8位
- 必须包含: 大写字母、小写字母、数字
- 不能包含: 用户名、连续3个相同字符
- 有效期: 90天
- 历史密码: 不能与前5次相同

## 预防账号锁定
- 修改密码后及时更新所有设备上的保存密码
- 特别注意: 手机邮件App、VPN客户端等自动登录的应用',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '域账号被锁定的解决方法');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '报销系统操作指南',
       '## 报销流程
1. 登录OA系统 →「财务」→「费用报销」
2. 选择报销类型（差旅/办公用品/招待/培训等）
3. 填写费用明细
4. 上传发票照片（电子发票上传PDF）
5. 提交审批

## 审批链
员工 → 直属主管 → 财务审核 → 财务总监（>5000元）

## 发票要求
- 必须是正规增值税发票
- 抬头: XX科技有限公司
- 税号: 91110000XXXXXXXXXX
- 发票日期需在报销月份内
- 不接受收据、白条

## 报销时限
- 当月费用需在次月15日前提交
- 超过3个月的费用不予报销

## 常见问题
- **打车费**: 需附行程截图，说明起止地点和事由
- **餐费**: 需注明用餐人数和事由
- **差旅**: 需附出差审批单',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'admin1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '报销系统操作指南');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT 'Git代码仓库使用规范',
       '## 仓库地址
- GitLab: git.company.com
- 账号: 域账号登录

## 分支规范
- main: 生产分支，仅通过MR合入
- develop: 开发分支
- feature/xxx: 功能分支
- hotfix/xxx: 紧急修复分支

## 提交规范
格式: <type>(<scope>): <subject>
- feat: 新功能
- fix: 修复Bug
- docs: 文档更新
- style: 代码格式
- refactor: 重构
- test: 测试
- chore: 构建/工具

示例: feat(user): 添加用户注册功能

## Code Review
- 所有代码必须经过至少1人Review
- Review通过后才能合入develop
- 合入main需2人Review

## SSH Key配置
1. 生成SSH Key: ssh-keygen -t ed25519
2. 复制公钥: cat ~/.ssh/id_ed25519.pub
3. 在GitLab → Settings → SSH Keys 中添加',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = 'Git代码仓库使用规范');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT '公司信息安全规范',
       '## 密码管理
- 不同系统使用不同密码
- 禁止将密码写在便利贴上
- 推荐使用公司授权的密码管理器

## 数据安全
- 机密文件禁止通过个人邮箱/网盘传输
- 离开工位时锁屏（Win+L / Mac: Ctrl+Cmd+Q）
- USB设备使用前需经IT部门检测

## 网络安全
- 不点击可疑链接和附件
- 不在公共WiFi下处理敏感工作
- 发现钓鱼邮件立即报告IT安全团队

## 软件安全
- 只使用公司授权的软件
- 不安装破解/盗版软件
- 及时更新系统补丁

## 物理安全
- 访客需佩戴访客证并由员工陪同
- 敏感区域（机房等）需刷卡进入
- 离开办公室时确认门窗关闭

## 安全事件报告
发现安全问题请立即联系:
- 邮箱: security@company.com
- 内线: 8889',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'admin1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = '公司信息安全规范');

INSERT INTO knowledge_base (title, content, status, created_by, created_at, updated_at)
SELECT 'ERP系统常见问题',
       '## 登录问题
- 用域账号（工号）和域密码登录
- 首次登录需修改初始密码
- 忘记密码: 联系IT重置

## 常见错误

### "会话超时"
- ERP会话30分钟无操作自动断开
- 重新登录即可
- 建议: 编辑较长单据时先保存草稿

### "无权限访问"
- 联系部门主管申请对应模块权限
- 权限变更需1个工作日生效

### "单据提交失败"
- 检查必填字段是否完整
- 检查金额/数量是否为负数
- 检查关联单据状态是否正常
- 如仍报错，截图错误信息提交工单

### "报表导出超时"
- 缩小查询时间范围
- 减少导出字段
- 大数据量导出请在非高峰期操作（12:00-13:30或18:00后）

## 系统维护时间
每月第一个周六 22:00-次日06:00',
       'PUBLISHED',
       (SELECT id FROM users WHERE username = 'engineer1'),
       NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE title = 'ERP系统常见问题');

-- ============================================================
-- Seed sample tickets for demo
-- ============================================================

INSERT INTO tickets (title, description, status, priority, priority_reason, customer_id, assigned_engineer_id, resolution_notes, created_at, updated_at, resolved_at)
SELECT '电脑开机后黑屏无法进入系统',
       '今天早上来公司后开机，显示器亮了一下然后就黑屏了，只有一个光标在闪。重启了3次都一样。急需处理，下午有重要的客户演示。',
       'OPEN', 'HIGH', 'AI判断: 涉及硬件故障且有紧急业务需求',
       (SELECT id FROM users WHERE username = 'customer1'),
       NULL, NULL,
       NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', NULL
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = '电脑开机后黑屏无法进入系统');

INSERT INTO tickets (title, description, status, priority, priority_reason, customer_id, assigned_engineer_id, resolution_notes, created_at, updated_at, resolved_at)
SELECT 'VPN连接不上，无法远程办公',
       '从昨天开始VPN就连不上了，一直显示"连接超时"。我在家远程办公，没有VPN就完全无法访问公司内网系统。已经尝试重新安装VPN客户端，问题依然存在。',
       'IN_PROGRESS', 'HIGH', 'AI判断: 影响远程办公，阻塞工作',
       (SELECT id FROM users WHERE username = 'customer2'),
       (SELECT id FROM users WHERE username = 'engineer1'),
       NULL,
       NOW() - INTERVAL '1 day', NOW() - INTERVAL '6 hours', NULL
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'VPN连接不上，无法远程办公');

INSERT INTO tickets (title, description, status, priority, priority_reason, customer_id, assigned_engineer_id, resolution_notes, created_at, updated_at, resolved_at)
SELECT '申请安装Figma设计软件',
       '我是新来的UI设计师，需要安装Figma桌面客户端用于设计工作。目前只能用网页版，体验不太好。',
       'RESOLVED', 'LOW', 'AI判断: 非紧急软件安装请求',
       (SELECT id FROM users WHERE username = 'customer3'),
       (SELECT id FROM users WHERE username = 'engineer2'),
       '已通过公司软件白名单审批，远程协助安装了Figma桌面版(v116.15.5)。同时配置了公司代理设置确保正常访问。',
       NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = '申请安装Figma设计软件');

INSERT INTO tickets (title, description, status, priority, priority_reason, customer_id, assigned_engineer_id, resolution_notes, created_at, updated_at, resolved_at)
SELECT '3楼打印机卡纸频繁',
       '3楼的HP打印机最近一周经常卡纸，几乎每天都会卡1-2次。已经按照知识库的方法清理过纸道，但问题没有改善。可能需要专业维修。',
       'OPEN', 'MEDIUM', 'AI判断: 影响多人，但有替代打印机可用',
       (SELECT id FROM users WHERE username = 'customer1'),
       NULL, NULL,
       NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours', NULL
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = '3楼打印机卡纸频繁');

INSERT INTO tickets (title, description, status, priority, priority_reason, customer_id, assigned_engineer_id, resolution_notes, created_at, updated_at, resolved_at)
SELECT 'OA系统报销单无法提交',
       '填完报销单点提交后，页面一直转圈，最后报"网络错误"。试了Chrome和Edge都一样。报销金额是上周出差的费用，需要在月底前提交。',
       'IN_PROGRESS', 'MEDIUM', 'AI判断: 影响财务流程，有时间限制',
       (SELECT id FROM users WHERE username = 'customer3'),
       (SELECT id FROM users WHERE username = 'engineer1'),
       NULL,
       NOW() - INTERVAL '8 hours', NOW() - INTERVAL '3 hours', NULL
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'OA系统报销单无法提交');
