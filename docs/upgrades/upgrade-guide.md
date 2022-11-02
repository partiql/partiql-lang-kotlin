# `partiql-lang-kotlin` Version Upgrade Guide

(Note: this folder's documents will likely be converted to a set of wikis on https://github.com/partiql/partiql-lang-kotlin/wiki
with each major version bump on a separate wiki page.
TBD if these documents will remain as a markdown file on GitHub)

The `partiql-lang-kotlin` [release notes](https://github.com/partiql/partiql-lang-kotlin/releases) list changes 
between minor versions as well as the last minor version to the subsequent major version (e.g. v0.2.7 -> v0.3.0). This 
document lists the aggregated release notes between major `partiql-lang-kotlin` versions (e.g. v0.2.7 -> v0.3.4). To 
make it easier to understand differences between `partiql-lang-kotlin` major versions, we've also
* unified the change format (new features, breaking changes - behavioral and API, deprecated items, misc/bug fixes)
* cleaned up some release note items (summarized sequences of related commits, omitted commits related to tests and 
build-related changes)
* moved CLI/REPL changes to a separate section
* found other breaking changes from the compatibility reports between versions using the [japi-compliance-checker](https://github.com/lvc/japi-compliance-checker)

The [CHANGELOG](https://github.com/partiql/partiql-lang-kotlin/blob/main/CHANGELOG.md) can also be viewed to see changes
between releases and also includes a section on features yet to be released (see [Unreleased](https://github.com/partiql/partiql-lang-kotlin/blob/main/CHANGELOG.md#unreleased)).

The repo, [test-partiql-version-upgrade](https://github.com/alancai98/test-partiql-version-migration), hosts all the 
example upgrade code between major versions.
TODO: as part of [partiql-lang-kotlin#692](https://github.com/partiql/partiql-lang-kotlin/issues/692), move the repo
under the `partiql` GitHub organization (either as submodule or as a separate repo).

---

## To create a new upgrade guide
1. Identify the previous version (call this `vA`) and the new version (call this `vB`)
2. Ensure `vA` and `vB` are published to Maven
3. Make a copy of the `vA-to-vB-upgrade-template.md` and fill in relevant sections from the `CHANGELOG` for versions
`vA` and `vB`
4. Within the `version-upgrade` directory, make a copy of the `vA-to-vB-upgrade-template` directory
5. Update `examples`'s `build.gradle` partiql-lang-kotlin version to `vA`
6. Update `migrated-examples`'s `build.gradle` partiql-lang-kotlin version to `vB`
7. In `settings.gradle`, update to include the added Gradle projects
```
'version-upgrade:vA-to-vB-upgrade:examples',
'version-upgrade:vA-to-vB-upgrade:migrated-examples'
```
8. Add the relevant upgrade examples to the `BreakingChanges.kt` file
