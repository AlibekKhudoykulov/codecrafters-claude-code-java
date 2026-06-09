package tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionTool;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of available tools, indexed by name.
 * Provides specifications for the LLM and dispatches execution to the right tool.
 */
public final class ToolRegistry {

    private final Map<String, Tool> toolsByName;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolRegistry(List<Tool> tools) {
        this.toolsByName = new HashMap<>();
        for (Tool tool : tools) {
            toolsByName.put(tool.name(), tool);
        }
    }

    /** Returns specifications for all registered tools (sent to the LLM). */
    public List<ChatCompletionTool> specifications() {
        return toolsByName.values().stream()
                .map(Tool::specification)
                .toList();
    }

    /** Executes a tool call and returns the result as a string. */
    public String execute(ChatCompletionMessageToolCall toolCall) throws Exception {
        String name = toolCall.function().name();
        Tool tool = toolsByName.get(name);

        if (tool == null) {
            return "Error: unknown tool '" + name + "'";
        }

        JsonNode arguments = objectMapper.readTree(toolCall.function().arguments());
        return tool.execute(arguments);
    }
}