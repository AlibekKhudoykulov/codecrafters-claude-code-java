import config.AgentConfig;

/**
 * Entry point for the AI coding agent.
 *
 * Usage: java Main -p "<prompt>"
 */
public final class Main {

    private static final String PROMPT_FLAG = "-p";
    private static final int EXIT_INVALID_USAGE = 1;
    private static final int EXIT_ERROR = 2;

    public static void main(String[] args) {
        String prompt = parsePromptArgument(args);

        try {
            AgentConfig config = AgentConfig.fromEnvironment();
            Agent agent = new Agent(config);
            String result = agent.run(prompt);
            System.out.print(result);
        } catch (Exception e) {
            System.err.println("Agent failed: " + e.getMessage());
            System.exit(EXIT_ERROR);
        }
    }

    private static String parsePromptArgument(String[] args) {
        if (args.length < 2 || !PROMPT_FLAG.equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(EXIT_INVALID_USAGE);
        }
        return args[1];
    }

    private Main() {
        // Utility class — prevent instantiation
    }
}