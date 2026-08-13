public class ConsoleColors {
    // Reset
    public static final String RESET = "\033[0m";  // Text Reset

    // System Messages (Yellow)
    public static final String SYS = "\033[1;33m";

    // Prompts (Cyan)
    public static final String PROMPT = "\033[1;36m";

    // Peer Messages (Green)
    public static final String PEER_MSG = "\033[1;32m";

    // Errors & Disconnects (Red)
    public static final String ERROR = "\033[1;31m";

    // Highlights (Purple)
    public static final String HIGHLIGHT = "\033[1;35m";

    // Chat Mode Specifics
    public static final String CHAT_BANNER = "\033[45m\033[1;37m"; // Purple background, White bold text
    public static final String CHAT_PROMPT = "\033[1;33m"; // Bright Yellow for your typing prompt

}