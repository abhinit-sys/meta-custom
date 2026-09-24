# BeagleBone Black — Yocto Build Changes to Enable CAN (DCAN1)

**Scope.** This document records every change made to the BeagleBone Black
Yocto image to bring up native CAN (DCAN1 + SN65HVD230 transceiver), plus the
two boot-time fixes needed to get the board booting the custom image at all. It
is a change-log / reference so the work can be reproduced, reviewed, or folded
permanently into the build. It does NOT include the RPi5 MCP2515 side (that was
a `/boot/config.txt` overlay only, no image rebuild — see the note at the end).

**Environment.**
- Board: BeagleBone Black (AM335x), booting a custom Yocto image from microSD.
- Distro/kernel: Poky (Scarthgap) 5.0.15, kernel linux-yocto 6.6.21.
- Build host: HP OMEN, Ubuntu 26.04 — **builds run ONLY inside the Docker
  container** `crops/poky:ubuntu-22.04` (the bare host's glibc 2.43 / tar 1.35
  are too new for Scarthgap and corrupt the shared sstate-cache). Prompt must
  read `pokyuser@...`, not the host user.
- Custom layer used: `meta-custom`.
- Build dir: `build-bbb`.

---

## 0. Problem summary (what was wrong out of the box)

Three independent problems, fixed in this order:

1. **Board wouldn't boot the SD image at all** — it kept booting the factory
   Debian on eMMC instead of the SD card. (Boot-source problem.)
2. **Even booting the SD card, the kernel hung at `Starting kernel ...`** — the
   generated `extlinux.conf` pointed the device tree at a directory, not a file.
   (Boot-config problem.)
3. **Once booting, there was no CAN at all** — the DCAN1 node was present but
   `disabled`, no pinmux was set for the CAN pins, and the kernel had **no
   c_can driver compiled in**. (The actual CAN-enable work.)

---

## 1. eMMC bootloader erase (force SD boot) — one-time, on-device (NOT a build change)

The board booted factory Debian from eMMC regardless of the SD card, and the
BOOT/S2 button did not reliably switch it. Fix: erase the eMMC's first-stage
bootloader from the U-Boot prompt so the ROM falls through to the SD card.

At the U-Boot `=>` prompt (interrupt autoboot with a keypress):
```text
=> mmc list
OMAP SD/MMC: 0        (SD card)
OMAP SD/MMC: 1        (eMMC)
=> mmc dev 1
=> mmc erase 0 0x800
=> reset
```
`mmc erase 0 0x800` wipes the first 1 MB (2048 x 512-byte blocks) of eMMC — the
MLO/U-Boot region only; the eMMC rootfs is untouched and Debian is restorable
later via a BeagleBoard eMMC-flasher image. After this, the board boots the SD
card automatically, no button needed.

**This is an on-device action, not a change to the Yocto build.** Recorded here
for completeness because it was required to reach the SD image.

---

## 2. extlinux `fdt` fix — currently a manual SD edit; PERMANENT fix belongs in the build

**Symptom:** SD U-Boot found `extlinux.conf`, loaded the kernel, then failed
with `Skipping ... failure retrieving FDT` / `EXTLINUX FAILED`, and the kernel
hung at `Starting kernel ...`.

**Cause:** the generated `extlinux.conf` contained an empty FDT directive:
```text
fdt /
```
i.e. it pointed at the root directory instead of a device-tree file, so U-Boot
could not load a DTB and fell back to an unusable one.

**Manual fix applied on the SD card's boot partition** (mounted on the laptop):
```text
# /extlinux/extlinux.conf  — the fdt line changed from  "fdt /"  to:
fdt /am335x-boneblack.dtb
```
Full corrected stanza:
```text
default Yocto
label Yocto
   kernel /zImage
   fdt /am335x-boneblack.dtb
append root=PARTUUID=076c4a2a-02 rootwait console=ttyS0,115200
```
(The DTB files `am335x-bone.dtb`, `am335x-boneblack.dtb`, `am335x-bonegreen.dtb`
are all present on the boot partition; the boneblack one is correct for this
board.)

**STATUS: NOT YET FIXED IN THE BUILD.** This is currently a per-SD manual edit.
The permanent fix belongs in whatever generates `extlinux.conf` in the image
(the WIC kickstart `.wks` / `bootimg-partition`, or the extlinux/uEnv recipe) so
it emits the real DTB filename instead of an empty `fdt /`. This should be
folded into `meta-custom` and is called out as an open build task.

---

## 3. CAN enable — the actual Yocto build changes (in `meta-custom`)

All CAN-enable work was done through the existing kernel bbappend in
`meta-custom`. Two source files were added and the bbappend was extended.

### 3.1 The kernel bbappend (extended)

File: `meta-custom/recipes-kernel/linux/linux-yocto_%.bbappend`

**Before** (pre-existing):
```text
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += "file://bbb-net.cfg"
```

**After** (with CAN additions):
```text
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += "file://bbb-net.cfg file://dcan1-bbb.dtsi file://can.cfg"

do_configure:append() {
    if [ -f ${S}/arch/arm/boot/dts/ti/omap/am335x-boneblack.dts ]; then
        cp ${WORKDIR}/dcan1-bbb.dtsi ${S}/arch/arm/boot/dts/ti/omap/
        if ! grep -q 'dcan1-bbb.dtsi' \
             ${S}/arch/arm/boot/dts/ti/omap/am335x-boneblack.dts; then
            echo '#include "dcan1-bbb.dtsi"' \
             >> ${S}/arch/arm/boot/dts/ti/omap/am335x-boneblack.dts
        fi
    fi
}
```

Notes:
- The bbappend is `linux-yocto_%` so it also applies to the RPi5 kernel build,
  but the `do_configure:append` is guarded on the boneblack `.dts` existing, so
  it is a no-op for non-BBB builds — safe.
- The `.dtsi` is copied into the in-tree DTS directory and pulled in by
  appending an `#include` to `am335x-boneblack.dts` at configure time. (An
  earlier attempt to `#include` from an out-of-tree path or rely on overlay
  loading was not used; U-Boot on this image loads a base DTB via extlinux with
  no overlay plumbing, so enabling DCAN1 directly in the base DTB is simplest.)

### 3.2 Device-tree fragment (new file)

File: `meta-custom/recipes-kernel/linux/files/dcan1-bbb.dtsi`
```text
/* Enable DCAN1 on BeagleBone Black P9.24 (TX) / P9.26 (RX) */
&am33xx_pinmux {
	dcan1_pins: dcan1_pins {
		pinctrl-single,pins = <
			0x184 0x0002	/* P9.24 uart1_txd.d_can1_tx, MODE2, output */
			0x180 0x0032	/* P9.26 uart1_rxd.d_can1_rx, MODE2, input+pullup */
		>;
	};
};

&dcan1 {
	status = "okay";
	pinctrl-names = "default";
	pinctrl-0 = <&dcan1_pins>;
};
```

Critical detail — **raw pinctrl-single values, NOT the `AM33XX_IOPAD(...)` /
`PIN_OUTPUT_PULLUP | MUX_MODE2` macros.** Using the macros caused DTC parse
errors (`syntax error` / `Unable to parse input tree`) in this tree/include
context. The raw `<offset value>` pairs are what `pinctrl-single,pins` consumes
directly:
- `0x184 0x0002` = P9.24, control-module offset 0x184, mode 2 (d_can1_tx),
  output.
- `0x180 0x0032` = P9.26, control-module offset 0x180, mode 2 (d_can1_rx),
  input + pull enabled.

Verified in the running device tree after boot:
```text
# xxd of .../dcan1_pins/pinctrl-single,pins :
0000 0184 0000 0002 0000 0180 0000 0032
```
and `.../can@0/status` reads `okay`.

Also note a whitespace/heredoc pitfall hit during authoring: the `<` opening the
pin list was dropped once (`pinctrl-single,pins =` with no `<`), which reproduced
the same DTC syntax error at the first pin line — the fix was ensuring the line
reads exactly `pinctrl-single,pins = <`.

### 3.3 Kernel config fragment (new file)

File: `meta-custom/recipes-kernel/linux/files/can.cfg`
```text
CONFIG_CAN=y
CONFIG_CAN_DEV=y
CONFIG_CAN_RAW=y
CONFIG_CAN_BCM=y
CONFIG_CAN_C_CAN=y
CONFIG_CAN_C_CAN_PLATFORM=y
CONFIG_CAN_NETLINK=y
```
Built `=y` (compiled in, not modules) so no module-load step is needed. The two
that were actually missing and blocking probe were `CONFIG_CAN_C_CAN` and
`CONFIG_CAN_C_CAN_PLATFORM` (the AM335x DCAN driver); the rest are the CAN core
and the socket protocols `candump`/`cansend` use.

---

## 4. Build, deploy, and the "two artifacts" gotcha

Rebuild the kernel **inside the container**:
```bash
# host: enter the container (per the migration/build doc), then:
source poky/oe-init-build-env build-bbb
bitbake virtual/kernel -c cleansstate
bitbake virtual/kernel
```
Pitfalls encountered:
- A stale copy of `dcan1-bbb.dtsi` in
  `build-bbb/tmp/work-shared/beaglebone-yocto/kernel-source/arch/arm/boot/dts/ti/omap/`
  survived a `cleansstate` and rebuilt the OLD (broken) fragment. Fix: delete
  that stale copy on the host, then `-c cleansstate` and rebuild so
  `do_configure` recopies the corrected fragment.
- **The DTB change alone is not enough for the CAN-driver change** — the driver
  is compiled into `zImage`. When the config fragment was added, BOTH the new
  `zImage` AND the new `am335x-boneblack.dtb` had to be copied to the SD card.
  A DTB-only copy left `can0` still absent.

Verify the built artifacts before copying:
```bash
# DTB carries the node:
strings tmp/deploy/images/beaglebone-yocto/am335x-boneblack.dtb | grep dcan1
# kernel config took:
grep CONFIG_CAN_C_CAN \
  tmp/work/beaglebone_yocto-poky-linux-gnueabi/linux-yocto/6.6.21+git/\
linux-beaglebone_yocto-standard-build/.config
```

Copy to the SD card boot partition (mounted on the laptop as e.g. `/dev/sda1`):
```bash
cp tmp/deploy/images/beaglebone-yocto/zImage              /mnt/bootpart/zImage
cp tmp/deploy/images/beaglebone-yocto/am335x-boneblack.dtb /mnt/bootpart/am335x-boneblack.dtb
sync
```

---

## 5. On-device verification (what "working" looked like)

```text
# dmesg after boot:
c_can_platform 481d0000.can: c_can_platform device registered (regs=..., irq=29)

# interface present and up:
ip link show                                   # can0 appears
ip link set can0 up type can bitrate 500000
ip -details link show can0                     # state UP, ERROR-ACTIVE, bitrate 500000

# live DT confirms the enable + pinmux:
cat /sys/firmware/devicetree/.../can@0/status  # -> okay
# pinmux-pins shows the dcan1_pins group claimed by 481d0000.can
```
`can0` on the BBB comes up cleanly and reports ERROR-ACTIVE. **This confirms the
software/driver side is fully working.**

Important honesty note carried into the book: the **physical two-node bus
(RPi5 MCP2515 <-> BBB SN65HVD230) does NOT pass frames** — dead-zero RX on the
BBB, RPi5 goes ERROR-PASSIVE/BUS-OFF. That is a physical-layer fault (wrong
~220-300 ohm termination instead of 120 ohm x2 ~= 60 ohm, plus a suspected
CAN-H/CAN-L wiring/contact issue; a common-ground wire was necessary but not
sufficient). None of that is a Yocto/build problem — the build changes above are
complete and correct.

---

## 6. Summary of changes attributable to this work

Build changes (in `meta-custom`, permanent once merged):
1. Extended `recipes-kernel/linux/linux-yocto_%.bbappend`: added `dcan1-bbb.dtsi`
   and `can.cfg` to `SRC_URI`, plus a `do_configure:append` that injects the
   DTSI include into `am335x-boneblack.dts`.
2. Added `recipes-kernel/linux/files/dcan1-bbb.dtsi` — enables `&dcan1` and sets
   the P9.24/P9.26 pinmux (raw pinctrl-single values).
3. Added `recipes-kernel/linux/files/can.cfg` — compiles the CAN core + c_can
   platform driver into the kernel.

On-device / SD actions (not build changes, but required):
4. Erased eMMC bootloader (`mmc erase 0 0x800`) to force SD boot.
5. Hand-edited SD `extlinux.conf` `fdt /` -> `fdt /am335x-boneblack.dtb`.

Open build task (recommended, not yet done):
6. Fix the image/WIC config so `extlinux.conf` is generated with the correct DTB
   filename, making change #5 unnecessary on future SD writes.

RPi5 side (for contrast, no image rebuild): CAN there is the MCP2515 SPI module
enabled purely via `/boot/config.txt`:
```text
dtparam=spi=on
dtoverlay=mcp2515-can0,oscillator=8000000,interrupt=25
```
(classic CAN only; no CAN-FD).
