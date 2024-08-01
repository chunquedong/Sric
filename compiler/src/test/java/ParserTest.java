
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.AstNode.FileUnit;
import sc2.compiler.ast.Token;
import sc2.compiler.backend.CppGenerator;
import sc2.compiler.parser.Parser;
import sc2.compiler.parser.Tokenizer;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author yangjiandong
 */
public class ParserTest {
    @Test
    public void testSys() throws IOException {
        String file = "target/test-classes/sys.sc";
        String src = Files.readString(Path.of(file));
        
        CompilerLog log = new CompilerLog();
        FileUnit unit = new FileUnit(file);
        Parser parser = new Parser(log, src, unit);
        parser.parse();
        
        CppGenerator generator = new CppGenerator(System.out);
        unit.walk(generator);
        //System.out.println(file);
    }
    
    @Test
    public void testStruct() throws IOException {
        String file = "target/test-classes/testStruct.sc";
        String src = Files.readString(Path.of(file));
        
        CompilerLog log = new CompilerLog();
        FileUnit unit = new FileUnit(file);
        Parser parser = new Parser(log, src, unit);
        parser.parse();
        
        CppGenerator generator = new CppGenerator(System.out);
        unit.walk(generator);
        //System.out.println(file);
    }
}
