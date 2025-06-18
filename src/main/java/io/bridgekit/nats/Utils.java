package io.bridgekit.nats;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.gson.Gson;

public class Utils {
    /**
     * A cryptographically secure random number generator you can use to do whatever random stuff you want!
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * The 62 characters that make up the set of valid alphanumeric runes.
     */
    private static final String ALPHANUMERIC_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * Our singleton JSON parser.
     */
    private static final Gson GSON = new Gson();

    /**
     * Returns a random integer between these two numbers.
     *
     * @param min The lower bound (inclusive)
     * @param max The upper bound (exclusive)
     */
    public static int randomInt(int min, int max) {
        return RANDOM.nextInt(max - min) + min;
    }

    /**
     * Parses the given value as an integer. Null/empty strings and invalid formats silently result in zero.
     *
     * @param value The numeric string to parse.
     * @return The parsed integer; zero if there's any errors.
     */
    public static int parseInt(String value) {
        try {
            return value == null || value.isEmpty() ? 0 : Integer.parseInt(value);
        }
        catch (Exception e) {
            return 0;
        }
    }

    /**
     * Generates a random alphanumeric string of the specified length.
     *
     * @param length The length of the random string to generate
     * @return A random string containing alphanumeric characters (a-z, A-Z, 0-9)
     */
    public static String randomAlphanumeric(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = RANDOM.nextInt(ALPHANUMERIC_CHARS.length());
            result.append(ALPHANUMERIC_CHARS.charAt(randomIndex));
        }
        return result.toString();
    }

    /**
     * Randomly generates either 'true' or 'false'. I thought this one was pretty self-explanatory.
     */
    public static boolean randomBoolean() {
        return RANDOM.nextBoolean();
    }

    /**
     * Simply a more expressive way to indicate how long the current thread should sleep. It also gobbles
     * up and ignores the potential interrupt exception, so the demo code isn't littered with unnecessary
     * crap just to make the compiler happy.
     */
    public static void sleepSeconds(long numSeconds) {
        try {
            Thread.sleep(numSeconds * 1000);
        }
        catch (InterruptedException e) {
            // Ignore
        }
    }

    /**
     * A null/empty safe way to check the value of the first element of main's "args" array.
     *
     * @param args The command line arguments for main()
     * @return The first argument (i.e. args[0])
     * @throws IllegalArgumentException If there were no arguments provided.
     */
    public static String firstArg(String[] args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Missing command line arguments.");
        }
        return args[0];
    }

    /**
     * A null/empty safe way to check the value of the first element of main's "args" array.
     *
     * @param args The command line arguments for main()
     * @return The first argument (i.e. args[0]) - or "" if there were no args passed.
     */
    public static String firstArgOptional(String[] args) {
        return args == null || args.length == 0 ? "" : args[0];
    }

    /**
     * Converts a raw byte array of characters into a String.
     */
    public static String asString(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Invokes the suppliers one by one until we get a non-null value, and that's what gets put in
     * the Optional bucket. If all of the suppliers result in null, you'll get an empty/nullable Optional.
     */
    @SafeVarargs
    public static <T> Optional<T> optional(Supplier<Optional<T>> ... suppliers) {
        for (var supplier : suppliers) {
            Optional<T> value = supplier.get();
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the MIME content type based on the give file name's extension. This is not an exhaustive
     * list of file types - just enough for us to have some variations in the demos.
     */
    public static String mimeType(String fileName) {
        fileName = fileName.toLowerCase();

        if (fileName.endsWith(".jpg")) {
            return "image/jpeg";
        }
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }

    /**
     * Creates a file reference for the given path. If the parent directory does not already
     * exist, this will create it.
     *
     * @param dirPath  The path to the directory where the file resides / should reside.
     * @param fileName The name of the actual file.
     */
    public static File asFile(String dirPath, String fileName) {
        var dir = new File(dirPath);
        dir.mkdirs();
        return new File(dir, fileName);
    }

    /**
     * Formats the object as a simple JSON string.
     */
    public static String marshalJSON(Object object) {
        return GSON.toJson(object);
    }

    /**
     * Parses the given JSON and overlays its values onto a new instance of the given type.
     */
    public static <T> T unmarshalJSON(byte[] json, Class<T> clazz) {
        return unmarshalJSON(asString(json), clazz);
    }

    /**
     * Parses the given JSON and overlays its values onto a new instance of the given type.
     */
    public static <T> T unmarshalJSON(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    /**
     * Returns true if the given string is non-null with at least one non-whitespace character.
     */
    public static boolean hasText(String string) {
        return string != null && !string.trim().isEmpty();
    }

    /**
     * Closes the given resource, quietly ignoring cases where the resource is null or fails to close cleanly.
     */
    public static void closeQuietly(AutoCloseable resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        }
        catch (Exception e) {
            // Ignore... this is supposed to be quiet.
        }
    }

    public static void closeOnShutdown(AutoCloseable resource) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> closeQuietly(resource)));
    }
}
