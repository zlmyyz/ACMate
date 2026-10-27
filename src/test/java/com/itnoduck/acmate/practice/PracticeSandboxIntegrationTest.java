package com.itnoduck.acmate.practice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfSystemProperty(named="practice.sandbox-check",matches="true")
class PracticeSandboxIntegrationTest {
 final PracticeRunner runner=new PracticeRunner(true,"http://127.0.0.1:5050","./deploy/judge/.judge-token",new ObjectMapper());
 @Test void runsCppJavaAndPythonInSandbox(){
  assertTrue(runner.available());
  var cpp=runner.run("CPP17","#include <iostream>\nint main(){int a,b;std::cin>>a>>b;std::cout<<a+b;}\n","1 2\n");assertEquals("FINISHED",cpp.status(),cpp.stderr());assertEquals("3",cpp.stdout().strip());
  var java=runner.run("JAVA17","import java.util.*;public class Main{public static void main(String[] args){Scanner s=new Scanner(System.in);System.out.println(s.nextInt()+s.nextInt());}}","1 2\n");assertEquals("FINISHED",java.status(),java.stderr());assertEquals("3",java.stdout().strip());
  var python=runner.run("PYTHON3","a,b=map(int,input().split());print(a+b)","1 2\n");assertEquals("FINISHED",python.status(),python.stderr());assertEquals("3",python.stdout().strip());
 }
 @Test void reportsCompilationErrorsAndEnforcesTimeAndOutputLimits(){
  assertEquals("COMPILE_ERROR",runner.run("CPP17","this is not valid C++","").status());
  assertEquals("Time Limit Exceeded",runner.run("PYTHON3","while True: pass","").status());
  assertEquals("Output Limit Exceeded",runner.run("PYTHON3","while True: print('x'*1000)","").status());
 }
 @Test void sandboxCannotReadApplicationFiles(){
  var result=runner.run("PYTHON3","import os; print(os.path.exists('/app/src/main/resources/application-local.yml')); print(os.path.exists('/opt/mount.yaml'))","");
  assertEquals("FINISHED",result.status());assertEquals("False\nFalse",result.stdout().strip());
 }
 @Test void runsAdditionalLanguagesWithCustomInput(){
  var cases=java.util.Map.of(
   "C17","#include <stdio.h>\nint main(){int a,b;scanf(\"%d%d\",&a,&b);printf(\"%d\",a+b);}",
   "CSHARP","using System;class Program{static void Main(){var a=Console.ReadLine().Split();Console.WriteLine(int.Parse(a[0])+int.Parse(a[1]));}}",
   "TYPESCRIPT","import {readFileSync} from 'node:fs';const a:number[]=readFileSync(0,'utf8').trim().split(/\\s+/).map(Number);console.log(a[0]+a[1]);",
   "JAVASCRIPT","const fs=require('fs');const a=fs.readFileSync(0,'utf8').trim().split(/\\s+/).map(Number);console.log(a[0]+a[1]);",
   "GO","package main\nimport(\"fmt\";\"os\";\"bufio\")\nfunc main(){in:=bufio.NewReader(os.Stdin);var a,b int;fmt.Fscan(in,&a,&b);fmt.Println(a+b)}",
   "RUST","use std::io::{self,Read};fn main(){let mut s=String::new();io::stdin().read_to_string(&mut s).unwrap();let a:Vec<i64>=s.split_whitespace().map(|x|x.parse().unwrap()).collect();println!(\"{}\",a[0]+a[1]);}"
  );
  for(var entry:cases.entrySet()){
   var result=runner.run(entry.getKey(),entry.getValue(),"5 7\n");
   assertEquals("FINISHED",result.status(),entry.getKey()+": "+result.stderr());assertEquals("12",result.stdout().strip(),entry.getKey());
  }
 }
}
