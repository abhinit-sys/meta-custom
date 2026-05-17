DESCRIPTION = "BeagleBone Black Development Image -- Qt5 + Bluetooth + Embedded Course"
LICENSE = "MIT"
inherit core-image

#################################################
# BBB IMAGE WITH QT5 (5.15.13)
#
# PREFERRED_VERSION_qtbase = "5.%" in local.conf
# forces BitBake to pick Qt5 for all qt* recipes.
#
# BBB has 512MB RAM -- Qt5 is lightweight and perfect.
# Qt6 needs more RAM + better GPU which BBB lacks.
#
# Build Qt5 apps:
#   qmake myapp.pro && make
#
# Run Qt5 apps:
#   QT_QPA_PLATFORM=linuxfb ./myapp       (on HDMI)
#   QT_QPA_PLATFORM=vnc QT_QPA_VNC_PORT=5900 ./myapp &
#
# BBB differences from RPi5:
#   - Serial port = ttyS0 (not ttyAMA0)
#   - Ethernet IP = 192.168.0.101 (RPi5 = .100)
#   - USB gadget SSH = 192.168.7.2
#   - No onboard WiFi/BT (use USB dongle)
#   - Has ADC (7 channels) -- RPi5 does NOT
#   - Hold S2 button to boot from SD card
#################################################

#################################################
# IMAGE FEATURES
#################################################
IMAGE_FEATURES += " \
    debug-tweaks \
    package-management \
    serial-autologin-root \
"

#################################################
# INPUT / TOUCH SUPPORT
#################################################
IMAGE_INSTALL += " \
    libinput \
    evtest \
"

#################################################
# BASE SYSTEM + DEBUGGING
#################################################
IMAGE_INSTALL += " \
    openssh \
    gdb \
    strace \
    ltrace \
    procps \
    util-linux \
    iproute2 \
    tcpdump \
    ethtool \
    vim \
    nano \
    bash \
    grep \
    sed \
    gawk \
    less \
    findutils \
    coreutils \
    busybox \
"

#################################################
# SYSTEMD + DBUS
#################################################
IMAGE_INSTALL += " \
    systemd \
    systemd-analyze \
    dbus \
    dbus-tools \
"

#################################################
# BLUETOOTH (for USB BT dongles -- BBB has no onboard BT)
#################################################
IMAGE_INSTALL += " \
    bluez5 \
    bluez5-obex \
    bluez-tools \
    bluez5-testtools \
"

#################################################
# QT5 RUNTIME (5.15.13)
# Same packages as the original RPi5 Qt5 image
# so all your Qt5 programs run on both boards.
#################################################
IMAGE_INSTALL += " \
    qtbase \
    qtbase-plugins \
    qtdeclarative \
    qtdeclarative-qmlplugins \
    qtconnectivity \
    qtmultimedia \
    qtsvg \
    qtgraphicaleffects \
    qtquickcontrols2 \
    qtquickcontrols2-qmlplugins \
    qtconnectivity-qmlplugins \
    qtquickcontrols \
"

#################################################
# QT5 DEV TOOLS
#################################################
IMAGE_INSTALL += " \
    qtbase-dev \
    qtdeclarative-dev \
    qttools \
    qttools-dev \
    qtquickcontrols2-dev \
"

#################################################
# WAYLAND + WESTON DISPLAY SERVER
# For micro-HDMI display output
#################################################
IMAGE_INSTALL += " \
    wayland \
    wayland-utils \
    weston \
    weston-init \
    seatd \
    libpam \
    libdrm \
    libdrm-tests \
    kmscube \
"

#################################################
# FONTS
#################################################
IMAGE_INSTALL += " \
    ttf-dejavu-sans \
    ttf-dejavu-sans-mono \
    fontconfig \
    fontconfig-utils \
"

#################################################
# BOOST
#################################################
IMAGE_INSTALL += " \
    boost \
    boost-thread \
    boost-system \
    boost-regex \
    boost-atomic \
    boost-filesystem \
"

#################################################
# PYTHON + DEBUG
#################################################
IMAGE_INSTALL += " \
    python3 \
    python3-dbus \
    python3-pip \
    python3-smbus2 \
    python3-dev \
"

#################################################
# EMBEDDED COURSE -- GPIO / I2C / SPI / CAN
#################################################
IMAGE_INSTALL += " \
    libgpiod \
    libgpiod-dev \
    libgpiod-tools \
    i2c-tools \
    i2c-tools-misc \
    can-utils \
"

#################################################
# C/C++ DEVELOPMENT TOOLS
# BBB has 512MB RAM -- large compiles may need swap.
# Consider cross-compiling on laptop for big projects.
#################################################
IMAGE_INSTALL += " \
    gcc \
    g++ \
    make \
    cmake \
    binutils \
    glibc-dev \
    libstdc++-dev \
"

#################################################
# KERNEL DEVELOPMENT
#################################################
IMAGE_INSTALL += " \
    kernel-dev \
    kernel-devsrc \
    kernel-modules \
"

#################################################
# TESTING TOOLS
#################################################
IMAGE_INSTALL += " \
    valgrind \
    usbutils \
"

#################################################
# ROOTFS POST-PROCESS COMMANDS
#################################################

# Serial console on ttyS0 (BBB UART0)
enable_serial_getty() {
    install -d ${IMAGE_ROOTFS}/etc/systemd/system/getty.target.wants
    ln -sf /usr/lib/systemd/system/serial-getty@.service \
        ${IMAGE_ROOTFS}/etc/systemd/system/getty.target.wants/serial-getty@ttyS0.service
}
ROOTFS_POSTPROCESS_COMMAND += "enable_serial_getty; "

# Ethernet static IP: 192.168.0.101
# Different from RPi5 (192.168.0.100) so both can be on network
create_network_config() {
    install -d ${IMAGE_ROOTFS}/etc/systemd/network
    echo '[Match]'                       >  ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
    echo 'Name=e*'                       >> ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
    echo ''                              >> ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
    echo '[Network]'                     >> ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
    echo 'Address=192.168.0.101/24'      >> ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
    echo 'Gateway=192.168.0.1'           >> ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
    echo 'DNS=192.168.0.1'               >> ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
}
ROOTFS_POSTPROCESS_COMMAND += "create_network_config; "

# USB gadget network (for SSH over USB Client cable)
# Connect mini-USB to laptop, SSH to 192.168.7.2
create_usb_network_config() {
    install -d ${IMAGE_ROOTFS}/etc/systemd/network
    echo '[Match]'                       >  ${IMAGE_ROOTFS}/etc/systemd/network/20-usb.network
    echo 'Name=usb*'                     >> ${IMAGE_ROOTFS}/etc/systemd/network/20-usb.network
    echo ''                              >> ${IMAGE_ROOTFS}/etc/systemd/network/20-usb.network
    echo '[Network]'                     >> ${IMAGE_ROOTFS}/etc/systemd/network/20-usb.network
    echo 'Address=192.168.7.2/24'        >> ${IMAGE_ROOTFS}/etc/systemd/network/20-usb.network
}
ROOTFS_POSTPROCESS_COMMAND += "create_usb_network_config; "

# Disable SysV init conflicts
disable_sysvinit_net() {
    if [ -f ${IMAGE_ROOTFS}/etc/init.d/networking ]; then
        chmod -x ${IMAGE_ROOTFS}/etc/init.d/networking
    fi
}
ROOTFS_POSTPROCESS_COMMAND += "disable_sysvinit_net; "

disable_sysv_sshd() {
    if [ -f ${IMAGE_ROOTFS}/etc/init.d/sshd ]; then
        chmod -x ${IMAGE_ROOTFS}/etc/init.d/sshd
    fi
}
ROOTFS_POSTPROCESS_COMMAND += "disable_sysv_sshd; "

# SSH via direct daemon (same reliable fix as RPi5)
enable_sshd_direct() {
    install -d ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants
    echo '[Unit]'                                          >  ${IMAGE_ROOTFS}/usr/lib/systemd/system/sshd-direct.service
    echo 'Description=OpenSSH Server'                      >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/sshd-direct.service
    echo 'After=network.target sshdgenkeys.service'        >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/sshd-direct.service
    echo 'Wants=sshdgenkeys.service'                       >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/sshd-direct.service
    echo ''                                                >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/sshd-direct.service
    echo '[Service]'                                       >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/sshd-direct.service
    echo 'ExecStartPre=/bin/mkdir -p /var/run/sshd'        >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/sshd-direct.service
    echo 'ExecStart=/usr/sbin/sshd -D'                     >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/sshd-direct.service
    echo 'Restart=always'                                  >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/sshd-direct.service
    echo ''                                                >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/sshd-direct.service
    echo '[Install]'                                       >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/sshd-direct.service
    echo 'WantedBy=multi-user.target'                      >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/sshd-direct.service
    ln -sf /usr/lib/systemd/system/sshd-direct.service \
        ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants/sshd-direct.service
    ln -sf /usr/lib/systemd/system/sshdgenkeys.service \
        ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants/sshdgenkeys.service
}
ROOTFS_POSTPROCESS_COMMAND += "enable_sshd_direct; "

# seatd for display access
enable_seatd() {
    echo '[Unit]'                                          >  ${IMAGE_ROOTFS}/usr/lib/systemd/system/seatd.service
    echo 'Description=Seat management daemon'              >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/seatd.service
    echo ''                                                >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/seatd.service
    echo '[Service]'                                       >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/seatd.service
    echo 'Type=simple'                                     >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/seatd.service
    echo 'ExecStart=/usr/bin/seatd -g video'               >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/seatd.service
    echo 'Restart=always'                                  >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/seatd.service
    echo ''                                                >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/seatd.service
    echo '[Install]'                                       >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/seatd.service
    echo 'WantedBy=multi-user.target'                      >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/seatd.service
    install -d ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants
    ln -sf /usr/lib/systemd/system/seatd.service \
        ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants/seatd.service
}
ROOTFS_POSTPROCESS_COMMAND += "enable_seatd; "

# Weston display server
enable_weston() {
    install -d ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants
    if [ -f ${IMAGE_ROOTFS}/usr/lib/systemd/system/weston.service ]; then
        ln -sf /usr/lib/systemd/system/weston.service \
            ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants/weston.service
    fi
    install -d ${IMAGE_ROOTFS}/etc/systemd/system/sockets.target.wants
    if [ -f ${IMAGE_ROOTFS}/usr/lib/systemd/system/weston.socket ]; then
        ln -sf /usr/lib/systemd/system/weston.socket \
            ${IMAGE_ROOTFS}/etc/systemd/system/sockets.target.wants/weston.socket
    fi
}
ROOTFS_POSTPROCESS_COMMAND += "enable_weston; "

# Enable systemd-networkd
enable_networkd() {
    install -d ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants
    ln -sf /usr/lib/systemd/system/systemd-networkd.service \
        ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants/systemd-networkd.service
}
ROOTFS_POSTPROCESS_COMMAND += "enable_networkd; "

# Disable network-online wait (blocks boot)
disable_network_wait() {
    rm -f ${IMAGE_ROOTFS}/etc/systemd/system/network-online.target.wants/systemd-networkd-wait-online.service
}
ROOTFS_POSTPROCESS_COMMAND += "disable_network_wait; "

# Disable firewall
disable_firewall() {
    rm -f ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants/iptables.service
    rm -f ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants/ip6tables.service
}
ROOTFS_POSTPROCESS_COMMAND += "disable_firewall; "

# Bring eth0 up immediately
add_eth0_udev() {
    install -d ${IMAGE_ROOTFS}/etc/udev/rules.d
    printf 'SUBSYSTEM=="net", ACTION=="add", NAME=="eth0", RUN+="/sbin/ip link set eth0 up"\n' \
        > ${IMAGE_ROOTFS}/etc/udev/rules.d/10-eth0.rules
}
ROOTFS_POSTPROCESS_COMMAND += "add_eth0_udev; "

# Ethernet link retry -- sometimes fails first handshake
enable_eth0_retry() {
    echo '[Unit]'                                                   >  ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'Description=Retry eth0 link'                              >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'After=systemd-networkd.service'                           >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'Wants=systemd-networkd.service'                           >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo ''                                                         >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo '[Service]'                                                >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'Type=oneshot'                                             >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'ExecStart=/bin/sh -c "sleep 15; if ! ip link show eth0 | grep -q state.UP; then ip link set eth0 down; sleep 2; ip link set eth0 up; fi"' >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'RemainAfterExit=yes'                                      >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo ''                                                         >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo '[Install]'                                                >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'WantedBy=multi-user.target'                               >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    install -d ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants
    ln -sf /usr/lib/systemd/system/eth0-retry.service \
        ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants/eth0-retry.service
}
ROOTFS_POSTPROCESS_COMMAND += "enable_eth0_retry; "
