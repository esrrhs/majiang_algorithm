cmake_minimum_required(VERSION 3.16)
project(majiangserver_fakelua LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(FAKELUA_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../fakelua" CACHE PATH "FakeLua source root")
set(FAKELUA_BUILD "${FAKELUA_ROOT}/build" CACHE PATH "FakeLua build dir")
set(MAJIANG_CPP_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../cpp" CACHE PATH "C++ algorithm library root")

add_subdirectory("${MAJIANG_CPP_ROOT}" majiang_cpp_build EXCLUDE_FROM_ALL)

add_executable(majiangserver_fakelua
  main.cpp
  native_majiang.cpp
)
target_include_directories(majiangserver_fakelua PRIVATE
  "${FAKELUA_ROOT}/include"
  "${MAJIANG_CPP_ROOT}/include"
)

find_library(FAKELUA_LIB fakelua HINTS "${FAKELUA_BUILD}/lib64" "${FAKELUA_BUILD}/lib" REQUIRED)

if(APPLE)
  target_link_options(majiangserver_fakelua PRIVATE "-Wl,-export_dynamic")
elseif(NOT WIN32)
  target_link_options(majiangserver_fakelua PRIVATE "-Wl,-E")
endif()

target_link_libraries(majiangserver_fakelua PRIVATE
  "${FAKELUA_LIB}"
  majiang
  pthread
  dl
)

# 与 cpp 库一致,保证 AI 评分跨语言对齐
if(NOT MSVC)
  target_compile_options(majiangserver_fakelua PRIVATE -ffp-contract=off)
endif()

set_target_properties(majiangserver_fakelua PROPERTIES
  BUILD_RPATH "${FAKELUA_BUILD}/lib64;${FAKELUA_BUILD}/lib"
  INSTALL_RPATH "${FAKELUA_BUILD}/lib64;${FAKELUA_BUILD}/lib"
)
