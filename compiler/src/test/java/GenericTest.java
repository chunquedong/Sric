//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 *
 * @author yangjiandong
 */
public class GenericTest {
    @Test
    public void test() throws IOException {
        String file = "res/code/testGeneric.sc";
        String libPath = "res/lib";
        
        sc2.compiler.Compiler compiler = sc2.compiler.Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        compiler.run();
    }
}
