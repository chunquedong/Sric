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
import sc2.compiler.resolve.ExprTypeResolver;
import sc2.compiler.resolve.TopLevelTypeResolver;

/**
 *
 * @author yangjiandong
 */
public class TypeCheckTest {
    @Test
    public void test() throws IOException {
        String file = "res/code/testExpr.sc";
        String libPath = "res/lib";
        
        sc2.compiler.Compiler compiler = sc2.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        compiler.run();
    }
    
    @Test
    public void testAll() throws IOException {
        String libPath = "res/lib";
        
        File[] list = new File("res/code").listFiles();
        for (File file : list) {
            if (!file.getName().endsWith(".sc")) {
                continue;
            }
        
            sc2.compiler.Compiler compiler = sc2.compiler.Compiler.makeDefault(file.getPath(), libPath);
            compiler.genCode = false;
            compiler.run();
        }
    }
}
