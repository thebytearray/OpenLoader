# Security policy

## Reporting a vulnerability

If you believe you have found a security vulnerability in OpenLoader, do not create a public GitHub issue for it until it has been addressed.

Preferred options:

1. GitHub: use [Private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing/privately-reporting-a-security-vulnerability) if it is enabled on this repository.
2. Email: contact the maintainers at a security address published on [thebytearray.org](https://thebytearray.org) or your organization’s security contact, if different.

Include enough detail to reproduce or understand the issue (steps, version, Android API level, and whether Shizuku/ADB was involved), without posting exploit code in public channels before a fix is available.

## Incident handling (intent)

For confirmed issues, the typical process is:

* Confirm scope and affected versions.
* Develop and test a fix; consider related code paths.
* Release patched builds where applicable.
* Publish an advisory or release notes describing the issue after users can update.

This mirrors common open-source practice; exact timelines depend on severity and maintainer capacity.

## PGP / encrypted mail

If maintainers publish a security PGP key or fingerprint on their website, prefer encrypting sensitive reports when appropriate.
