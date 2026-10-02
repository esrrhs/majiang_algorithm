cmake_minimum_required(VERSION 3.14)
project(majiang_algorithm_cpp CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

add_library(majiang STATIC
    src/def.cpp
    src/common.cpp
    src/hu_table.cpp
    src/hu_util.cpp
    src/ai_table.cpp
    src/ai_util.cpp
    src/api.cpp
)
target_include_directories(majiang PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)

if(MSVC)
    target_compile_options(majiang PRIVATE /utf-8 /W4)
else()
    # -ffp-contract=off 保证浮点运算与 Java/Go 逐位一致(gen 输出与 parity 评分)
    target_compile_options(majiang PRIVATE -Wall -Wextra -ffp-contract=off)
endif()

add_executable(majiang_tests
    test/main.cpp
    test/def_test.cpp
    test/hu_util_test.cpp
    test/ai_util_test.cpp
    test/gen_test.cpp
    test/parity_test.cpp
)
target_link_libraries(majiang_tests PRIVATE majiang)
target_include_directories(majiang_tests PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/src)

if(NOT MSVC)
    target_compile_options(majiang_tests PRIVATE -ffp-contract=off)
endif()

enable_testing()
# 测试工作目录固定为 cpp/,使 ../data 能找到共享查表文件
add_test(NAME majiang_tests COMMAND majiang_tests
         WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})
