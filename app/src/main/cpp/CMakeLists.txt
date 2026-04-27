cmake_minimum_required(VERSION 3.22.1)

project(Winlator)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -Wno-unused-function -Wimplicit-function-declaration")

add_subdirectory(patchelf)
add_subdirectory(adrenotools)
set(LSFGVK_ANDROID_WINE ON CACHE BOOL "Build lsfg-vk for Winlator" FORCE)
if(ANDROID)
        set(VOLK_STATIC_DEFINES VK_USE_PLATFORM_ANDROID_KHR CACHE STRING "Vulkan platform defines for volk" FORCE)
        add_compile_definitions(VK_USE_PLATFORM_ANDROID_KHR)
endif()
add_subdirectory(lsfg)
if(TARGET lsfg-vk)
        set(LSFGVK_MAIN_SOURCE "${CMAKE_CURRENT_SOURCE_DIR}/lsfg/src/main.cpp")
        set(LSFGVK_PATCHED_MAIN_DIR "${CMAKE_CURRENT_BINARY_DIR}/lsfg-winlator")
        set(LSFGVK_PATCHED_MAIN "${LSFGVK_PATCHED_MAIN_DIR}/main.cpp")
        file(MAKE_DIRECTORY "${LSFGVK_PATCHED_MAIN_DIR}")
        file(READ "${LSFGVK_MAIN_SOURCE}" LSFGVK_MAIN_CONTENT)
        string(REPLACE
                "std::ofstream latest(\"/tmp/lsfg-vk_last\", std::ios::trunc);"
                "const char* latestPathEnv = std::getenv(\"LSFG_LAST_PATH\");\n            const std::string latestPath = latestPathEnv && *latestPathEnv != '\\0' ? std::string(latestPathEnv) : std::string(\"/tmp/lsfg-vk_last\");\n            std::ofstream latest(latestPath, std::ios::trunc);"
                LSFGVK_MAIN_CONTENT "${LSFGVK_MAIN_CONTENT}")
        string(REPLACE
                "throw std::runtime_error(\"Failed to open /tmp/lsfg-vk_last for writing\");"
                "throw std::runtime_error(\"Failed to open \" + latestPath + \" for writing\");"
                LSFGVK_MAIN_CONTENT "${LSFGVK_MAIN_CONTENT}")
        string(REPLACE
                "throw std::runtime_error(\"Failed to write to /tmp/lsfg-vk_last\");"
                "throw std::runtime_error(\"Failed to write to \" + latestPath);"
                LSFGVK_MAIN_CONTENT "${LSFGVK_MAIN_CONTENT}")
        file(WRITE "${LSFGVK_PATCHED_MAIN}" "${LSFGVK_MAIN_CONTENT}")

        get_target_property(LSFGVK_SOURCES lsfg-vk SOURCES)
        set(LSFGVK_FILTERED_SOURCES)
        foreach(LSFGVK_SOURCE IN LISTS LSFGVK_SOURCES)
                if(IS_ABSOLUTE "${LSFGVK_SOURCE}")
                        set(LSFGVK_SOURCE_ABS "${LSFGVK_SOURCE}")
                else()
                        get_filename_component(LSFGVK_SOURCE_ABS "${LSFGVK_SOURCE}" ABSOLUTE BASE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/lsfg")
                endif()
                if(NOT LSFGVK_SOURCE_ABS STREQUAL LSFGVK_MAIN_SOURCE)
                        list(APPEND LSFGVK_FILTERED_SOURCES "${LSFGVK_SOURCE}")
                endif()
        endforeach()
        set_property(TARGET lsfg-vk PROPERTY SOURCES ${LSFGVK_FILTERED_SOURCES})
        target_sources(lsfg-vk PRIVATE "${LSFGVK_PATCHED_MAIN}")
endif()

# Add Winlator shared library
add_library(winlator SHARED
        winlator/drawable.c
        winlator/gpu_image.c
        winlator/sysvshared_memory.c
        winlator/xconnector_epoll.c
        winlator/alsa_client.c
        winlator/process_lifecycle.c
        winlator/vulkan.c
        xz/native_xz_stream.c
        xz/xz_embedded/xz_crc32.c
        xz/xz_embedded/xz_crc64.c
        xz/xz_embedded/xz_dec_bcj.c
        xz/xz_embedded/xz_dec_lzma2.c
        xz/xz_embedded/xz_dec_stream.c
)

target_compile_options(winlator PRIVATE -Wall -Wextra)
target_compile_definitions(winlator PRIVATE
        XZ_USE_CRC64
        XZ_DEC_CONCATENATED
        XZ_DEC_X86
        XZ_DEC_ARM
        XZ_DEC_ARMTHUMB
        XZ_DEC_ARM64
        XZ_DEC_RISCV
        XZ_DEC_POWERPC
        XZ_DEC_IA64
        XZ_DEC_SPARC
)
target_include_directories(winlator PRIVATE
        xz/xz_embedded
)

target_link_libraries(winlator
        log
        android
        jnigraphics
        aaudio
        EGL
        GLESv2
        GLESv3
        adrenotools
)

# Fake evdev input shim used by the pb_controller_fix controller path.
add_library(fakeinput SHARED
        winlator/fakeinput.cpp
)
target_compile_options(fakeinput PRIVATE -Wall -Wextra -fvisibility=hidden)
target_link_libraries(fakeinput log dl)

# Evshim - controller support shim (loaded via LD_PRELOAD, uses dlopen for SDL2)
add_library(evshim SHARED
        winlator/evshim.c
)
target_compile_options(evshim PRIVATE -Wall -Wextra -fvisibility=hidden)
target_link_libraries(evshim log dl)
