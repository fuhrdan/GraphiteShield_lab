# GraphiteShield Lab

GraphiteShield Lab is a local-only, open-source mobile threat behavior emulation and detection workbench. It helps defensive researchers demonstrate suspicious behavior patterns, test detection logic, triage findings, and export evidence without exploiting or connecting to a real device.

## Features

- Eight-tab desktop research interface
- Seven safe experiment scenarios
- Live synthetic telemetry with search, severity filters, sorting, and inspection
- Configurable detection rules with manual rule testing
- Finding acknowledgement, closure, and reopening
- Risk scoring and session dashboard
- Multiple synthetic Android device profiles
- Adjustable experiment playback speed
- CSV telemetry export
- Styled HTML investigation reports
- Sample dataset generator
- Local-only safety boundary with no device or network integration

## Requirements

- Java 17 or newer (a JRE is sufficient)
- Windows, macOS, or Linux

## Run on Windows

Double-click `run-windows.bat`, or open PowerShell in the project folder and run:

```powershell
.\run-windows.bat
```

## Run on macOS or Linux

Make the launcher executable once and run it:

```bash
chmod +x run-linux-mac.sh
./run-linux-mac.sh
```

The launchers start the included `GraphiteShield-Lab.jar`. No compilation, external libraries, images, services, or network downloads are required.

You can also launch it directly:

```bash
java -jar GraphiteShield-Lab.jar
```

## Suggested first experiment

Open **Experiment Lab**, choose **Combined advanced intrusion simulation**, select `2×` speed, and click **Run experiment**. Then inspect **Live Telemetry**, triage items in **Detections**, test a rule in **Detection Rules**, and generate an HTML report from **Reports**.

## Safety model

GraphiteShield Lab is intentionally a simulator:

- It does not contain exploits.
- It cannot connect to phones or use ADB.
- It does not open command-and-control connections.
- It does not collect messages, credentials, location, audio, or other personal information.
- Network addresses are documentation-only TEST-NET fixtures.
- All events and identities are synthetic.

This project is designed for education, blue-team exercises, demonstrations, detection engineering, and controlled defensive research.

## License

MIT License. See `LICENSE`.
