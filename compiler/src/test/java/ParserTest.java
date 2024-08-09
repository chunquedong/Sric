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
import sc2.compiler.CompilerLog;
import sc2.compiler.Util;
import sc2.compiler.ast.AstNode.FileUnit;
import sc2.compiler.ast.Token;
import sc2.compiler.backend.CppGenerator;
import sc2.compiler.parser.Parser;
import sc2.compiler.parser.Tokenizer;

/**
 *
 * @author yangjiandong
 */
public class ParserTest {
    @Test
    public void testSys() throws IOException {
        String file = "res/code/testStruct.sc";
        String src = Files.readString(Path.of(file));
        
        CompilerLog log = new CompilerLog();
        FileUnit unit = new FileUnit(file);
        Parser parser = new Parser(log, src, unit);
        parser.parse();
        assertTrue(log.errors.size() == 0);
        
        CppGenerator generator = new CppGenerator(log, System.out);
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
            Parser parser = new Parser(log, src, unit);
            parser.parse();
            
            if (log.hasError()) {
                String name = Util.getBaseName(file.getName());
                GoldenTest.verifyGolden(log.toString(), "parser", name+".cpp");
                return;
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            CppGenerator generator = new CppGenerator(log, new PrintStream(stream));
            unit.walkChildren(generator);
            
            String str = stream.toString("UTF-8");
            String name = file.getName().substring(0, file.getName().lastIndexOf("."));
            GoldenTest.verifyGolden(str, "parser", name+".cpp");
        }
    }
}
