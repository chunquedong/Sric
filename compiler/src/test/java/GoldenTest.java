//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author yangjiandong
 */
public class GoldenTest {

    public static String goldenFileDir = "./goldenFile/";

    public static File goldenFile(String dirName, String name) {
        File dir = new File(goldenFileDir + dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, name);
        return file;
    }

    public static void verifyGolden(String data, String dirName, String name) {
        try {
            File file = goldenFile(dirName, name);
            if (!file.exists()) {
                Files.writeString(file.toPath(), data, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                System.out.println("please run again");
                return;
            }

            String content = Files.readString(file.toPath());
            if (!data.equals(content)) {
                Path path = Path.of(file.getPath() + ".error");
                Files.writeString(path, data, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                fail();
            }
            else {
                assertTrue(data.length() > 0);
                Path path = Path.of(file.getPath() + ".error");
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            Logger.getLogger(GoldenTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
    }
}
