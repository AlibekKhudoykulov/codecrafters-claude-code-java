package util;

import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts SDK response messages to parameter objects suitable for follow-up requests.
 *
 * <p>The OpenAI SDK uses different types for incoming messages and outgoing parameters,
 * so a manual conversion is required to maintain conversation history.
 */
public final class MessageConverter {

    private MessageConverter() {
        // Utility class
    }

    /** Converts an assistant response message into a parameter object. */
    public static ChatCompletionAssistantMessageParam toAssistantParam(ChatCompletionMessage message) {
        ChatCompletionAssistantMessageParam.Builder builder =
                ChatCompletionAssistantMessageParam.builder();

        message.content().ifPresent(builder::content);

        message.toolCalls().ifPresent(toolCalls -> {
            List<ChatCompletionMessageToolCall> converted = new ArrayList<>();
            for (ChatCompletionMessageToolCall tc : toolCalls) {
                converted.add(ChatCompletionMessageToolCall.builder()
                        .id(tc.id())
                        .function(ChatCompletionMessageToolCall.Function.builder()
                                .name(tc.function().name())
                                .arguments(tc.function().arguments())
                                .build())
                        .build());
            }
            builder.toolCalls(converted);
        });

        return builder.build();
    }
}
