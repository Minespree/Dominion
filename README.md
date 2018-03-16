# Dominion

[![Discord](https://img.shields.io/discord/352874955957862402.svg)](https://discord.gg/KUFmKXN)
[![License](https://img.shields.io/github/license/Minespree/Dominion.svg)](LICENSE)
![Documentation](https://img.shields.io/badge/docs-javadocs-green.svg)

This is the code that powered the core BungeeCord services of the former Minespree Network. It includes the database models used by the BungeeCord proxies, party and friends managers, the server queue API and utility classes.
There was a plan to integrate this plugin with [Mango](https://github.com/Minespree/Mango) (a new base module that connected all of our backend services), but the server closed down before we were able to migrate to it.

Besides the removal of some branding and configuration data, it is more or less unmodified. It is probably not _directly_ useful to third parties in its current state, but it may be help in understanding how the Minespree network operated.

We are quite open to the idea of evolving this into something more generally useful. If you would like to contribute to this effort, talk to us in [Discord](https://discord.gg/KUFmKXN).

Please note that this project might have legacy code that was planned to be refactored and as so, we kindly ask you not to judge the programming skills of the author(s) based on this single codebase.

## Requirements

To build Dominion, the following will need to be installed and available from your shell:

* [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) version 131 or later (older versions _might_ work)
* [Git](https://git-scm.com/)
* [Maven](https://maven.apache.org/)

You can find detailed installation instructions for these tools on the [Getting started](https://github.com/Minespree/Docs/blob/master/setup/DEPENDENCIES.md) docs page.

## Getting started

This project depends on these modules, so they should also be built and placed on your `plugins/` directory before running Dominion:

* [Myers](https://github.com/Minespree/Myers)

You can build this project running the following command:

```
mvn package
```

Next, move the produced artifact on `target/` to your BungeeCord server `plugins/` directory and restart the proxy. Once started, add your MongoDB and Redis credentials to the `config.yml` file.

## Architecture

This repo contains the following components:

* Data models and services
* MongoDB and Redis service abstractions
* Chat spamming prevention system
* Friend and party managers
* Hub and game instances coordinators
* MOTD updater (uses Redis)
* Whitelist manager
* Server queue API (used for joining servers with friends, parties...)
* Deprecated punishment system. This submodule includes lots of legacy code.

**Note:** most of these components support a multi-proxy setup (by using [RedisBungee](https://github.com/minecrafter/RedisBungee)), but this was never tested.

## Authors

This project was maintained by the Minespree Network team. If you have any questions or problems, feel free to reach out to the specific writers and maintainers of this project:

<table>
  <tbody>
    <tr>
      <td align="center">
          <a href="https://github.com/astei">
            <img width="150" height="150" src="https://github.com/astei.png?v=3&s=150">
            </br>
            Tux
          </a>
        </td>
      <td align="center">
          <a href="https://github.com/hugmanrique">
            <img width="150" height="150" src="https://github.com/hugmanrique.png?v=3&s=150">
            </br>
            Hugmanrique
          </a>
        </td>
      <td align="center">
          <a href="https://github.com/exception">
            <img width="150" height="150" src="https://github.com/exception.png?v=3&s=150">
            </br>
            exception
          </a>
        </td>
    </tr>
  <tbody>
</table>

## Coding Conventions

* We generally follow the Sun/Oracle coding standards.
* No tabs; use 4 spaces instead
* No trailing whitespaces
* No CRLF line endings, LF only, put your git's `core.autocrlf` on `true`.
* No 80 column limit or 'weird' midstatement newlines.

## License

Dominion is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
                                
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

A copy of the GNU Affero General Public License is included in the file LICENSE, and can also be found at https://www.gnu.org/licenses/agpl-3.0.en.html

**The AGPL license is quite restrictive, please make sure you understand it. If you run a modified version of this software as a network service, anyone who can use that service must also have access to the modified source code.**