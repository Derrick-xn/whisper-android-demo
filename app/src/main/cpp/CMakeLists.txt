cmake_minimum_required(VERSION 3.22.1)

project("whisperdemo")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add compiler flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -DNDEBUG -fPIC")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -DNDEBUG -fPIC")

# Disable unnecessary whisper features to avoid build issues
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)

# Clone and build whisper.cpp
include(FetchContent)
FetchContent_Declare(
    whisper
    GIT_REPOSITORY https://github.com/ggerganov/whisper.cpp.git
    GIT_TAG        master
    GIT_SHALLOW    TRUE
    GIT_PROGRESS   TRUE
)

# Set whisper build options before making available
set(WHISPER_NO_AVX ON CACHE BOOL "" FORCE)
set(WHISPER_NO_AVX2 ON CACHE BOOL "" FORCE) 
set(WHISPER_NO_FMA ON CACHE BOOL "" FORCE)
set(WHISPER_NO_F16C ON CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(whisper)

# Add our native library
add_library(whisperdemo SHARED
    native-lib.cpp
    audio_utils.cpp
)

# Find required libraries
find_library(log-lib log)

# Include whisper headers - handle different directory structures
target_include_directories(whisperdemo PRIVATE 
    ${whisper_SOURCE_DIR}/include
    ${whisper_SOURCE_DIR}
)

# Try to include ggml headers if they exist
if(EXISTS "${whisper_SOURCE_DIR}/ggml/include")
    target_include_directories(whisperdemo PRIVATE ${whisper_SOURCE_DIR}/ggml/include)
endif()

if(EXISTS "${whisper_SOURCE_DIR}/ggml")
    target_include_directories(whisperdemo PRIVATE ${whisper_SOURCE_DIR}/ggml)
endif()

# Link libraries
target_link_libraries(whisperdemo
    whisper
    ${log-lib}
    android
) 