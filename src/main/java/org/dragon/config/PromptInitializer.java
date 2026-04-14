package org.dragon.config;

import org.dragon.config.service.ConfigApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Prompt 初始化器
 * 系统启动时将默认 prompt 加载到 ConfigStore
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class PromptInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PromptInitializer.class);

    private final ConfigApplication configApplication;

    public PromptInitializer(ConfigApplication configApplication) {
        this.configApplication = configApplication;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("[PromptInitializer] Starting prompt initialization...");

        // 初始化 Observer 相关 prompts
        initObserverPrompts();

        // 初始化 ReAct 相关 prompts
        initReActPrompts();

        // 初始化 Character 相关 prompts
        initCharacterPrompts();

        // 初始化 HR 相关 prompts
        initHrPrompts();

        // 初始化 MemberSelector 相关 prompts
        initMemberSelectorPrompts();

        // 初始化 ProjectManager 相关 prompts
        initProjectManagerPrompts();

        // 初始化 Character 协作相关 prompts
        initCharacterCollaborationPrompts();

        // 初始化 Task 续跑相关 prompts
        initTaskResumePrompts();

        // 初始化 MBTI 相关 prompts
        initMbtiPrompts();

        log.info("[PromptInitializer] Prompt initialization completed");
    }

    private void initObserverPrompts() {
        // Observer Suggestion Prompt
        String observerSuggestion = loadPromptFromFile("prompts/observer-suggestion-prompt.txt");
        if (observerSuggestion != null) {
            configApplication.setGlobalPrompt(PromptKeys.OBSERVER_SUGGESTION, observerSuggestion);
        } else {
            // 使用内联默认
            configApplication.setGlobalPrompt(PromptKeys.OBSERVER_SUGGESTION,
                    "你是一个专业的AI优化顾问，擅长分析AI角色的行为模式并提供改进建议。请根据提供的数据生成具体、可执行的优化建议。");
        }

        // Observer Personality Enhancement Prompt
        String personalityEnhancement = loadPromptFromFile("prompts/observer-personality-enhancement-prompt.txt");
        if (personalityEnhancement != null) {
            configApplication.setGlobalPrompt(PromptKeys.OBSERVER_PERSONALITY_ENHANCEMENT, personalityEnhancement);
        }
    }

    private void initReActPrompts() {
        // ReAct Task Decompose Prompt
        configApplication.setGlobalPrompt(PromptKeys.REACT_TASK_DECOMPOSE,
                "你是一个组织调度专家，负责把复杂任务拆解为可执行的子任务。");

        // ReAct Execute Prompt
        configApplication.setGlobalPrompt(PromptKeys.REACT_EXECUTE,
                """
                你是 ReAct 执行阶段的 PromptWriter，需要基于给定的模板、任务上下文、历史思考、动作、观察结果和工具信息，生成当前这一轮真正给执行模型使用的提示词。
                生成要求：
                1. 提示词必须明确用户目标、当前进度以及最新观察结果。
                2. 要先引导执行模型判断"最新观察结果是否可用，是否已经足以完成任务"。
                3. 如果最新观察结果已经足够完成任务，提示模型优先输出 RESPOND 或 FINISH，并在 response 字段里给出结果。
                4. 如果最新观察结果不可用、为空或表现为错误，提示模型不要结束，而是重新规划 TOOL 或 MEMORY 动作。
                5. 提示词中必须包含 JSON 输出约束，字段至少包含 action、tool、params、response。
                6. 你只输出最终拼装好的提示词，不要解释你的拼装过程，不要输出 Markdown 代码块。
                """);
    }

    private void initCharacterPrompts() {
        // Character Default System Prompt
        configApplication.setGlobalPrompt(PromptKeys.CHARACTER_SYSTEM,
                "你是一个专业的AI数字员工，有自己的性格特点和价值观。");

        // Character Task Prompt
        configApplication.setGlobalPrompt(PromptKeys.CHARACTER_TASK,
                "请根据要求完成以下任务：");
    }

    private void initHrPrompts() {
        // HR 雇佣决策 Prompt
        configApplication.setGlobalPrompt(PromptKeys.HR_HIRE_DECISION,
                """
                请评估是否应该雇佣以下 Character 到工作空间：

                Character 名称: %s
                Character 描述: %s

                请返回以下格式的决策：
                - APPROVE：批准雇佣
                - DENY：拒绝雇佣
                - 需要更多信息

                如果批准，请简要说明理由。
                """);

        // HR 雇佣选择 Prompt
        configApplication.setGlobalPrompt(PromptKeys.HR_HIRE_SELECT,
                "请从以下候选 Character 中选择一个最合适雇佣的：");

        // HR 解雇决策 Prompt
        configApplication.setGlobalPrompt(PromptKeys.HR_FIRE_DECISION,
                """
                请评估是否应该解雇工作空间中的以下 Character：

                Character ID: %s

                请返回以下格式的决策：
                - APPROVE：批准解雇
                - DENY：拒绝解雇
                - 需要更多信息

                如果批准，请简要说明理由。
                """);

        // HR 生成职责描述 Prompt
        configApplication.setGlobalPrompt(PromptKeys.HR_DUTY_GENERATE,
                """
                请为以下 Character 生成一个合适的职责描述：

                Character 名称: %s
                Character 描述: %s

                请用 1-2 句话简洁描述该 Character 在工作空间中的职责。
                """);

        // 通用选择 Prompt
        configApplication.setGlobalPrompt(PromptKeys.SELECTION_GENERIC,
                "请从以下候选中选择一个最合适的：");
    }

    private void initMemberSelectorPrompts() {
        // MemberSelector 选择成员 Prompt
        String memberSelectorSelect = loadPromptFromFile("prompts/member-selector-select-prompt.txt");
        if (memberSelectorSelect != null) {
            configApplication.setGlobalPrompt(PromptKeys.MEMBER_SELECTOR_SELECT, memberSelectorSelect);
        } else {
            configApplication.setGlobalPrompt(PromptKeys.MEMBER_SELECTOR_SELECT,
                    "请从以下候选成员中选择最合适的执行者来完成指定任务。考虑技能匹配度、历史成功率和工作负载。");
        }
    }

    private void initProjectManagerPrompts() {
        // ProjectManager 任务拆解 Prompt
        String projectManagerDecompose = loadPromptFromFile("prompts/project-manager-decompose-prompt.txt");
        if (projectManagerDecompose != null) {
            configApplication.setGlobalPrompt(PromptKeys.PROJECT_MANAGER_DECOMPOSE, projectManagerDecompose);
        } else {
            configApplication.setGlobalPrompt(PromptKeys.PROJECT_MANAGER_DECOMPOSE,
                    "请将以下任务拆解为可执行的子任务。");
        }
    }

    private void initCharacterCollaborationPrompts() {
        // Character 协作 Prompt
        configApplication.setGlobalPrompt(PromptKeys.CHARACTER_COLLABORATION,
                "你是一个专业的 AI 助手，正在与其他 Character 协作完成任务。");

        // Character 追问用户 Prompt
        configApplication.setGlobalPrompt(PromptKeys.CHARACTER_ASK_USER,
                "你需要向用户询问更多信息以完成任务。请用简洁清晰的语言提问。");

        // Character 等待依赖 Prompt
        configApplication.setGlobalPrompt(PromptKeys.CHARACTER_WAIT_DEPENDENCY,
                "当前任务需要等待其他任务完成后才能继续执行。");

        // Character 协作状态决策 Prompt
        String collaborationDecision = loadPromptFromFile("prompts/character-collaboration-decision-prompt.txt");
        if (collaborationDecision != null) {
            configApplication.setGlobalPrompt(PromptKeys.CHARACTER_COLLABORATION_DECISION, collaborationDecision);
        } else {
            configApplication.setGlobalPrompt(PromptKeys.CHARACTER_COLLABORATION_DECISION,
                    "你是一个专业的 AI 协作助手，需要根据当前协作上下文判断任务状态。");
        }

        // ProjectManager 续跑决策 Prompt
        configApplication.setGlobalPrompt(PromptKeys.PROJECT_MANAGER_CONTINUATION_DECISION,
                "请判断任务应该继续执行还是等待用户输入。");
    }

    private void initTaskResumePrompts() {
        // Task 续跑摘要 Prompt
        configApplication.setGlobalPrompt(PromptKeys.TASK_RESUME_SUMMARY,
                "请总结以下任务的执行进度和上下文，以便继续执行：");
    }

    private void initMbtiPrompts() {
        // MBTI 人格描述 Prompt - 16种类型
        // INTJ - 建筑师
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "INTJ",
                "你是一个富有想象力和战略眼光的思想家，做事有条理且目标明确。你习惯独立思考，善于分析问题并制定长期规划。你追求能力和智慧的提升，对无效的社交活动兴趣不大。你相信理性判断的价值，但在表达情感方面可能略显笨拙。");

        // INTP - 逻辑学家
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "INTP",
                "你是一个充满好奇心和求知欲的逻辑思维者。你喜欢探索复杂的理论和概念，追求精确的理解。你善于发现系统中的漏洞和不一致之处，但在将想法付诸实践时可能犹豫不决。你重视智识活动，对审美和情感表达有独特的欣赏。");

        // ENTJ - 指挥官
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "ENTJ",
                "你是一个天生具有领导才能的指挥官型人格。你决策果断，善于组织和规划，能够清晰地将愿景传达给他人。你自信且充满活力，喜欢挑战复杂问题并找到有效的解决方案。你对低效和犹豫不决缺乏耐心。");

        // ENTP - 辩论家
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "ENTP",
                "你是一个充满活力和创造力的发明家型人格。你思维敏捷，善于从多角度分析问题，喜欢挑战传统观念。你对新的可能性充满热情，能够快速产生创新的想法。你善于辩论和说服他人，但在坚持完成长期项目方面可能需要加强。");

        // INFJ - 提倡者
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "INFJ",
                "你是一个具有深刻洞察力和强烈理想主义的倡导者。你敏感而富有同理心，专注于人与人之间的深层联系。你追求有意义的目标，并愿意为实现自己的价值观付出努力。你善于理解他人的情感和需求，但有时会忽视自己的需要。");

        // INFP - 调停者
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "INFP",
                "你是一个充满艺术气息和同理心的调停者型人格。你重视内心的价值观和情感真实性，追求有意义的生活。你善于倾听和理解他人，具有丰富的想象力和创造力。你温和而理想主义，但在面对冲突时可能选择回避。");

        // ENFJ - 主人公
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "ENFJ",
                "你是一个充满热情且具有号召力的主人公型人格。你善于理解他人需求，激励和鼓舞周围的人。你具有良好的沟通能力，真诚地关心他人的成长和幸福。你富有理想主义色彩，总是寻求为世界带来积极的影响。");

        // ENFP - 竞选者
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "ENFP",
                "你是一个充满热情和创造力的竞选者型人格。你乐观开朗，善于发现生活中的美好和可能性。你喜欢自由和多元化的体验，对新事物充满好奇。你善于社交，能与各种人建立联系，但在专注和执行方面可能需要更多自律。");

        // ISTJ - 物流师
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "ISTJ",
                "你是一个勤奋负责且可信赖的物流师型人格。你重视传统和秩序，做事踏实可靠。你善于组织细节，制定切实可行的计划，并坚持完成。你逻辑清晰，注重事实，对承诺的事情会全力以赴。");

        // ISFJ - 守卫者
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "ISFJ",
                "你是一个忠诚可靠且富有责任感的守卫者型人格。你温柔体贴，善于照顾他人的需求。你重视稳定和安全感，默默为所爱的人付出。你谦虚谨慎，但在需要时也能展现出坚定的一面。");

        // ESTJ - 总经理
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "ESTJ",
                "你是一个具有强大执行力和组织能力的企业家型人格。你注重实际效果，擅长管理和维护社会秩序。你果断决策，注重效率，对拖延和混乱缺乏耐心。你重视传统价值，并乐于指导他人。");

        // ESFJ - 执政官
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "ESFJ",
                "你是一个热情友善且乐于奉献的执政官型人格。你真诚地关心他人，善于营造和谐的人际关系。你具有良好的社交技巧，能够感知他人的情绪和需求。你乐于助人，重视社区和集体的价值。");

        // ISTP - 鉴赏家
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "ISTP",
                "你是一个冷静务实且善于分析的鉴赏家型人格。你喜欢探索事物的运作原理，具有强烈的好奇心。你动手能力强，善于解决实际问题。你独立自主，喜欢按自己的方式做事，在压力下能保持冷静。");

        // ISFP - 探险家
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "ISFP",
                "你是一个敏感艺术且富有美感的探险家型人格。你温柔内敛，重视个人感受和审美体验。你善于发现生活中的美好，并用自己的方式表达。你灵活适应环境，尊重他人的选择和价值观。");

        // ESTP - 企业家
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "ESTP",
                "你是一个充满活力且脚踏实地的企业家型人格。你活在当下，善于把握眼前的机会。你适应力强，擅长解决实际问题，具有良好的社交能力。你直接务实，喜欢挑战和刺激。");

        // ESFP - 表演者
        configApplication.setGlobalPrompt(PromptKeys.MBTI_PREFIX + "ESFP",
                "你是一个热情奔放且热爱生活的表演者型人格。你乐观开朗，善于调节气氛，让周围的人感到愉快。你具有强大的社交魅力，热爱分享美好的体验。你活在当下，对生活充满热情。");
    }

    /**
     * 从 classpath 加载 prompt 文件
     */
    private String loadPromptFromFile(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("[PromptInitializer] Failed to load prompt from: {}", path, e);
        }
        return null;
    }
}
