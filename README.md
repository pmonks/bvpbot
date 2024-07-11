| | |
|---:|:---:|
| [**release**](https://github.com/pmonks/bvpbot/tree/release) | [![Dependencies](https://github.com/pmonks/bvpbot/actions/workflows/dependencies.yml/badge.svg?branch=release)](https://github.com/pmonks/bvpbot/actions?query=workflow%3Adependencies+branch%3Arelease) |
| [**dev**](https://github.com/pmonks/bvpbot/tree/dev) | [![Dependencies](https://github.com/pmonks/bvpbot/actions/workflows/dependencies.yml/badge.svg?branch=dev)](https://github.com/pmonks/bvpbot/actions?query=workflow%3Adependencies+branch%3Adev) |

[![License](https://img.shields.io/github/license/pmonks/bvpbot.svg)](https://github.com/pmonks/bvpbot/blob/release/LICENSE) [![Open Issues](https://img.shields.io/github/issues/pmonks/bvpbot.svg)](https://github.com/pmonks/bvpbot/issues) [![Vulnerabilities](https://github.com/pmonks/bvpbot/actions/workflows/vulnerabilities.yml/badge.svg)](https://pmonks.github.io/bvpbot/nvd/dependency-check-report.html)


<img alt="bvpbot logo" align="right" src="https://github.com/pmonks/bvpbot/blob/release/bvpbot.png?raw=true"/>

# bvpbot

A small [Discord](https://discord.com/) bot.

Please review the [privacy policy](https://github.com/pmonks/bvpbot/blob/release/PRIVACY.md) before interacting with the deployed instance of the bot.

## Contributor Information

[Contributing Guidelines](https://github.com/pmonks/bvpbot/blob/release/.github/CONTRIBUTING.md)

[Bug Tracker](https://github.com/pmonks/bvpbot/issues)

[Code of Conduct](https://github.com/pmonks/bvpbot/blob/release/.github/CODE_OF_CONDUCT.md)

### Developer Tools

`bvpbot` uses [tools.build](https://clojure.org/guides/tools_build) for development-time automation. The full list of available build tasks can be obtained by running:

```shell
$ clojure -A:deps -T:build help/doc
```

### Developer Workflow

This project uses the [git-flow branching strategy](https://nvie.com/posts/a-successful-git-branching-model/), and the permanent branches are called `release` and `dev`. Any changes to the `release` branch are considered a release and auto-deployed to the hosting service.

For this reason, **all development must occur either in branch `dev`, or (preferably) in temporary branches off of `dev`.**  All PRs from forked repos must also be submitted against `dev`; the `release` branch is **only** updated from `dev` via PRs created by the core development team.  All other changes submitted to `release` will be rejected.

## License

Copyright © 2024 Peter Monks

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)

### Attributions

[Bot icon created by iconading - Flaticon](https://www.flaticon.com/free-icons/poop)
