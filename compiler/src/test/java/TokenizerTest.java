//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.Token;
import sc2.compiler.parser.Tokenizer;

/**
 *
 * @author yangjiandong
 */
public class TokenizerTest {

    public TokenizerTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void test() throws IOException {
        String src = Files.readString(Path.of("target/test-classes/sys.sc"));
        
        CompilerLog log = new CompilerLog();
        Tokenizer toker = new Tokenizer(log, "sys.sc", src);
        ArrayList<Token> toks = toker.tokenize();
        System.out.println(toks);
        assertTrue(log.errors.size() == 0);
    }
}
