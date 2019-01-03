#!/bin/bash

zeebeVersion='develop'
protoFile=schema.proto
protoFilePath=../proto/
genPath=Connector/Zeebe/Hazelcast/Connector

protoc=./packages/Google.Protobuf.Tools.3.6.1/tools/linux_x64/protoc

# restore packages
echo -e "nuget restore Connector.sln\n"
nuget restore Connector.sln

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
