package io.bridgekit.nats;

import java.time.LocalTime;

/**
 * It's not the world's best logger... but it *is* the world's worst logger.
 */
public class Logger {
    /** ANSI Terminal color code for green. */
    private static final String COLOR_GREEN = "\033[0;32m";
    /** ANSI Terminal color code for grey. */
    private static final String COLOR_GREY = "\033[1;30m";
    /** ANSI Terminal color code for red. */
    private static final String COLOR_RED = "\033[0;31m";
    /** ANSI Terminal color code for going back to default color. */
    private static final String COLOR_DEFAULT = "\033[0m";

    private final String name;

    private Logger(String name) {
        this.name = name;
    }

    public static Logger instance(Class<?> context) {
        return new Logger(context.getSimpleName());
    }

    public static Logger instance(String context) {
        return new Logger(context);
    }

    public void info(String message, Object... args) {
        print("INFO", COLOR_GREEN, message, args); // yes... a space so all levels print 5 chars.
    }

    public void error(String message, Object... args) {
        print("ERROR", COLOR_RED, message, args);
    }

    public void error(Throwable t, String message, Object... args) {
        print("ERROR", COLOR_RED, message, args);
        t.printStackTrace(System.out);
    }

    private void print(String level, String colorCode, String message, Object... args) {
        System.out.printf("%s%s %s %s%20s%s %s\n",
            COLOR_GREY,
            LocalTime.now().toString().substring(0, 12), // ms is good enough, don't need nanos
            level,
            colorCode,
            name,
            COLOR_DEFAULT,
            String.format(message, args)
        );
    }
}
