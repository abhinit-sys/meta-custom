FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " \
    file://sshd_config \
    file://sshdgenkeys.service \
"

do_install:append() {
    install -m 0600 ${WORKDIR}/sshd_config \
        ${D}${sysconfdir}/ssh/sshd_config
    install -m 0644 ${WORKDIR}/sshdgenkeys.service \
        ${D}${systemd_system_unitdir}/sshdgenkeys.service
}
