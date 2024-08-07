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
import sc2.compiler.CompilerLog;
import sc2.compiler.Util;
import sc2.compiler.ast.AstNode;
import sc2.compiler.backend.CppGenerator;
import sc2.compiler.parser.DeepParser;
import sc2.compiler.parser.Parser;


/**
 *
 * @author yangjiandong
 */
public class DeepParserTest {
    @Test
    public void testSys() throws IOException {
        String file = "res/code/testVarArgs.sc";
        String src = Files.readString(Path.of(file));
        
        CompilerLog log = new CompilerLog();
        AstNode.FileUnit unit = new AstNode.FileUnit(file);
        DeepParser parser = new DeepParser(log, src, unit);
        parser.parse();
        
        assertTrue(log.errors.size() == 0);
        
        CppGenerator generator = new CppGenerator(System.out);
        unit.walk(generator);
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
                GoldenTest.verifyGolden(log.toString(), "parser", name+".cpp");
                return;
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            CppGenerator generator = new CppGenerator(new PrintStream(stream));
            unit.walk(generator);
            
            String str = stream.toString("UTF-8");
            String name = file.getName().substring(0, file.getName().lastIndexOf("."));
            GoldenTest.verifyGolden(str, "deepParser", name+".cpp");
        }
    }
}
