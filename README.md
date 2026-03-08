# Git Commit Analyzer 使用说明

## 一、安装 / 运行插件

### 方式 A：在开发环境中运行（调试）

1. **用 IntelliJ IDEA 打开本插件项目**
   - 打开 `git-commit-analyzer` 文件夹（选择「Open」并选中该目录）
   - 等待 Gradle 同步完成

2. **启动带插件的 IDE**
   - 打开右侧 **Gradle** 面板 → 展开 **git-commit-analyzer** → **Tasks** → **intellij**
   - 双击 **runIde**
   - 会启动一个新的 IDEA 窗口（沙箱），该窗口已安装本插件

3. **在沙箱 IDE 中打开一个 Git 项目**
   - 随便打开一个已有 Git 仓库的项目（例如你的业务代码仓库）

### 方式 B：安装已构建的插件包

1. 在项目根目录执行：
   ```bash
   ./gradlew buildPlugin
   ```
   （Windows 下可用 `gradlew.bat buildPlugin`）

2. 构建完成后，插件 zip 在：`build/distributions/git-commit-analyzer-1.0.0.zip`

3. 在 IDEA 中：**Settings/Preferences → Plugins → 齿轮图标 → Install Plugin from Disk...**，选择该 zip 安装，然后重启 IDEA。

---

## 二、配置大模型 API

使用前必须配置可调用的 LLM 接口（支持 OpenAI 兼容格式）。

1. 打开 **File → Settings**（Windows/Linux）或 **IntelliJ IDEA → Preferences**（macOS）
2. 左侧找到 **Tools → Git Commit Analyzer**
3. 填写：
   - **API Base URL**：例如 `https://api.openai.com/v1` 或你使用的兼容接口地址
   - **API Key**：你的 API 密钥
   - **Model**：模型名，如 `gpt-4`、`gpt-3.5-turbo`、`qwen-turbo` 等（按你实际使用的接口填写）
   - **超时时间**：请求超时秒数，可按需调整
   - **System Prompt**：可选，留空则使用默认分析说明；可点「恢复默认 Prompt」还原

4. 点击 **Apply** 或 **OK** 保存。

---

## 三、分析某个提交

1. **打开 Git Log**
   - 菜单 **View → Tool Windows → Git**，或点击左侧 **Git**
   - 在 Git 工具窗口中选择 **Log** 标签

2. **选中一条提交**
   - 在提交列表中**只选中一条** commit（单击即可）

3. **触发分析**
   - 在该条提交上 **右键**
   - 选择 **「AI 分析该提交」**

4. **等待分析**
   - 会先出现进度条（获取 diff → 调用大模型）
   - 完成后自动弹出结果对话框

5. **查看结果**
   - **AI 分析结果** 标签：大模型给出的摘要、风险、合并建议等
   - **原始 Diff** 标签：该 commit 的完整 diff 文本

6. **是否合并**
   - 若决定采纳分析建议，可点击 **「Cherry-Pick 该提交」**，会提示确认后执行
   - 若不需要合并，点击 **「关闭」** 即可

---

## 四、常见问题

| 情况 | 处理方式 |
|------|----------|
| 右键没有「AI 分析该提交」 | 确保**只选中了一条** commit；若仍没有，检查是否在 Git Log 面板内右键（而不是在项目树等地方） |
| 提示「未配置 API Key」 | 到 **Settings → Tools → Git Commit Analyzer** 填写 API Key 并保存 |
| 大模型调用失败 | 检查 API Base URL、API Key、Model 是否正确；网络是否可达；超时时间是否过短 |
| Diff 很大时分析很慢或失败 | 在设置里适当减小「Diff 最大字符数」，或等接口/网络稳定后再试 |
| Cherry-Pick 失败 | 可能有冲突或当前分支状态不允许，根据错误提示在终端或 Git 工具里处理冲突后再操作 |

---

## 五、小结

- **配置一次**：Settings → Tools → Git Commit Analyzer（API 地址、Key、模型）
- **使用方式**：Git Log 里选中一条 commit → 右键 → **AI 分析该提交** → 看结果 → 可选 Cherry-Pick
