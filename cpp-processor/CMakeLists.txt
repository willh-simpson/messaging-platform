cmake_minimum_required(VERSION 3.20)
project(CppMessageProcessor VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

include(FetchContent)

FetchContent_Declare(
    nlohmann_json
    GIT_REPOSITORY https://github.com/nlohmann/json.git
    GIT_TAG        v3.11.3
    GIT_SHALLOW    TRUE
)
FetchContent_MakeAvailable(nlohmann_json)

find_package(RdKafka CONFIG REQUIRED)

find_package(protobuf CONFIG REQUIRED)

find_package(gRPC CONFIG REQUIRED)

get_target_property(PROTOC_EXE       protobuf::protoc    LOCATION)
get_target_property(GRPC_CPP_PLUGIN  gRPC::grpc_cpp_plugin LOCATION)

set(PROTO_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../grpc-stubs/src/main/proto")
set(PROTO_FILE "${PROTO_DIR}/message_processor.proto")
 
set(PROTO_GENERATED_DIR "${CMAKE_CURRENT_BINARY_DIR}/generated")
file(MAKE_DIRECTORY ${PROTO_GENERATED_DIR})

add_custom_command(
    OUTPUT
        "${PROTO_GENERATED_DIR}/message_processor.pb.cc"
        "${PROTO_GENERATED_DIR}/message_processor.pb.h"
    COMMAND "${PROTOC_EXE}"
        --cpp_out=${PROTO_GENERATED_DIR}
        --proto_path=${PROTO_DIR}
        "${PROTO_FILE}"
    DEPENDS "${PROTO_FILE}"
    COMMENT "Generating protobuf C++ sources"
)

add_custom_command(
    OUTPUT
        "${PROTO_GENERATED_DIR}/message_processor.grpc.pb.cc"
        "${PROTO_GENERATED_DIR}/message_processor.grpc.pb.h"
    COMMAND "${PROTOC_EXE}"
        --grpc_out=${PROTO_GENERATED_DIR}
        --plugin=protoc-gen-grpc="${GRPC_CPP_PLUGIN}"
        --proto_path=${PROTO_DIR}
        "${PROTO_FILE}"
    DEPENDS "${PROTO_FILE}"
    COMMENT "Generating gRPC C++ stubs"
)

add_executable(cpp-processor
    src/main.cpp
    src/kafka/KafkaConsumer.cpp
    src/batch/MessageBatcher.cpp
    src/grpc/ProcessorServiceImpl.cpp
    ${PROTO_GENERATED_DIR}/message_processor.pb.cc
    ${PROTO_GENERATED_DIR}/message_processor.grpc.pb.cc
)

target_include_directories(cpp-processor PRIVATE
    src/
    ${PROTO_GENERATED_DIR}
)

target_link_libraries(cpp-processor PRIVATE
    nlohmann_json::nlohmann_json
    RdKafka::rdkafka++
    protobuf::libprotobuf
    gRPC::grpc++
    gRPC::grpc++_reflection
)

target_compile_options(cpp-processor PRIVATE
    # MSVC flags
    $<$<CXX_COMPILER_ID:MSVC>:/W4 /WX- /utf-8>
    $<$<AND:$<CXX_COMPILER_ID:MSVC>,$<CONFIG:Debug>>:/Od /Zi>
    $<$<AND:$<CXX_COMPILER_ID:MSVC>,$<CONFIG:Release>>:/O2 /DNDEBUG>
 
    # GCC / Clang flags (Linux, macOS)
    $<$<NOT:$<CXX_COMPILER_ID:MSVC>>:-Wall -Wextra -Wpedantic>
    $<$<AND:$<NOT:$<CXX_COMPILER_ID:MSVC>>,$<CONFIG:Release>>:-O3 -DNDEBUG>
    $<$<AND:$<NOT:$<CXX_COMPILER_ID:MSVC>>,$<CONFIG:Debug>>:-g>
)

if(MSVC)
    target_compile_options(cpp-processor PRIVATE /external:anglebrackets /external:W0)
    # Suppress MSVC warning about std::getenv being "unsafe" (C4996).
    # getenv is standard C — MSVC's replacement (_dupenv_s) is non-portable.
    target_compile_definitions(cpp-processor PRIVATE _CRT_SECURE_NO_WARNINGS)
endif()
 
install(TARGETS cpp-processor DESTINATION bin)