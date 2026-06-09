import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.*;
import config.AgentConfig;
import tools.BashTool;
import tools.ReadTool;
import tools.ToolRegistry;
import tools.WriteTool;
import util.MessageConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the agent loop: send messages to the LLM, execute tool calls,
 * and repeat until the model produces a final response.
 */
public final class Agent {

    private static final int MAX_ITERATIONS = 25;

    private final AgentConfig config;
    private final OpenAIClient client;
    private final ToolRegistry tools;

    public Agent(AgentConfig config) {
        this.config = config;
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(config.apiKey())
                .baseUrl(config.baseUrl())
                .build();
        this.tools = new ToolRegistry(List.of(
                new ReadTool(),
                new WriteTool(),
                new BashTool()
        ));
    }

    /**
     * Runs the agent loop with the given user prompt.
     *
     * @return the final assistant response
     * @throws IllegalStateException if the loop exceeds the iteration limit
     */
    public String run(String prompt) throws Exception {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(buildUserMessage(prompt));

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            ChatCompletionMessage assistantMessage = sendRequest(messages);
            messages.add(ChatCompletionMessageParam.ofAssistant(
                    MessageConverter.toAssistantParam(assistantMessage)
            ));

            if (!hasToolCalls(assistantMessage)) {
                return assistantMessage.content().orElse("");
            }

            executeAndAppendToolResults(assistantMessage, messages);
        }

        throw new IllegalStateException(
                "Agent exceeded maximum iterations (" + MAX_ITERATIONS + ")");
    }

    private ChatCompletionMessage sendRequest(List<ChatCompletionMessageParam> messages) {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(config.model())
                .maxTokens(config.maxTokens())
                .messages(messages);

        tools.specifications().forEach(builder::addTool);

        ChatCompletion response = client.chat().completions().create(builder.build());
        if (response.choices().isEmpty()) {
            throw new IllegalStateException("API returned no choices");
        }
        return response.choices().get(0).message();
    }

    private void executeAndAppendToolResults(
            ChatCompletionMessage assistantMessage,
            List<ChatCompletionMessageParam> messages) throws Exception {

        for (ChatCompletionMessageToolCall toolCall : assistantMessage.toolCalls().get()) {
            String result = tools.execute(toolCall);
            messages.add(ChatCompletionMessageParam.ofTool(
                    ChatCompletionToolMessageParam.builder()
                            .toolCallId(toolCall.id())
                            .content(result)
                            .build()
            ));
        }
    }

    private static boolean hasToolCalls(ChatCompletionMessage message) {
        return message.toolCalls().isPresent() && !message.toolCalls().get().isEmpty();
    }

    private static ChatCompletionMessageParam buildUserMessage(String prompt) {
        return ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                        .content(prompt)
                        .build()
        );
    }
}