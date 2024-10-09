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
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import sric.compiler.CompilerLog;
import sric.compiler.Util;
import sric.compiler.ast.AstNode;
import sric.compiler.backend.CppGenerator;
import sric.compiler.parser.DeepParser;
import sric.compiler.parser.Parser;


/**
 *
 * @author yangjiandong
 */
public class DeepParserTest {
    @Test
    public void test() throws IOException {
        String file = "res/code/testOperator.sc";
        String src = Files.readString(Path.of(file));
        
        CompilerLog log = new CompilerLog();
        AstNode.FileUnit unit = new AstNode.FileUnit(file);
        DeepParser parser = new DeepParser(log, src, unit);
        parser.parse();
        
        log.printError();
        assertTrue(log.errors.size() == 0);
        
        CppGenerator generator = new CppGenerator(log, System.out);
        generator.headMode = false;
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
            AstNode.FileUnit unit = new AstNode.FileUnit(file.getPath());
            DeepParser parser = new DeepParser(log, src, unit);
            parser.parse();
            
            if (log.hasError()) {
                String name = Util.getBaseName(file.getName());
                GoldenTest.verifyGolden(log.toString(), "deepParser", name+".cpp");
                continue;
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            CppGenerator generator = new CppGenerator(log, new PrintStream(stream));
            generator.headMode = false;
            unit.walkChildren(generator);
            
            String str = stream.toString("UTF-8");
            String name = file.getName().substring(0, file.getName().lastIndexOf("."));
            GoldenTest.verifyGolden(str, "deepParser", name+".cpp");
        }
    }
}
