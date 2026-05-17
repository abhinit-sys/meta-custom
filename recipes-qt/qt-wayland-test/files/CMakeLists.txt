cmake_minimum_required(VERSION 3.16)
project(qt_wayland_test LANGUAGES CXX)

set(CMAKE_AUTOMOC ON)

find_package(Qt5 REQUIRED COMPONENTS Core Gui Widgets)

add_executable(qt-wayland-test
    qt-wayland-test.cpp
)

target_link_libraries(qt-wayland-test
    Qt5::Core
    Qt5::Gui
    Qt5::Widgets
)
