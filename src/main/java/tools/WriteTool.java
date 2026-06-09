package tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionTool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/** Writes content to a file, creating parent directories if needed. */
public final class WriteTool implements Tool {

    private static final String NAME = "Write";
    private static final String DESCRIPTION = "Write content to a file";
    private static final String PARAM_FILE_PATH = "file_path";
    private static final String PARAM_CONTENT = "content";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ChatCompletionTool specification() {
        Map<String, Object> properties = Map.of(
                PARAM_FILE_PATH, Map.of(
                        "type", "string",
                        "description", "The path to the file to write"
                ),
                PARAM_CONTENT, Map.of(
                        "type", "string",
                        "description", "The content to write to the file"
                )
        );

        FunctionParameters parameters = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("required",
                        JsonValue.from(List.of(PARAM_FILE_PATH, PARAM_CONTENT)))
                .build();

        return ChatCompletionTool.builder()
                .type(JsonValue.from("function"))
                .function(FunctionDefinition.builder()
                        .name(NAME)
                        .description(DESCRIPTION)
                        .parameters(parameters)
                        .build())
                .build();
    }

    @Override
    public String execute(JsonNode arguments) {
        String filePath = arguments.get(PARAM_FILE_PATH).asText();
        String content = arguments.get(PARAM_CONTENT).asText();

        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content);
            return "File written successfully: " + filePath;
        } catch (Exception e) {
            return "Error writing file '" + filePath + "': " + e.getMessage();
        }
    }
}