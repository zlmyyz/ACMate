package com.itnoduck.acmate.practice;
import java.util.List;

/** All commands and filenames are server-owned constants. */
public enum PracticeLanguage {
    CPP17("Main.cpp","main",List.of("/usr/bin/g++","Main.cpp","-std=c++17","-O2","-o","main"),List.of("./main")),
    C17("Main.c","main",List.of("/usr/bin/gcc","Main.c","-std=c17","-O2","-lm","-o","main"),List.of("./main")),
    JAVA17("Main.java","main.jar",List.of("/bin/sh","-c","javac -encoding UTF-8 Main.java && jar -J-Djava.io.tmpdir=/w cfe main.jar Main *.class"),List.of("/usr/bin/java","-Xmx128m","-Xss8m","-XX:+UseSerialGC","-jar","main.jar")),
    PYTHON3("Main.py",null,List.of(),List.of("/usr/bin/python3","Main.py")),
    CSHARP("Main.cs","Main.dll",List.of("/usr/local/bin/acmate-csc"),List.of("/usr/share/dotnet/dotnet","Main.dll")),
    TYPESCRIPT("Main.ts","Main.js",List.of("/usr/local/bin/node","--max-old-space-size=256","/usr/local/lib/node_modules/typescript/bin/tsc","Main.ts","--target","ES2022","--module","commonjs","--strict","--types","node","--typeRoots","/usr/local/lib/node_modules/@types","--skipLibCheck","--noEmitOnError"),List.of("/usr/local/bin/node","--max-old-space-size=128","Main.js")),
    JAVASCRIPT("Main.js",null,List.of(),List.of("/usr/local/bin/node","--max-old-space-size=128","Main.js")),
    GO("Main.go","main",List.of("/usr/local/go/bin/go","build","-p","1","-ldflags=-s -w","-o","main","Main.go"),List.of("./main")),
    RUST("Main.rs","main",List.of("/usr/bin/rustc","Main.rs","--edition=2021","-C","opt-level=1","-C","strip=symbols","-o","main"),List.of("./main"));
    final String filename,artifact;
    final List<String> compile,run;
    PracticeLanguage(String filename,String artifact,List<String> compile,List<String> run){this.filename=filename;this.artifact=artifact;this.compile=compile;this.run=run;}
}
