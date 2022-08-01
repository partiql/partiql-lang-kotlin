# `partiql-lang-kotlin` Version Migration Guide

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

The repo, [test-partiql-version-migration](https://github.com/alancai98/test-partiql-version-migration), hosts all the 
example migration code between major versions.
