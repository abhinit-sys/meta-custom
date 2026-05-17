do_deploy:append() {
    sed -i 's|root=/dev/mmcblk0p2|root=/dev/sda2|' ${DEPLOYDIR}/bootfiles/cmdline.txt
}
