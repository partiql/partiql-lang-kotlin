# Releases

IonSQL++ current release is available in `live` and we recommend clients to merge from live regularly to avoid lagging
behind features and bug fixes. 

Previous versions can be obtained through building any commit from the release 
branch, starting from [1.0] commit. If you need to depend on a previous commit please reach out to the [Ion team], we 
want to avoid having clients on fragmented versions and may be able to accommodate your needs so you can go back to `live` .

**Warning:** Unless you have explicit permission from the Ion team any dependencies on code before [1.0] will not receive any support, use at your own risk.

## Internal version

IonSQL++ releases follow the [semantic versioning] rules of Major.Minor.Patch where:
* Major is incremented every time a backward incompatible change is included in the release
* Minor is incremented every time new backward compatible functionality is changed
* Patch for backward compatible bug fixes

When a higher version is incremented it resets the lower ones, examples: 
* If the current version is `1.2.55` and a backward incompatible change is introduced the new version will be `2.0.0`
* If the current version is `1.3.99` and a new feature change is introduced the version will be `1.4.0`  

The full version is noted 

- in each release branch commit message, and, 
- as a `git` tag on the `release` branch. 

Include the version when reporting bugs or asking questions, it speeds
up the process to reproduce and avoids confusion

## Brazil

The Brazil version uses the `Major.X` convention, so versions `1.0.15` and `1.1.12` both map to `1.X` in Brazil. This is 
done to push backward compatible changes automatically to customers through `live` avoiding version fragmentation. Any
backward incompatible change, including ones required by bug fixes, will cause a Major version bump

## Communication

You can follow [ionsql-interest] mailing to get notified about new releases and the release notes. Commit messages for
commits in the release branch will have the release notes as well 

## Branches

A quick note about IonSQL branches. There are two main branches: 
* `mainline` for current development 
* `release` for finalized releases. 

**Only** commits in the `release` branch, starting with [1.0], are officially supported, `mainline` may have 
incomplete features and breaking behavior, use it at **your own risk**. 

When a new release is ready to be published commits from mainline are merged into a single commit into 
`release`, this is done to keep all commits in the `release` branch as valid buildable releases. Again it is important 
to note that this starts with version [1.0].

[semantic versioning]: https://semver.org/
[1.0]: https://code.amazon.com/packages/IonSQLSandbox/commits/fdb0e62ef746a2c2f0474c16ca66d72131ed6bb0#
[Ion team]: https://w.amazon.com/index.php/Ion/SQL%2B%2B#Communication
[ionsql-interest]: https://email-list.corp.amazon.com/email-list/email-list.mhtml?action=search&name=ionsql
