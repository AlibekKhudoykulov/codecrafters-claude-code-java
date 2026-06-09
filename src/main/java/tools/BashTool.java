package tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionTool;

import java.util.List;
import java.util.Map;

/**
 * Executes a shell command via {@code sh -c} and returns combined stdout/stderr.
 *
 * <p>WARNING: This tool gives the LLM unrestricted shell access. In production,
 * commands should be sandboxed (e.g., run inside a container) and validated.
 */
public final class BashTool implements Tool {

    private static final String NAME = "Bash";
    private static final String DESCRIPTION = "Execute a shell command";
    private static final String PARAM_COMMAND = "command";

    private static final String SHELL = "sh";
    private static final String SHELL_FLAG = "-c";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ChatCompletionTool specification() {
        Map<String, Object> properties = Map.of(
                PARAM_COMMAND, Map.of(
                        "type", "string",
                        "description", "The command to execute"
                )
        );

        FunctionParameters parameters = FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("required", JsonValue.from(List.of(PARAM_COMMAND)))
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
        String command = arguments.get(PARAM_COMMAND).asText();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(SHELL, SHELL_FLAG, command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                return String.format("Command failed (exit %d):%n%s", exitCode, output);
            }
            return output.isEmpty() ? "Command executed successfully" : output;
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }
}