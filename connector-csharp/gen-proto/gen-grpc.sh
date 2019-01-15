#!/bin/bash

schemaVersion='master'
protoFile=schema.proto
protoFilePath=./
genPath=Connector/Zeebe/Hazelcast/Connector

protoc=./packages/Google.Protobuf.Tools.3.6.1/tools/linux_x64/protoc

# restore packages
echo -e "nuget restore Connector.sln\n"
nuget restore Connector.sln

# get schame proto file
echo -e "wget https://raw.githubusercontent.com/zeebe-io/zeebe-exporter-protobuf/${schemaVersion}/src/main/proto/${protoFile}\n"
wget https://raw.githubusercontent.com/zeebe-io/zeebe-exporter-protobuf/${schemaVersion}/src/main/proto/${protoFile}

# generate protobuf
echo "
 ${protoc} \
  -I/usr/include/ \
  -I${protoFilePath} \
  --csharp_out ${genPath} \
  ${protoFilePath}/${protoFile} "
echo -e "\n"

 ${protoc} \
  -I/usr/include/ \
  -I${protoFilePath} \
  --csharp_out ${genPath} \
  ${protoFilePath}/${protoFile} 
