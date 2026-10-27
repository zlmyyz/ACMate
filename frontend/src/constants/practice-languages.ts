export const practiceLanguages = {
  CPP17: { label:'C++17', filename:'Main.cpp', template:'#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    ios::sync_with_stdio(false);\n    cin.tie(nullptr);\n    // 在这里编写代码\n    return 0;\n}\n' },
  C17: { label:'C17', filename:'Main.c', template:'#include <stdio.h>\n\nint main(void) {\n    // 在这里编写代码\n    return 0;\n}\n' },
  JAVA17: { label:'Java17', filename:'Main.java', template:'import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner in = new Scanner(System.in);\n        // 在这里编写代码\n    }\n}\n' },
  PYTHON3: { label:'Python3', filename:'Main.py', template:'import sys\n\n# 在这里编写代码\n' },
  CSHARP: { label:'C# / .NET 10', filename:'Main.cs', template:'using System;\n\npublic class Program\n{\n    public static void Main()\n    {\n        // 在这里编写代码\n    }\n}\n' },
  TYPESCRIPT: { label:'TypeScript 5.9', filename:'Main.ts', template:"import { readFileSync } from 'node:fs';\n\nconst input: string = readFileSync(0, 'utf8');\n// 在这里编写代码\n" },
  JAVASCRIPT: { label:'JavaScript / Node.js 24', filename:'Main.js', template:"const fs = require('node:fs');\nconst input = fs.readFileSync(0, 'utf8');\n// 在这里编写代码\n" },
  GO: { label:'Go', filename:'Main.go', template:'package main\n\nimport (\n    "bufio"\n    "fmt"\n    "os"\n)\n\nfunc main() {\n    in := bufio.NewReader(os.Stdin)\n    out := bufio.NewWriter(os.Stdout)\n    defer out.Flush()\n    // 示例：读取并输出一个整数\n    var n int\n    if _, err := fmt.Fscan(in, &n); err == nil {\n        fmt.Fprintln(out, n)\n    }\n}\n' },
  RUST: { label:'Rust', filename:'Main.rs', template:'use std::io::{self, Read};\n\nfn main() {\n    let mut input = String::new();\n    io::stdin().read_to_string(&mut input).unwrap();\n    // 在这里编写代码\n}\n' },
} as const
export type PracticeLanguage = keyof typeof practiceLanguages
export const practiceKeywords:Record<PracticeLanguage,string[]>={
  CPP17:['auto','bool','break','case','cin','cout','class','const','continue','double','else','for','if','int','long','namespace','return','sort','string','struct','vector','void','while'],
  C17:['break','case','char','const','continue','double','else','for','if','int','long','printf','return','scanf','sizeof','struct','typedef','void','while'],
  JAVA17:['boolean','break','class','continue','double','else','for','if','import','int','long','new','public','return','static','String','System.out.println','void','while'],
  PYTHON3:['and','break','class','continue','def','elif','else','enumerate','False','for','if','import','input','int','len','None','not','or','print','range','return','True','while'],
  CSHARP:['bool','break','class','Console.ReadLine','Console.WriteLine','continue','double','else','for','foreach','if','int','long','new','public','return','static','string','using','var','void','while'],
  TYPESCRIPT:['async','await','boolean','break','const','console.log','continue','else','export','for','function','if','import','interface','let','new','number','return','string','type','while'],
  JAVASCRIPT:['async','await','break','const','console.log','continue','else','export','for','function','if','import','let','new','return','while'],
  GO:['append','bool','break','case','chan','const','continue','defer','else','fmt.Fscan','fmt.Fprintln','for','func','go','if','import','int','interface','len','make','map','package','range','return','string','struct','switch','var'],
  RUST:['break','const','continue','else','enum','false','fn','for','if','impl','let','loop','match','mut','Option','println!','pub','return','Some','String','struct','trait','true','use','Vec','while'],
}
