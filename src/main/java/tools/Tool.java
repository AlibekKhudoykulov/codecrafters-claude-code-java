package tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.openai.models.chat.completions.ChatCompletionTool;

/**
 * Represents a tool that the LLM can invoke.
 *
 * Each tool has:
 * - A unique name (used by the LLM to reference it)
 * - A specification (sent to the LLM so it knows the tool exists)
 * - An execution method (runs when the LLM requests this tool)
 */
public interface Tool {

    /** The tool's unique name, e.g. "Read", "Write", "Bash". */
    String name();

    /** The OpenAI-formatted specification advertised to the LLM. */
    ChatCompletionTool specification();

    /** Executes the tool with the parsed JSON arguments and returns the result. */
    String execute(JsonNode arguments) throws Exception;
}