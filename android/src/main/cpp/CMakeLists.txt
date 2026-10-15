cmake_minimum_required(VERSION 3.10.2)

project(jitsie2eeframecryptor CXX)

add_library(jitsie2eeframecryptor SHARED JitsiE2EEFrameCryptor.cpp)

set_target_properties(jitsie2eeframecryptor PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON)

find_library(log-lib log)

target_link_libraries(jitsie2eeframecryptor ${log-lib})
