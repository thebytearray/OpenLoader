# Contributing

Thanks for your interest in contributing to OpenLoader.

* [Contributing code](#contributing-code)
* [Creating an issue](#creating-an-issue)
* [Pull requests](#pull-requests)
* [Git workflow](#git-workflow)
* [Commit message guidelines](#commit-message-guidelines)

## Contributing code

* By contributing, you agree that your contributions are licensed under the GNU General Public License v3.0, as described in [`LICENSE`](../LICENSE).
* For non-trivial changes, add yourself to [`AUTHORS`](../AUTHORS) (or ask the maintainer to do so in the PR).

## Creating an issue

* Security vulnerabilities: do not open a public issue. Use [`SECURITY.md`](SECURITY.md).
* For bugs, include steps to reproduce, expected vs actual behavior, device/Android version, and OpenLoader version when possible.
* For features, describe the problem you are solving and any constraints (Shizuku vs ADB, etc.).

## Pull requests

Good pull requests stay focused and avoid unrelated changes.

1. Follow the Installation section in [`README.md`](../README.md#installation).
2. Use [`PULL_REQUEST_TEMPLATE.md`](PULL_REQUEST_TEMPLATE.md) when opening a PR.
3. Update [`README.md`](../README.md) or other docs if your change affects behavior, setup, or versioning.

For large features or refactors, open an issue first to align on direction.

## Git workflow

Use a simple, clear branch naming style, for example:

```text
<type>/<short-description>
```

Where `<type>` can be `feature`, `fix`, `docs`, `chore`, etc.

Target the default development branch your fork uses (often `main`); follow any branch protection rules on the upstream repository.

## Commit message guidelines

We recommend [Conventional Commits](https://www.conventionalcommits.org):

```text
<type>(<optional scope>): <short description>

[optional body]

[optional footer]
```

Common values for `<type>`:

* `feat`: new feature
* `fix`: bug fix
* `docs`: documentation only
* `style`: formatting, no logic change
* `refactor`: code change without fixing or adding behavior
* `perf`: performance
* `test`: tests
* `build`: build system or dependencies
* `ci`: CI configuration
* `chore`: maintenance
