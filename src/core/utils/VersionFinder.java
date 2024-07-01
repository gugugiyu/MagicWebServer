package core.utils;

public class VersionFinder {
    /**
     * Returns the major version of any java version. For java 1.x, the major is the second part, and for the rest, it's the first part
     *
     * @return an {@code int} for the major version
     */
    public static int getJavaMajorVersion() {
        String javaVersion = System.getProperty("java.version");

        String[] versionParts = javaVersion.split("\\.");

        int majorVersion;
        if (versionParts[0].equals("1")) {
            // For Java 1.x, the major version is the second part
            majorVersion = Integer.parseInt(versionParts[1]);
        } else {
            // For Java 9 and later, the major version is the first part
            majorVersion = Integer.parseInt(versionParts[0]);
        }

        return majorVersion;
    }
}
