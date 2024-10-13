//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import sric.compiler.CompilerLog;
import sric.compiler.Util;
import sric.compiler.ast.AstNode.FileUnit;
import sric.compiler.ast.Token;
import sric.compiler.backend.CppGenerator;
import sric.compiler.backend.ScLibGenerator;
import sric.compiler.parser.DeepParser;
import sric.compiler.parser.Parser;
import sric.compiler.parser.Tokenizer;

/**
 *
 * @author yangjiandong
 */
public class ParserTest {
    @Test
    public void test() throws IOException {
        String file = "res/code/testStruct.sc";
        String src = Files.readString(Path.of(file));
        
        CompilerLog log = new CompilerLog();
        FileUnit unit = new FileUnit(file);
        Parser parser = new DeepParser(log, src, unit);
        parser.parse();
        assertTrue(log.errors.size() == 0);
        
        ScLibGenerator generator = new ScLibGenerator(log, System.out);
        unit.walkChildren(generator);
        //System.out.println(file);
    }
    
    @Test
    public void testAll() throws IOException {
        File[] list = new File("res/code").listFiles();
        for (File file : list) {
            if (!file.getName().endsWith(".sc")) {
                continue;
            }
            String src = Files.readString(file.toPath());
        
            CompilerLog log = new CompilerLog();
            FileUnit unit = new FileUnit(file.getPath());
            Parser parser = new DeepParser(log, src, unit);
            parser.parse();
            
            if (log.hasError()) {
                String name = Util.getBaseName(file.getName());
                GoldenTest.verifyGolden(log.toString(), "parser", name+".sc");
                continue;
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ScLibGenerator generator = new ScLibGenerator(log, new PrintStream(stream));
            //generator.headMode = true;
            unit.walkChildren(generator);
            
            String str = stream.toString("UTF-8");
            String name = file.getName().substring(0, file.getName().lastIndexOf("."));
            GoldenTest.verifyGolden(str, "parser", name+".h");
        }
    }
}
