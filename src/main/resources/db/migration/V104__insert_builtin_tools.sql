-- ============================================================================
-- V104: 插入内置 ATOMIC 工具数据
-- 包含所有平台内建工具的 tools 主表记录、tool_versions 版本记录，
-- 以及 published_version_id 的回填更新。
-- ============================================================================

-- ============================================================================
-- 1. tool 主表（11 个内置工具）
-- ============================================================================
INSERT INTO tool (id, name, introduction, tool_type, visibility, builtin, tags, creator_type, status, created_at) VALUES
('builtin-bash',           'Bash',           'Execute a bash command in the terminal',                                          'ATOMIC', 'PUBLIC', 1, '["shell","执行","命令行"]',       'OFFICIAL', 'ACTIVE', NOW()),
('builtin-read',           'Read',           'Read a file from the filesystem',                                                  'ATOMIC', 'PUBLIC', 1, '["文件","读取"]',                 'OFFICIAL', 'ACTIVE', NOW()),
('builtin-file-edit',      'FileEdit',       'Edit an existing file using exact string replacement',                             'ATOMIC', 'PUBLIC', 1, '["文件","编辑"]',                 'OFFICIAL', 'ACTIVE', NOW()),
('builtin-file-write',     'FileWrite',      'Create or overwrite a file with new content',                                     'ATOMIC', 'PUBLIC', 1, '["文件","写入"]',                 'OFFICIAL', 'ACTIVE', NOW()),
('builtin-grep',           'Grep',           'Search file contents using regular expressions (ripgrep)',                         'ATOMIC', 'PUBLIC', 1, '["搜索","文件","正则"]',          'OFFICIAL', 'ACTIVE', NOW()),
('builtin-glob',           'Glob',           'Find files by name pattern or wildcard',                                          'ATOMIC', 'PUBLIC', 1, '["搜索","文件","通配符"]',        'OFFICIAL', 'ACTIVE', NOW()),
('builtin-web-search',     'WebSearch',      'Search the web for information',                                                   'ATOMIC', 'PUBLIC', 1, '["搜索","网络"]',                 'OFFICIAL', 'ACTIVE', NOW()),
('builtin-web-fetch',      'WebFetch',       'Fetch and extract content from a URL',                                            'ATOMIC', 'PUBLIC', 1, '["网络","抓取","URL"]',           'OFFICIAL', 'ACTIVE', NOW()),
('builtin-todo-write',     'TodoWrite',      'Manage the session task checklist',                                               'ATOMIC', 'PUBLIC', 1, '["任务","清单"]',                 'OFFICIAL', 'ACTIVE', NOW()),
('builtin-enter-plan',     'EnterPlanMode',  'Switch to plan mode to design an approach before coding',                         'ATOMIC', 'PUBLIC', 1, '["规划","模式切换"]',             'OFFICIAL', 'ACTIVE', NOW()),
('builtin-exit-plan',      'ExitPlanMode',   'Exit plan mode and present the implementation plan to the user for approval',      'ATOMIC', 'PUBLIC', 1, '["规划","模式切换"]',             'OFFICIAL', 'ACTIVE', NOW()),
('builtin-ask-question',   'AskUserQuestion','Ask the user clarifying questions when requirements are unclear',                  'ATOMIC', 'PUBLIC', 1, '["交互","提问"]',                 'OFFICIAL', 'ACTIVE', NOW()),
('builtin-browser',        'Browser',        'Control a web browser to navigate pages, take screenshots, and interact with UI', 'ATOMIC', 'PUBLIC', 1, '["浏览器","网页","自动化"]',      'OFFICIAL', 'ACTIVE', NOW());

-- ============================================================================
-- 2. tool_version 版本表（每个工具对应一个初始版本）
--    parameters 字段：Map<String, ParameterSchema> 的 JSON 序列化
--    required_params 字段：必填参数名的 JSON 数组
--    execution_config 字段：ATOMIC 类型固定格式 {"className": "..."}
-- ============================================================================

-- ── Bash ─────────────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-bash', 1, 'Bash',
  'Execute a bash command in the terminal. Use this to run shell commands, scripts, file operations, package management, and any other terminal tasks.',
  '{
    "command":          {"type": "string",  "description": "The bash command to execute. Can include pipes, redirections, and multi-line commands."},
    "timeout":          {"type": "integer", "description": "Timeout in milliseconds. Defaults to 120000 (2 minutes). Maximum 600000 (10 minutes)."},
    "description":      {"type": "string",  "description": "A short human-readable description of what the command does, shown in the UI."},
    "run_in_background":{"type": "boolean", "description": "If true, run the command in background and return immediately without waiting for output."}
  }',
  '["command"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.BashTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── Read ─────────────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-read', 1, 'Read',
  'Read a file from the filesystem. Supports text files (with line numbers), images (base64), and PDF files. Use offset and limit to read large files in chunks.',
  '{
    "file_path": {"type": "string",  "description": "The absolute path to the file to read."},
    "offset":    {"type": "integer", "description": "The line number to start reading from (1-based). Only for text files."},
    "limit":     {"type": "integer", "description": "The maximum number of lines to read. Only for text files."}
  }',
  '["file_path"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.FileReadTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── FileEdit ─────────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-file-edit', 1, 'FileEdit',
  'Edit an existing file by replacing a specific string with new content. The old_string must match exactly (including whitespace). For new files, set old_string to empty string.',
  '{
    "file_path":   {"type": "string",  "description": "The absolute path to the file to modify (must be absolute, not relative)."},
    "old_string":  {"type": "string",  "description": "The text to replace. Must match the existing file content exactly, including whitespace. Use empty string to create a new file or write to an empty file."},
    "new_string":  {"type": "string",  "description": "The text to replace old_string with."},
    "replace_all": {"type": "boolean", "description": "If true, replace all occurrences of old_string. If false (default), only replace one occurrence (which must be unique)."}
  }',
  '["file_path", "old_string", "new_string"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.FileEditTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── FileWrite ─────────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-file-write', 1, 'FileWrite',
  'Create a new file or completely overwrite an existing file with the given content. Prefer FileEdit for modifying existing files to avoid overwriting unintended content.',
  '{
    "file_path": {"type": "string", "description": "The absolute path to the file to write (must be absolute, not relative)."},
    "content":   {"type": "string", "description": "The content to write to the file."}
  }',
  '["file_path", "content"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.FileWriteTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── Grep ──────────────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-grep', 1, 'Grep',
  'Search file contents using regular expressions (powered by ripgrep). Supports multiple output modes: content lines, matching file paths, or match counts.',
  '{
    "pattern":        {"type": "string",  "description": "The regular expression pattern to search for in file contents."},
    "path":           {"type": "string",  "description": "File or directory to search in. Defaults to current working directory."},
    "glob":           {"type": "string",  "description": "Glob pattern to filter files (e.g. ''*.java'', ''*.{ts,tsx}'')."},
    "output_mode":    {"type": "string",  "description": "Output mode: ''content'' shows matching lines, ''files_with_matches'' shows file paths, ''count'' shows match counts. Defaults to ''files_with_matches''.", "enumValues": ["content", "files_with_matches", "count"]},
    "case_insensitive":{"type": "boolean","description": "Case insensitive search."},
    "context_lines":  {"type": "integer", "description": "Number of lines to show before and after each match."},
    "head_limit":     {"type": "integer", "description": "Limit output to first N lines/entries. Defaults to 250. Pass 0 for unlimited."},
    "offset":         {"type": "integer", "description": "Skip first N lines/entries before applying head_limit. Defaults to 0."}
  }',
  '["pattern"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.GrepTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── Glob ──────────────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-glob', 1, 'Glob',
  'Find files by name pattern or wildcard. Searches recursively from the given directory. Results are sorted by modification time (most recent first).',
  '{
    "pattern": {"type": "string", "description": "The glob pattern to match files against. Examples: **/*.java, src/**/*.ts, *.json."},
    "path":    {"type": "string", "description": "The directory to search in. If not specified, the current working directory will be used."}
  }',
  '["pattern"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.GlobTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── WebSearch ─────────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-web-search', 1, 'WebSearch',
  'Search the web for up-to-date information. Returns a list of relevant results with titles, URLs, and snippets.',
  '{
    "query":       {"type": "string",  "description": "The search query."},
    "num_results": {"type": "integer", "description": "Number of results to return (1-20, default 5)."}
  }',
  '["query"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.WebSearchTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── WebFetch ──────────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-web-fetch', 1, 'WebFetch',
  'Fetch content from a URL and extract specific information using a prompt. Useful for reading documentation, articles, or any web page content.',
  '{
    "url":    {"type": "string", "description": "The URL to fetch content from."},
    "prompt": {"type": "string", "description": "The prompt to run on the fetched content - describes what information you want to extract."}
  }',
  '["url", "prompt"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.WebFetchTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── TodoWrite ─────────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-todo-write', 1, 'TodoWrite',
  'Manage the session task checklist. Use this to create, update, and track TODO items during complex multi-step tasks.',
  '{
    "todos": {
      "type": "array",
      "description": "The updated todo list.",
      "items": {
        "type": "object",
        "properties": {
          "id":      {"type": "string", "description": "Unique identifier for the TODO item."},
          "content": {"type": "string", "description": "The description/content of the TODO item."},
          "status":  {"type": "string", "description": "The current status of the TODO item.", "enumValues": ["pending", "in_progress", "completed", "cancelled"]}
        },
        "required": ["id", "content", "status"]
      }
    }
  }',
  '["todos"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.TodoWriteTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── EnterPlanMode ─────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-enter-plan', 1, 'EnterPlanMode',
  'Switch to plan mode. Use this before starting complex coding tasks to think through the approach, explore the codebase, and design a solution without making changes.',
  '{}',
  '[]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.EnterPlanModeTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── ExitPlanMode ──────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-exit-plan', 1, 'ExitPlanMode',
  'Exit plan mode and present the implementation plan to the user for approval. The user will review and either approve or request changes.',
  '{
    "plan": {"type": "string", "description": "The implementation plan to present to the user for approval. Should include: approach, files to create/modify, key design decisions."}
  }',
  '["plan"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.ExitPlanModeTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── AskUserQuestion ───────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-ask-question', 1, 'AskUserQuestion',
  'Ask the user clarifying questions when requirements are ambiguous or additional information is needed. Supports multiple choice options.',
  '{
    "questions": {
      "type": "array",
      "description": "Questions to ask the user (1-4 questions).",
      "items": {
        "type": "object",
        "properties": {
          "question": {"type": "string", "description": "The complete question to ask the user."},
          "header":   {"type": "string", "description": "Very short label displayed as a chip/tag."},
          "options":  {
            "type": "array",
            "description": "The available choices (2-4 options).",
            "items": {
              "type": "object",
              "properties": {
                "label":       {"type": "string", "description": "Display text for this option."},
                "description": {"type": "string", "description": "Explanation of this option."}
              }
            }
          }
        },
        "required": ["question"]
      }
    }
  }',
  '["questions"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.AskUserQuestionTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── Browser ───────────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-browser', 1, 'Browser',
  'Control a web browser to navigate pages, take screenshots, interact with UI elements, and extract content. Supports both Chrome (via CDP) and Playwright.',
  '{
    "action":        {"type": "string",  "description": "Browser action to perform.", "enumValues": ["status","start","stop","profiles","tabs","open","focus","close","snapshot","screenshot","navigate","console","pdf","upload","dialog","act"]},
    "profile":       {"type": "string",  "description": "Browser profile name to use."},
    "targetUrl":     {"type": "string",  "description": "URL to navigate to."},
    "targetId":      {"type": "string",  "description": "Target tab or window ID."},
    "node":          {"type": "string",  "description": "DOM node reference for interaction."},
    "selector":      {"type": "string",  "description": "CSS selector for element targeting."},
    "frame":         {"type": "string",  "description": "Frame reference for interaction."},
    "element":       {"type": "string",  "description": "Element description for interaction."},
    "inputRef":      {"type": "string",  "description": "Input element reference."},
    "level":         {"type": "string",  "description": "Snapshot detail level."},
    "promptText":    {"type": "string",  "description": "Text input for type/fill actions."},
    "ref":           {"type": "string",  "description": "Aria or role reference."},
    "target":        {"type": "string",  "description": "Target context.", "enumValues": ["sandbox","host","node"]},
    "snapshotFormat":{"type": "string",  "description": "Snapshot output format.", "enumValues": ["aria","ai"]},
    "mode":          {"type": "string",  "description": "Operation mode.", "enumValues": ["efficient"]},
    "refs":          {"type": "string",  "description": "Reference type to include.", "enumValues": ["role","aria"]},
    "type":          {"type": "string",  "description": "Screenshot image type.", "enumValues": ["png","jpeg"]},
    "limit":         {"type": "number",  "description": "Result limit."},
    "maxChars":      {"type": "number",  "description": "Maximum characters in output."},
    "depth":         {"type": "number",  "description": "Snapshot tree depth."},
    "timeoutMs":     {"type": "number",  "description": "Action timeout in milliseconds."},
    "width":         {"type": "number",  "description": "Viewport or screenshot width."},
    "height":        {"type": "number",  "description": "Viewport or screenshot height."},
    "interactive":   {"type": "boolean", "description": "Include only interactive elements in snapshot."},
    "compact":       {"type": "boolean", "description": "Use compact snapshot format."},
    "labels":        {"type": "boolean", "description": "Include labels in snapshot."},
    "fullPage":      {"type": "boolean", "description": "Capture full page screenshot."},
    "accept":        {"type": "boolean", "description": "Accept dialog (true) or dismiss (false)."},
    "headless":      {"type": "boolean", "description": "Launch browser in headless mode."},
    "paths":         {"type": "array",   "description": "File paths for upload action.", "items": {"type": "string"}},
    "request":       {"type": "object",  "description": "Action request for ''act''. Required field: kind. Supported kinds: click, type, press, hover, scrollIntoView, drag, select, fill, resize, wait, evaluate, goBack, goForward, close."}
  }',
  '["action"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.BrowserTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ============================================================================
-- 3. 回填 tool.published_version_id
-- ============================================================================
UPDATE tool t
JOIN tool_version v ON v.tool_id = t.id AND v.version = 1
SET t.published_version_id = v.id
WHERE t.builtin = 1
  AND t.id IN (
    'builtin-bash', 'builtin-read', 'builtin-file-edit', 'builtin-file-write',
    'builtin-grep', 'builtin-glob', 'builtin-web-search', 'builtin-web-fetch',
    'builtin-todo-write', 'builtin-enter-plan', 'builtin-exit-plan',
    'builtin-ask-question', 'builtin-browser'
  );

