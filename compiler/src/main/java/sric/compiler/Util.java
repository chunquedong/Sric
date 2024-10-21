//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author yangjiandong
 */
public class Util {
        
    public static String getBaseName(String name) {
        int pos = name.lastIndexOf(".");
        if (pos == -1) {
            return name;
        }
        String basename = name.substring(0, pos);
        return basename;
    }
    
    public static ArrayList<File> listFile(File file) {
        ArrayList<File> list = new ArrayList<File>();
        doListFile(file, list, ".sc");
        return list;
    }
    
    private static void doListFile(File file, ArrayList<File> sources, String extName) {
        if (!file.isDirectory()) {
            if (file.getName().endsWith(extName)) {
                sources.add(file);
            }
        }
        else {
            File[] list = file.listFiles();
            for (File file2 : list) {
                doListFile(file2, sources, extName);
            }
        }
    }
    
    public static HashMap<String, String> readProps(String file) throws IOException {
        HashMap<String, String> map = new HashMap<String, String>();
        List<String> lines = Files.readAllLines(Path.of(file));
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("//")) {
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }
            var fs = line.split("=");
            if (fs.length == 1 && line.endsWith("=")) {
                map.put(fs[0].trim(), "");
                continue;
            }
            if (fs.length != 2) {
                System.out.println("props parse error:"+line);
                continue;
            }
            map.put(fs[0].trim(), fs[1].trim());
        }
        return map;
    }
    
    public static void writeProps(String file, HashMap<String, String> props) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        for (var e : props.entrySet()) {
            sb.append(e.getKey());
            sb.append(" = ");
            sb.append(e.getValue());
            sb.append("\n");
        }
        
        String data = sb.toString();
        Files.writeString(Path.of(file), data, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
