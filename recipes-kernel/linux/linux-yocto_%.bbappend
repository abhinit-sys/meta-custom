FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += "file://bbb-net.cfg file://dcan1-bbb.dtsi file://can.cfg"

do_configure:append() {
    if [ -f ${S}/arch/arm/boot/dts/ti/omap/am335x-boneblack.dts ]; then
        cp ${WORKDIR}/dcan1-bbb.dtsi ${S}/arch/arm/boot/dts/ti/omap/
        if ! grep -q 'dcan1-bbb.dtsi' ${S}/arch/arm/boot/dts/ti/omap/am335x-boneblack.dts; then
            echo '#include "dcan1-bbb.dtsi"' >> ${S}/arch/arm/boot/dts/ti/omap/am335x-boneblack.dts
        fi
    fi
}
