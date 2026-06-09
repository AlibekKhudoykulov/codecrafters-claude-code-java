import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || !"-p".equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(1);
        }

        String prompt = args[1];

        String apiKey = System.getenv("OPENROUTER_API_KEY");
        String baseUrl = System.getenv("OPENROUTER_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENROUTER_API_KEY is not set");
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        ChatCompletionTool readTool = createReadTool();
        ChatCompletionTool writeTool = createWriteTool();

        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                        .content(prompt)
                        .build()
        ));

        while (true) {
            ChatCompletionCreateParams.Builder paramsBuilder =
                    ChatCompletionCreateParams.builder()
                            .model("anthropic/claude-haiku-4.5")
                            .addTool(readTool)
                            .addTool(writeTool)
                            .maxTokens(1000)
                            .messages(messages);

            ChatCompletion response = client.chat().completions().create(paramsBuilder.build());

            if (response.choices().isEmpty()) {
                throw new RuntimeException("no choices in response");
            }

            ChatCompletionMessage assistantMessage = response.choices().get(0).message();

            messages.add(ChatCompletionMessageParam.ofAssistant(
                    assistantMessageToParam(assistantMessage)
            ));

            if (assistantMessage.toolCalls().isEmpty() ||
                    assistantMessage.toolCalls().get().isEmpty()) {
                System.out.print(assistantMessage.content().orElse(""));
                return;
            }

            for (ChatCompletionMessageToolCall toolCall : assistantMessage.toolCalls().get()) {
                String result = executeToolCall(toolCall);

                messages.add(ChatCompletionMessageParam.ofTool(
                        ChatCompletionToolMessageParam.builder()
                                .toolCallId(toolCall.id())
                                .content(result)
                                .build()
                ));
            }
        }
    }

    private static ChatCompletionTool createReadTool() {
        Map<String, Object> filePathProperty = Map.of(
                "type", "string",
                "description", "The path to the file to read"
        );

        Map<String, Object> properties = Map.of(
                "file_path", filePathProperty
        );

        FunctionParameters parameters = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("required", JsonValue.from(List.of("file_path")))
                .build();

        FunctionDefinition function = FunctionDefinition.builder()
                .name("Read")
                .description("Read and return the contents of a file")
                .parameters(parameters)
                .build();

        return ChatCompletionTool.builder()
                .type(JsonValue.from("function"))
                .function(function)
                .build();
    }

    private static ChatCompletionTool createWriteTool(){
        Map<String, Object> filePathProperty = Map.of(
                "type", "string",
                "description", "The path to the file to write"
        );

        Map<String, Object> contentProperty = Map.of(
                "type", "string",
                "description", "The content to write to the file"
        );

        Map<String, Object> properties = Map.of(
                "file_path", filePathProperty,
                "content", contentProperty
        );

        FunctionParameters parameters = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("required", JsonValue.from(List.of("file_path", "content")))
                .build();

        FunctionDefinition function = FunctionDefinition.builder()
                .name("Write")
                .description("Write content to a file")
                .parameters(parameters)
                .build();

        return ChatCompletionTool.builder()
                .type(JsonValue.from("function"))
                .function(function)
                .build();
    }

    private static String executeToolCall(ChatCompletionMessageToolCall toolCall) throws Exception {
        String functionName = toolCall.function().name();
        String argumentsJson = toolCall.function().arguments();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode args = mapper.readTree(argumentsJson);

        if ("Read".equals(functionName)) {
            String filePath = args.get("file_path").asText();
            return Files.readString(Paths.get(filePath));
        }else if("Write".equals(functionName)){
            String filePath = args.get("file_path").asText();
            String content = args.get("content").asText();
            Files.writeString(Paths.get(filePath), content);
            return "File written successfully";
        }

        throw new RuntimeException("Unknown tool: " + functionName);
    }

    private static ChatCompletionAssistantMessageParam assistantMessageToParam(
            ChatCompletionMessage msg) {

        ChatCompletionAssistantMessageParam.Builder builder =
                ChatCompletionAssistantMessageParam.builder();

        msg.content().ifPresent(builder::content);

        msg.toolCalls().ifPresent(toolCalls -> {
            List<ChatCompletionMessageToolCall> paramCalls = new ArrayList<>();
            for (ChatCompletionMessageToolCall tc : toolCalls) {
                paramCalls.add(ChatCompletionMessageToolCall.builder()
                        .id(tc.id())
                        .function(ChatCompletionMessageToolCall.Function.builder()
                                .name(tc.function().name())
                                .arguments(tc.function().arguments())
                                .build())
                        .build());
            }
            builder.toolCalls(paramCalls);
        });

        return builder.build();
    }

}
