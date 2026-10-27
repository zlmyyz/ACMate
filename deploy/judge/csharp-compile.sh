#!/bin/sh
set -eu
for compiler in /usr/share/dotnet/sdk/*/Roslyn/bincore/csc.dll; do
    exec /usr/share/dotnet/dotnet "$compiler" @/usr/share/acmate/csharp.rsp Main.cs
done
echo 'C# compiler is unavailable' >&2
exit 1
