import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {
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


        ChatCompletion response = client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model("anthropic/claude-haiku-4.5")
                        .addTool(readTool)
                        .addUserMessage(prompt)
                        .maxTokens(1000)
                        .build()
        );

        ChatCompletion.Choice choice = response.choices().get(0);
        ChatCompletionMessage message = choice.message();


        if (response.choices().isEmpty()) {
            throw new RuntimeException("no choices in response");
        }
        System.err.println("Logs from your program will appear here!");

        if (message.toolCalls().isPresent() && !message.toolCalls().get().isEmpty()) {
            handleToolCall(message.toolCalls().get().get(0));
        } else {
            System.out.print(message.content().orElse(""));
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

    private static void handleToolCall(ChatCompletionMessageToolCall toolCall) throws IOException {
        String functionName = toolCall.function().name();
        String argumentsJson = toolCall.function().arguments();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode args = mapper.readTree(argumentsJson);

        if ("Read".equals(functionName)) {
            String filePath = args.get("file_path").asText();
            String content = Files.readString(Paths.get(filePath));
            System.out.print(content);
        } else {
            throw new RuntimeException("Unknown tool: " + functionName);
        }

    }
}
