DESCRIPTION = "RPi5 Development Image -- Qt6 + Bluetooth + Wayland"
LICENSE = "MIT"
inherit core-image

#################################################
# RPi5 IMAGE WITH QT6 (6.11.0) -- FINAL VERSION
#
# ALL FIXES BAKED IN:
# - SSH works on every boot (ExecStartPre mkdir)
# - Ethernet auto-retries (eth0-retry service)
# - Network matches any interface (Name=e*)
# - seatd enables GPU access for Weston
# - Weston auto-starts for display
# - VNC available for remote display on laptop
#
# SEPARATE FIX NEEDED (bbappend):
# - cmdline.txt root=/dev/sda2 (see rpi-cmdline bbappend)
#
# Build: bitbake core-image-rpi5-dev
# Flash: gunzip -c image.wic.gz | sudo dd of=/dev/sda bs=4M status=progress conv=fsync
# SSH:   ssh root@192.168.0.100
# VNC:   On RPi5: weston --backend=vnc-backend.so --port=5900 &
#        On laptop: vncviewer 192.168.0.100:5900
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
# multi-pane terminal: 4 panels in 1 SSH session
#################################################
IMAGE_INSTALL += " \
    openssh \
    gdb \
    gdbserver \
    tmux \             
    python3-pygments \ 
    lsof \        
    socat \           
    python3-pyserial \ 
    python3-lxml \     
    strace \
    ltrace \
    perf \
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
# BLUETOOTH
#################################################
IMAGE_INSTALL += " \
    bluez5 \
    bluez5-obex \
    ofono \
    bluez-tools \
    bluez5-testtools \
"

#################################################
# QT6 RUNTIME (6.11.0)
#################################################
IMAGE_INSTALL += " \
    qtbase \
    qtdeclarative \
    qtwayland \
    qtconnectivity \
    qtmultimedia \
    qtsvg \
    qtshadertools \
    qt5compat \
"

#################################################
# QT6 DEV TOOLS
# (qttools removed -- qlitehtml repo is dead)
#################################################
IMAGE_INSTALL += " \
    qtbase-dev \
    qtdeclarative-dev \
"

#################################################
# WAYLAND + WESTON DISPLAY SERVER + VNC
# weston supports VNC backend for remote display
# on laptop without physical monitor
#################################################
IMAGE_INSTALL += " \
    wayland \
    wayland-utils \
    weston \
    weston-init \
    seatd \
    libpam \
    mesa \
    mesa-megadriver \
    kmscube \
    libdrm \
    libdrm-tests \
"

#################################################
# AUDIO -- PipeWire for Bluetooth A2DP
#################################################
IMAGE_INSTALL += " \
    pipewire \
    pipewire-alsa \
    pipewire-pulse \
    alsa-utils \
    alsa-lib \
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
"

#################################################
# SQLITE -- lightweight database for app storage
#################################################
IMAGE_INSTALL += " \
    sqlite3 \
"

#################################################
# GSTREAMER -- media playback pipeline
# base = core elements (playbin, decodebin)
# good = stable plugins (autoaudiosink, wavparse)
# bad  = less stable (A2DP sink, HLS)
# ugly = patent-encumbered (mp3) needs commercial license
#################################################
IMAGE_INSTALL += " \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good \
    gstreamer1.0-plugins-bad \
    gstreamer1.0-plugins-ugly \
"

#################################################
# ROOTFS POST-PROCESS COMMANDS
# All fixes baked in -- no manual fixing needed!
#################################################

# Fix fstab for boot partition
fix_boot_fstab() {
    sed -i '/\/boot/d' ${IMAGE_ROOTFS}/etc/fstab
    echo "LABEL=boot  /boot  vfat  defaults,nofail  0  0" \
        >> ${IMAGE_ROOTFS}/etc/fstab
}
ROOTFS_POSTPROCESS_COMMAND += "fix_boot_fstab; "

# Serial console on ttyAMA0 (RPi5 UART)
enable_serial_getty() {
    install -d ${IMAGE_ROOTFS}/etc/systemd/system/getty.target.wants
    ln -sf /usr/lib/systemd/system/serial-getty@.service \
        ${IMAGE_ROOTFS}/etc/systemd/system/getty.target.wants/serial-getty@ttyAMA0.service
}
ROOTFS_POSTPROCESS_COMMAND += "enable_serial_getty; "

# Static IP on Ethernet (192.168.0.100)
# Name=e* matches eth0, end0, enp1s0 (all possible names)
create_network_config() {
    install -d ${IMAGE_ROOTFS}/etc/systemd/network
    echo '[Match]'                       >  ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
    echo 'Name=e*'                       >> ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
    echo ''                              >> ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
    echo '[Network]'                     >> ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
    echo 'Address=192.168.0.100/24'      >> ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
    echo 'Gateway=192.168.0.1'           >> ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
    echo 'DNS=192.168.0.1'               >> ${IMAGE_ROOTFS}/etc/systemd/network/10-eth.network
}
ROOTFS_POSTPROCESS_COMMAND += "create_network_config; "

# Disable conflicting sysvinit
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

# SSH via direct daemon with /var/run/sshd fix
# ExecStartPre creates the privilege separation directory
# which is on tmpfs and gets wiped on every reboot
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

# seatd for GPU/input access (Weston needs this)
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

# Ethernet link retry -- RPi5 PHY sometimes fails first handshake
# This bounces eth0 5 seconds after boot to force link up
enable_eth0_retry() {
    echo '[Unit]'                                                   >  ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'Description=Retry eth0 link'                              >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'After=systemd-networkd.service'                           >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'Wants=systemd-networkd.service'                           >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo ''                                                         >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo '[Service]'                                                >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'Type=oneshot'                                             >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'ExecStart=/bin/sh -c "sleep 5; ip link set eth0 down; sleep 1; ip link set eth0 up"' >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'RemainAfterExit=yes'                                      >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo ''                                                         >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo '[Install]'                                                >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    echo 'WantedBy=multi-user.target'                               >> ${IMAGE_ROOTFS}/usr/lib/systemd/system/eth0-retry.service
    install -d ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants
    ln -sf /usr/lib/systemd/system/eth0-retry.service \
        ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants/eth0-retry.service
}
ROOTFS_POSTPROCESS_COMMAND += "enable_eth0_retry; "

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

# Bring eth0 up via udev
add_eth0_udev() {
    install -d ${IMAGE_ROOTFS}/etc/udev/rules.d
    printf 'SUBSYSTEM=="net", ACTION=="add", NAME=="eth0", RUN+="/sbin/ip link set eth0 up"\n' \
        > ${IMAGE_ROOTFS}/etc/udev/rules.d/10-eth0.rules
}
ROOTFS_POSTPROCESS_COMMAND += "add_eth0_udev; "
# Enable WirePlumber (PipeWire session manager)
# Discovers audio/video devices for PipeWire
enable_wireplumber() {
    install -d ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants
    ln -sf /usr/lib/systemd/system/wireplumber.service \
        ${IMAGE_ROOTFS}/etc/systemd/system/multi-user.target.wants/wireplumber.service
    install -d ${IMAGE_ROOTFS}/root/.local/state/wireplumber
}
ROOTFS_POSTPROCESS_COMMAND += "enable_wireplumber; "

# Setup SSH config to disable strict host key checking for dev
# This creates a laptop-friendly known_hosts setup
setup_ssh_config() {
    install -d ${IMAGE_ROOTFS}/etc/ssh
    if ! grep -q 'StrictHostKeyChecking' ${IMAGE_ROOTFS}/etc/ssh/sshd_config 2>/dev/null; then
        echo 'PermitRootLogin yes'          >> ${IMAGE_ROOTFS}/etc/ssh/sshd_config
        echo 'PasswordAuthentication yes'   >> ${IMAGE_ROOTFS}/etc/ssh/sshd_config
    fi
}
ROOTFS_POSTPROCESS_COMMAND += "setup_ssh_config; "
