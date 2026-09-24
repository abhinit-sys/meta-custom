# Postinst-intercept fixes for Yocto Scarthgap 5.0.15

**These files are NOT part of meta-custom's layer structure.** They are
drop-in replacements for files inside the upstream `poky/` checkout at:

`poky/` is not tracked by this repo, so a fresh clone of Poky (or a `git
reset`/re-checkout inside an existing one) will silently revert to the
original, broken versions. **After any fresh poky checkout, manually copy
these files back in:**

```bash
cp poky-patches/postinst-intercepts/* /home/hs/yocto/poky/scripts/postinst-intercepts/
```

## Root cause (applies to most of these)

`run_intercepts()` in `poky/meta/lib/oe/package_manager/__init__.py` only
ever exports two environment variables into these scripts when it runs
them: `$D` and `$STAGING_DIR_NATIVE`. Several of the original scripts
reference other BitBake variables (`MIMEDIR`, `libdir_native`, etc.)
assuming they'll be available — they never are, and silently resolve to
an empty string. This shows up as a double-slash or truncated path in the
error output (e.g. `.../rootfs/packages` instead of
`.../rootfs/usr/share/mime/packages`).

Additionally, several of these scripts assume a tool/directory always
exists in every sysroot the script runs against. During `do_populate_sdk`
in particular, the same script runs once per internal SDK sysroot layer,
and not every layer has every tool installed — the fix is an explicit
`test -x "<path>" || exit 0` guard before the risky command, added as a
single self-contained line (a multi-line `if/then/fi` block was found to
interact badly with an SDK-variant script generator that does a
fixed-line-count header substitution on these files).

## Per-file summary

| File | Bug | Fix |
|---|---|---|
| `update_mime_database` | `${mimedir}`/`${MIMEDIR}` never exported; wrong path built | Hardcoded `$D/usr/share/mime`, added `mkdir -p` for images without shared-mime-info data |
| `update_mandb` | Assumes `/etc/man_db.conf` always exists | Guard: skip cleanly if man-db isn't installed in this image |
| `update_gtk_immodules_cache` | Assumes gtk-2.0/3.0 immodule dirs exist; original `&&`-chain masked failures | Added directory existence checks + proper `if/then` |
| `update_gio_module_cache` | Assumes `gio-querymodules` + `gio/modules` dir exist | Single-line guard (see root cause note on multi-line breakage) |
| `update_gtk_icon_cache` | `${libdir_native}` never exported (double-slash path bug) | Hardcoded `/usr/lib` for the native pixbuf loader path |
| `update_udev_hwdb` | `udevadm` missing in some `do_populate_sdk` sysroot layers | `test -x` guard, exit 0 cleanly if absent |
| `update_font_cache` | `${fontconfigcacheenv}` empty → shell word-collapse breaks the qemu invocation entirely; `fc-cache` also missing in some SDK layers | Explicit empty-string check before adding `-E`, plus `test -x` guard |

See the project's build-environment documentation for the full
diagnostic trail (how each was found, evidence gathered before each fix).
