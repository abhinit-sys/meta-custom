# meta-custom — Yocto BSP Layer for RPi5 & BeagleBone Black

Custom Yocto Scarthgap 5.0 layer for dual-board embedded Linux
development — Qt6 on Raspberry Pi 5 (aarch64) and Qt5 on
BeagleBone Black Rev D (armv7l).

## Hardware Targets

| Board | SoC | RAM | Qt | IP |
|-------|-----|-----|----|----|
| Raspberry Pi 5 | BCM2712 Cortex-A76 | 8 GB | Qt 6.11.0 | 192.168.0.100 |
| BeagleBone Black Rev D | AM335x Cortex-A8 | 512 MB | Qt 5.15.x | 192.168.0.101 |

## Layer Structure
meta-custom/
├── conf/layer.conf
├── recipes-bsp/           # cmdline.txt: root=/dev/sda2 (USB SSD boot)
├── recipes-connectivity/  # OpenSSH: sshd_config, sshdgenkeys.service
├── recipes-core/images/   # core-image-rpi5-dev.bb + core-image-bbb-dev.bb
├── recipes-graphics/      # Weston: weston.ini (DRM + VNC backend)
├── recipes-kernel/        # Kernel fragments: PHY drivers
├── recipes-qt/            # Qt Wayland test application
└── wic/                   # Custom 10 GB root partition layout

## Key Features

**RPi5 Image (core-image-rpi5-dev.bb)**
- Qt6 6.11.0 with Wayland/Weston compositor
- Bluetooth: BlueZ 5, oFono, obexd (HFP, A2DP, PBAP, AVRCP)
- PipeWire + WirePlumber audio routing
- GStreamer multimedia pipeline
- 15 systemd services via ROOTFS_POSTPROCESS_COMMAND
- Static IP 192.168.0.100, SSH root access, VNC remote display

**Kernel Config Fragments**
- `rpi5-net.cfg` — CONFIG_BROADCOM_PHY=y (BCM54213PE Gigabit PHY)
- `bbb-net.cfg` — SMSC PHY disabled (BBB Rev D probe error -EIO fix)

**Custom Partition Layout (WKS)**
- /boot — 100 MB FAT32 (RPi5 firmware requirement)
- /     — 10 GB ext4 fixed-size (Qt6 + kernel-dev + gcc headroom)

## Notable Issues Resolved

| # | Issue | Fix |
|---|-------|-----|
| 1 | Kernel panic on boot | cmdline.txt root=/dev/sda2 via bbappend |
| 2 | BCM54213PE PHY missing | CONFIG_BROADCOM_PHY=y kernel fragment |
| 3 | SMSC PHY -EIO on BBB Rev D | Disabled SMSC, enabled generic PHY |
| 4 | SSH host key changes on reflash | sshdgenkeys.service with ConditionPathExists |
| 5 | WirePlumber before BlueZ race | BlueZ polling + WirePlumber restart |
| 6 | oFono modem not powered | Manual D-Bus SetProperty after boot |
| 7 | Screen blank (VNC/DRM) | Separate weston instances, wayland-2 socket |
| 8 | Qt6 git:// port blocked | QT_GIT_PROTOCOL=https in local.conf |

## Build

```bash
# Clone and set up layers first (see bblayers.conf)

# RPi5:
source poky/oe-init-build-env build-rpi5
bitbake core-image-rpi5-dev

# BBB:
source poky/oe-init-build-env build-bbb
bitbake core-image-bbb-dev

# Generate SDK:
bitbake core-image-rpi5-dev -c populate_sdk
```

## Layer Dependencies
poky                    (scarthgap 5.0)
meta-raspberrypi
meta-openembedded       (meta-oe, meta-python, meta-networking, meta-multimedia)
meta-qt6                (for RPi5)
meta-qt5                (for BBB — never both simultaneously)

## Author

**Abhinit Kumar** — Senior Embedded Software Engineer
12+ years automotive infotainment | Yocto | Qt6 | Bluetooth | C++17
[LinkedIn](https://linkedin.com/in/abhinit-kumar-93096246) | abhinit.sys@gmail.com
