FROM criyle/go-judge:v1.13.3@sha256:97bec66d054e4d844e0ad29dfdd5c79ef3d09efba25b06dd58643a6e99918dcd AS sandbox
FROM node:24-bookworm-slim AS node
RUN npm install --global typescript@5.9.3 @types/node@24.13.3
FROM golang:1.26-bookworm AS golang
ENV GOCACHE=/usr/local/go/acmate-cache GOMAXPROCS=2 CGO_ENABLED=0
RUN go install -p 2 std
FROM debian:bookworm-slim AS dotnet
COPY .sdk-cache/dotnet-sdk-10.0.403-linux-x64.tar.gz /tmp/dotnet-sdk.tar.gz
RUN echo 'b7b3d8461cf98b07d2552ce9199702fa3ad010ac2671a5f8cd5136f7534e50f6855ba7bbb02b11494563f1d04156276a31cda1103b900e5eecd0d9473dc7b9c5  /tmp/dotnet-sdk.tar.gz' | sha512sum -c - && mkdir -p /usr/share/dotnet && tar -xzf /tmp/dotnet-sdk.tar.gz -C /usr/share/dotnet && rm /tmp/dotnet-sdk.tar.gz
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends g++ python3 openjdk-17-jdk-headless rustc libicu72 libssl3 zlib1g ca-certificates && rm -rf /var/lib/apt/lists/*
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
COPY --from=golang /usr/local/go /usr/local/go
COPY --from=dotnet /usr/share/dotnet /usr/share/dotnet
COPY csharp-compile.sh /usr/local/bin/acmate-csc
RUN sed -i 's/\r$//' /usr/local/bin/acmate-csc && chmod 755 /usr/local/bin/acmate-csc && mkdir -p /usr/share/acmate && \
    printf '%s\n' '-nologo' '-target:exe' '-out:Main.dll' '-langversion:latest' '-nostdlib+' > /usr/share/acmate/csharp.rsp && \
    for ref in /usr/share/dotnet/packs/Microsoft.NETCore.App.Ref/*/ref/net10.0/*.dll; do printf -- '-r:%s\n' "$ref" >> /usr/share/acmate/csharp.rsp; done
WORKDIR /opt
COPY --from=sandbox /opt/go-judge /opt/mount.yaml /opt/
RUN sed -i 's/java-21-openjdk/java-17-openjdk/g' /opt/mount.yaml
COPY LICENSE-go-judge.txt /usr/share/doc/go-judge/LICENSE
ENTRYPOINT ["/opt/go-judge"]


