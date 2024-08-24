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
import sc2.compiler.backend.ScLibGenerator;
import sc2.compiler.parser.DeepParser;
import sc2.compiler.resolve.ErrorChecker;
import sc2.compiler.resolve.ExprTypeResolver;
import sc2.compiler.resolve.TopLevelTypeResolver;

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
    
    private static ArrayList<Depend> listDepends(File libDir) {
        ArrayList<Depend> depends = new ArrayList<Depend>();
        File[] list = libDir.listFiles();
        for (File file2 : list) {
            if (!file2.getName().endsWith(".meta")) {
                continue;
            }
            Depend depend = new Depend();
            depend.name = Util.getBaseName(file2.getName());
            depend.version = "1.0";
            depends.add(depend);
        }
        return depends;
    }
    
    public static Compiler makeDefault(String sourcePath, String libPath) {
        File sourceDir = new File(sourcePath);
        
        SModule module = new SModule();
        module.name = Util.getBaseName(sourceDir.getName());
        module.version = "1.0";
        
        File libDir = new File(libPath);
        module.depends= listDepends(libDir);
        return new Compiler(module, sourceDir, libPath, libDir.getParent()+"/output/");
    }
    
    public static Compiler fromProps(String propsPath, String libPath) throws IOException {
        return fromProps(propsPath, libPath, null);
    }
    
    public static Compiler fromProps(String propsPath, String libPath, String srcDirs) throws IOException {
        var props = Util.readProps(propsPath);
        SModule module = SModule.fromProps(props);
        if (srcDirs == null) {
            srcDirs = props.get("srcDirs");
            if (srcDirs == null) {
                throw new RuntimeException("Unknow srcDirs");
            }
            else {
                srcDirs = new File(propsPath).getParent() + "/" + srcDirs;
            }
        }
        File sourceDir = new File(srcDirs);
        File libDir = new File(libPath);
        return new Compiler(module, sourceDir, libPath, libDir.getParent()+"/output/");
    }
    
    public boolean run() throws IOException {
        for (File file : sources) {
            AstNode.FileUnit funit = parse(file);
            funit.module = module;
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
        String libFile = libPath + "/" + moduleName;
        try {
            Compiler compiler = Compiler.fromProps(libFile+".meta", libPath, libFile+".sc");
            compiler.genCode = false;
        
            compiler.run();
            return compiler.module;
            
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Load lib fail:"+libFile+".meta");
        }
    }
    
    private void typeCheck() {
        TopLevelTypeResolver slotResolver = new TopLevelTypeResolver(log, module, this);
        slotResolver.run();
        
        if (log.hasError()) {
            return;
        }
        
        ExprTypeResolver exprResolver = new ExprTypeResolver(log, module);
        exprResolver.run();
        
        ErrorChecker errorChecker = new ErrorChecker(log, module);
        errorChecker.run();
        
        if (log.hasError()) {
            return;
        }
    }
    
    public AstNode.FileUnit parse(File file) throws IOException {
        String src = Files.readString(file.toPath());
        
        AstNode.FileUnit unit = new AstNode.FileUnit(file.getPath());
        DeepParser parser = new DeepParser(log, src, unit);
        parser.parse();
        return unit;
    }
    
    public void genOutput() throws IOException {
        String libFile = libPath + "/" + this.module.name;
        ScLibGenerator scGenerator = new ScLibGenerator(log, libFile + ".sc");
        scGenerator.run(module);
        
        var props = this.module.toMetaProps();
        Util.writeProps(libFile+".meta", props);
        
        new File(outputDir).mkdirs();
        
        String outputFile = outputDir + "/" + this.module.name;
        CppGenerator generator = new CppGenerator(log, outputFile+".h", true);
        generator.run(module);
        
        CppGenerator generator2 = new CppGenerator(log, outputFile+".cpp", false);
        generator2.run(module);

    }
    
}
