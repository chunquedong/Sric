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
import sric.compiler.resolve.ExprTypeResolver;
import sric.compiler.resolve.TopLevelTypeResolver;

/**
 *
 * @author yangjiandong
 */
public class TypeCheckTest {
    @Test
    public void test() throws IOException {
        String file = "res/code/testOperator.sc";
        String libPath = "res/lib";
        
        sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        boolean res = compiler.run();
        assertTrue(res);
    }
    
    @Test
    public void testAll() throws IOException {
        String libPath = "res/lib";
        
        File[] list = new File("res/code").listFiles();
        for (File file : list) {
            if (!file.getName().endsWith(".sc")) {
                continue;
            }
        
            sric.compiler.Compiler compiler = sric.compiler.Compiler.makeDefault(file.getPath(), libPath);
            compiler.genCode = false;
            boolean res = compiler.run();
            assertTrue(res);
        }
    }
}
