<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# TI 2.0 Testhub Doctor
> The first release of [Testhub Doctor](https://github.com/gematik/testhub-doctor) is available!
> Before building or starting the Testhub, use its command-line interface (CLI) or desktop GUI to check
> Java/JDK, Maven wrapper, Docker/Compose, required project files, ports, system resources, and registry
> connectivity. Findings include troubleshooting hints. **Testhub 3.18.0 or newer is supported.**
> The Doctor checks prerequisites; it does not install missing software or start the Testhub.
>
> ### Download and run
>
> Download the **CLI or GUI archive for your operating system and CPU architecture** from the
> [Testhub Doctor releases](https://github.com/gematik/testhub-doctor/releases).
> Release assets are named `testhub-doctor-<cli|gui>-<linux|windows|macos>-<architecture>.tar.gz`.
> Choose `ARM64` for Apple Silicon or `X64` for Intel/AMD where available; only use a build matching your
> machine. Extract the archive first. These are standalone executables, not installers or macOS `.app`
> bundles. No Rust installation or Java runtime is needed to run the Doctor itself.
>
> On **macOS/Linux**, open a terminal in the extracted directory:
>
> ```bash
> # CLI: replace the path with your Testhub working-copy root
> ./testhub-doctor check --project-root /path/to/ti2.0-testhub --show-remediation
>
> # Alternatively, start the GUI and select the Testhub working directory
> ./testhub-doctor-gui
> ```
>
> Run only the command for the variant you downloaded. If execute permissions were lost during extraction,
> run `chmod +x ./testhub-doctor` or `chmod +x ./testhub-doctor-gui`.
> For machine-readable results, use `./testhub-doctor check --project-root /path/to/ti2.0-testhub --json`;
> `./testhub-doctor --help` lists the available options.
> The Linux GUI needs WebKitGTK 4.1 and the other GUI libraries listed in the
> [Doctor README](https://github.com/gematik/testhub-doctor#requirements-for-building-from-source).
>
> On **Windows**, extract the `.tar.gz` archive (for example with `tar -xzf <archive>.tar.gz` in PowerShell),
> then open PowerShell in the extracted directory:
>
> ```powershell
> .\testhub-doctor.exe check --project-root "C:\path\to\ti2.0-testhub" --show-remediation
> # Or start the GUI (also possible by double-clicking the extracted .exe)
> .\testhub-doctor-gui.exe
> ```
>
> The Windows GUI requires the Microsoft Edge WebView2 Runtime, which is commonly preinstalled.
> The CLI does not require WebView2.
>
> ### First launch: unsigned binaries
>
> The binaries are not code-signed or notarized with Apple. **Only approve files downloaded from the
> official release page that you trust. Do not disable system-wide security protection.**
>
> On **macOS**, first try starting the binary. If Gatekeeper blocks it, open
> **System Settings → Privacy & Security → Open Anyway** and confirm the exception for that binary.
> Alternatively, remove the quarantine attribute from the specific extracted executable:
>
> ```bash
> sudo xattr -rd com.apple.quarantine ./testhub-doctor
> # For the GUI variant instead:
> sudo xattr -rd com.apple.quarantine ./testhub-doctor-gui
> ```
>
> Use only the command for your downloaded variant, not for an entire download folder.
> Administrator privileges are not normally required to run the Doctor.
>
> On **Windows**, if Microsoft Defender SmartScreen shows **Windows protected your PC**, choose
> **More info → Run anyway** after verifying the download source. If the downloaded file is marked as
> blocked, right-click it, open **Properties → General**, select **Unblock** if available, and apply.
> Managed devices may prohibit these exceptions; contact your administrator rather than disabling
> SmartScreen or antivirus protection.
