package br.com.ingenieux.mojo.beanstalk.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

public class SourceBundleUtil {
    public static final String PROCFILE = "Procfile";
    public static final String BUILDFILE = "Buildfile";
    public static final String EBEXTENSION = ".ebextension";
    public static final String DEFAULT_MAIN_JAR_APPLICATION_NAME = "web";

    public static boolean validateProcfileLine(String line) {
        final Pattern PROCFILE_PROCESS_LINE_PATTERN = Pattern
                .compile("^[A-Za-z0-9_]+:\\s*.+$");
        return PROCFILE_PROCESS_LINE_PATTERN.matcher(line).matches();
    }

    /**
     * Inspect a file from given path to see if the file is Procfile
     * @param procfilePath given file path
     * @throws IOException
     * @throws IllegalArgumentException if the given file doesn't satisfy Procfile format
     */
    public static void validateProcfile(String procfilePath) throws IOException {
        try {
            BufferedReader br = new BufferedReader(new FileReader(procfilePath));
            String line = br.readLine();
            int lineNo = 1;
            while (line != null) {
                if (!validateProcfileLine(line)) {
                    throw new IllegalArgumentException(
                            "Procfile doesn't follow regular expression: ^[A-Za-z0-9_]+:\\s*.+$ in line "
                                    + lineNo);
                }
                if (lineNo == 1 && !line.startsWith(DEFAULT_MAIN_JAR_APPLICATION_NAME + ":")) {
                    throw new IllegalArgumentException(
                            "The command that runs the main JAR in your application must be called web, "
                            + "and it must be the first command listed in your Procfile.");
                }
                lineNo++;
                line = br.readLine();
            }
            br.close();
        } catch (FileNotFoundException e) {
            // do nothing if file doesn't exist
        }
    }

    /**
     * Aggregate all the files/directories needed to designated folder.
     *
     * @param beanstalkerFolder The destination folder.
     * @throws IOException In case IO errors happen when copying files.
     * @throws FileNotFoundException In case the artifact jar file isn't found
     */
    public static void assemble(File jarFile, File beanstalkerFolder) throws IOException {
        if (!jarFile.exists()) {
            throw new FileNotFoundException(
                          "Artifact File does not exist! (file=" + jarFile.getPath() + ")");
        }
        if (!beanstalkerFolder.exists()) {
            beanstalkerFolder.mkdir();
        } else {
            FileUtils.deleteQuietly(beanstalkerFolder);
        }
        copyFileToDirectory(jarFile, beanstalkerFolder);
        copyFileToDirectory(new File(PROCFILE), beanstalkerFolder);
        copyFileToDirectory(new File(BUILDFILE), beanstalkerFolder);
        copyFileToDirectory(new File(EBEXTENSION), beanstalkerFolder);
    }

    /**
     * Copy the source file to the destination directory. The destination directory will be
     * the parent directory of the newly copied file no matter it's a normal file or a directory.
     *
     * @param srcFile File or directory to be copied from
     * @param destDir Destination directory
     * @throws IOException IOException might be thrown while copying
     */
    private static void copyFileToDirectory(File srcFile, File destDir) throws IOException {
        if (!srcFile.exists()) {
            throw new FileNotFoundException("No source file found at (" + srcFile.getPath() + ")");
        }
        if (!destDir.exists() || !destDir.isDirectory()) {
            throw new FileNotFoundException("No destination directory found at (" + destDir.getPath() + ")");
        }
        if (srcFile.isFile()) {
            FileUtils.copyFileToDirectory(srcFile, destDir);
        } else if (srcFile.isDirectory()) {
            FileUtils.copyDirectoryToDirectory(srcFile, destDir);
        }
    }

    /**
     * Create source bundle zip file from the given folder.
     *
     * @param beanstalkerFolder The source directory folder.
     * @param sourceBundleFile The target zip file to create
     * @throws FileNotFoundException In case no source directory found
     */
    public static void createSourceBundle(File beanstalkerFolder,
            File sourceBundleFile) throws FileNotFoundException {
        if (!beanstalkerFolder.exists() || !beanstalkerFolder.isDirectory()) {
            throw new FileNotFoundException("No source directory found at (" + beanstalkerFolder.getPath() + ")");
        }
        if (sourceBundleFile.exists()) {
            FileUtils.deleteQuietly(sourceBundleFile);
        }
        ZipUtil.pack(beanstalkerFolder, sourceBundleFile);
    }
}
