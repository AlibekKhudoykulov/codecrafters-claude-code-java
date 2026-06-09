package config;

/**
 * Holds runtime configuration for the agent.
 * Values are loaded from environment variables.
 */
public final class AgentConfig {

    private static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";
    private static final String DEFAULT_MODEL = "anthropic/claude-haiku-4.5";
    private static final int DEFAULT_MAX_TOKENS = 1000;

    private static final String ENV_API_KEY = "OPENROUTER_API_KEY";
    private static final String ENV_BASE_URL = "OPENROUTER_BASE_URL";

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int maxTokens;

    private AgentConfig(String apiKey, String baseUrl, String model, int maxTokens) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    public static AgentConfig fromEnvironment() {
        String apiKey = System.getenv(ENV_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(ENV_API_KEY + " environment variable is not set");
        }

        String baseUrl = System.getenv(ENV_BASE_URL);
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }

        return new AgentConfig(apiKey, baseUrl, DEFAULT_MODEL, DEFAULT_MAX_TOKENS);
    }

    public String apiKey()   { return apiKey; }
    public String baseUrl()  { return baseUrl; }
    public String model()    { return model; }
    public int maxTokens()   { return maxTokens; }
}