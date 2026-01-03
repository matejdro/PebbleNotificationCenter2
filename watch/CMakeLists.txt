cmake_minimum_required(VERSION 4.1)
project(watch C)

set(CMAKE_C_STANDARD 11)

include_directories(src)

file(GLOB SOURCES "src/*.c")

add_executable(watch ${SOURCES})

target_include_directories(watch PUBLIC ~/.pebble-sdk/SDKs/current/sdk-core/pebble/basalt/include)
target_precompile_headers(watch PUBLIC build/basalt/src/resource_ids.auto.h)
