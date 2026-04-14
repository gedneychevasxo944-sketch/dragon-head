package org.dragon.agent.llm.caller;

import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author binarytom
 * @date 2026/4/13
 */
@SpringBootTest
class MinimaxLLMCallerTest {

    @Autowired
    @Qualifier("minimaxLLMCaller")
    private MinimaxLLMCaller minimaxLLMCaller;

    @Test
    void llmStreamCall_Test() {
        LLMRequest llmRequest = new LLMRequest();
        llmRequest.setSystemPrompt("It's a LLM test system.");
        List<LLMRequest.LLMMessage> llmMessages = new ArrayList<>();
        llmMessages.add(new LLMRequest.LLMMessage(LLMRequest.LLMMessage.Role.USER, "hello", ""));
        llmRequest.setMessages(llmMessages);
        Stream<LLMResponse> streamResponse = minimaxLLMCaller.streamCall(llmRequest);
        streamResponse.forEach(llmResponse -> System.out.println(llmResponse.getContent()));
    }

}