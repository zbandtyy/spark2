package yolo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Util class to read image, graphDef and label files.
 */
public final class IOUtil {
    private final static Logger LOGGER = LoggerFactory.getLogger(IOUtil.class);
    private IOUtil() {}


    public static void createDirIfNotExists(final File directory) {
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public static String getFileName(final String path) {
        return path.substring(path.lastIndexOf("/") + 1, path.length());
    }
}
