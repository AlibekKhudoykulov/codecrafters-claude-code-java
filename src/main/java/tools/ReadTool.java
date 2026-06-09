package tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionTool;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/** Reads the contents of a file from the local filesystem. */
public final class ReadTool implements Tool {

    private static final String NAME = "Read";
    private static final String DESCRIPTION = "Read and return the contents of a file";
    private static final String PARAM_FILE_PATH = "file_path";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ChatCompletionTool specification() {
        Map<String, Object> properties = Map.of(
                PARAM_FILE_PATH, Map.of(
                        "type", "string",
                        "description", "The path to the file to read"
                )
        );

        FunctionParameters parameters = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("required", JsonValue.from(List.of(PARAM_FILE_PATH)))
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
    public String execute(JsonNode arguments) throws Exception {
        String filePath = arguments.get(PARAM_FILE_PATH).asText();
        try {
            return Files.readString(Paths.get(filePath));
        } catch (Exception e) {
            return "Error reading file '" + filePath + "': " + e.getMessage();
        }
    }
}