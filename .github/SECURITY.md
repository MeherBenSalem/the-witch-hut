# Security Policy

## Supported Versions

| Version root | Minecraft line | Loaders |
| --- | --- | --- |
| `1.20.1/` | 1.20.1 | Fabric, Forge |
| `1.21.1/` | 1.21.1 | Fabric, NeoForge |
| `26.2/` | 26.2 | Fabric, NeoForge |
| `26.3/` | 26.3 | Fabric, NeoForge |

Security fixes go to the latest release (2.0.2 and later), whose 8 jars cover exactly these targets. Older releases do
not receive backports. Fixes may be applied selectively depending on impact and porting cost across isolated version roots.

## Reporting a Vulnerability

Do not disclose security issues publicly before maintainers can investigate.

Use GitHub private vulnerability reporting or a private security advisory. Include:

- affected version root and loader
- reproduction steps or proof of concept
- expected impact
- relevant logs or stack traces

For non-security bugs, open a normal GitHub issue.
