package br.com.ingenieux.mojo.beanstalk.util;

import static br.com.ingenieux.mojo.beanstalk.util.SourceBundleUtil.validateProcfileLine;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

@SuppressWarnings("deprecation")
public class SourceBundleUtilTest {

    @Test
    public void validateProcfileLineTest() {
        String correctLine = "web: java -jar server.jar -Xmms:256m";
        String hypenLine = "web-app: java -jar foo.jar";
        String noColonLine = "web, java -jar foo.jar";

        Assert.assertTrue(validateProcfileLine(correctLine));
        Assert.assertFalse(validateProcfileLine(hypenLine));
        Assert.assertFalse(validateProcfileLine(noColonLine));
    }

    @Test
    public void validateProcfileTest() {
        String correctProcfileFormat =
                "web: java -jar server.jar -Xmms:256m\n"
                + "cache: java -jar mycache.jar\n"
                + "web_foo: java -jar other.jar\n";
        String webIsNotFirstLine =
                "cache: java -jar mycache.jar\n"
                + "web: java -jar server.jar -Xmms:256m\n"
                + "web_foo: java -jar other.jar\n";
        try {
            File correctProcfile = createTempFileWithContent("foo", "bar", correctProcfileFormat);
            correctProcfile.deleteOnExit();
            SourceBundleUtil.validateProcfile(correctProcfile.getPath());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException thrown while Procfile's format is correct!");
        }

        try {
            File webIsNotFirstLineFile = createTempFileWithContent("bar", "foo", webIsNotFirstLine);
            webIsNotFirstLineFile.deleteOnExit();
            SourceBundleUtil.validateProcfile(webIsNotFirstLineFile.getPath());
            fail("IllegalArgumentException didn't throw while Procfile's format is incorrect!");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
        }

    }

    private File createTempFileWithContent(String prefix, String suffix, String content) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file.getPath()));
        writer.write(content);
        writer.close();
        return file;
    }

}
