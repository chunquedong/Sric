//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.SModule;
import sc2.compiler.ast.SModule.Depend;
import sc2.compiler.backend.CppGenerator;
import sc2.compiler.parser.DeepParser;
import sc2.compiler.resolve.ExprTypeResolver;
import sc2.compiler.resolve.SlotTypeResolver;

/**
 *
 * @author yangjiandong
 */
public class Compiler {

    public ArrayList<File> sources;
    public SModule module;
    public CompilerLog log;
    
    public String outputDir;
    public String libPath;
    
    public boolean genCode = true;
    
    public Compiler(SModule module, File sourceDir, String libPath, String outputDir) {
        this.module = module;
        log = new CompilerLog();
        this.sources = Util.listFile(sourceDir);
        this.libPath = libPath;
        this.outputDir = outputDir;
    }
    
    public static Compiler makeDefault(String sourcePath, String libPath) {
        File sourceDir = new File(sourcePath);
        
        SModule module = new SModule();
        Depend selfDepend = Depend.fromFile(sourceDir);
        if (selfDepend != null) {
            module.name = selfDepend.name;
            module.version = selfDepend.version;
        }
        else {
            module.name = Util.getBaseName(sourceDir.getName());
            module.version = "1.0";
        }
        
        File libDir = new File(libPath);
        File[] list = libDir.listFiles();
        for (File file2 : list) {
            if (!file2.getName().endsWith(".sc")) {
                continue;
            }
            Depend depend = Depend.fromFile(file2);
            module.depends.add(depend);
        }
        return new Compiler(module, sourceDir, libPath, libDir.getParent()+"/output/");
    }
    
    public boolean run() throws IOException {
        for (File file : sources) {
            AstNode.FileUnit funit = parse(file);
            module.fileUnits.add(funit);
        }
        
        if (log.printError()) {
            return false;
        }
        
        typeCheck();
        
        if (log.printError()) {
            return false;
        }

        if (genCode) {
            genOutput();
        }
        return true;
    }
    
    public SModule importModule(String moduleName, String version) {
        String file = libPath+"/"+moduleName + "-" + version +".sc";
        Compiler compiler = Compiler.makeDefault(file, libPath);
        compiler.genCode = false;
        try {
            compiler.run();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Load lib fail:"+file);
        }
        return compiler.module;
    }
    
    private void typeCheck() {
        SlotTypeResolver slotResolver = new SlotTypeResolver(module, log, this);
        slotResolver.run();
        
        if (log.hasError()) {
            return;
        }
        
        ExprTypeResolver exprResolver = new ExprTypeResolver(module, log);
        exprResolver.run();
        
        if (log.hasError()) {
            return;
        }
    }
    
    public AstNode.FileUnit parse(File file) throws IOException {
        String src = Files.readString(file.toPath());
        
        CompilerLog log = new CompilerLog();
        AstNode.FileUnit unit = new AstNode.FileUnit(file.getPath());
        DeepParser parser = new DeepParser(log, src, unit);
        parser.parse();
        return unit;
    }
    
    public void genOutput() throws IOException {
        new File(outputDir).mkdirs();
        
        String outputFile = outputDir + "/" + this.module.name;
        CppGenerator generator = new CppGenerator(outputFile+".h", true);
        module.walk(generator);
        
        CppGenerator generator2 = new CppGenerator(outputFile+".cpp", false);
        module.walk(generator2);
    }
    
}
