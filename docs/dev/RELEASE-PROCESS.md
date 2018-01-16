# Release Process 

This file documents the steps to be taken for a release of IonSQL++
within Amazon.  For general information on releases and the versioning
scheme used by IonSQL++ see [Releases](Releases.md)


# Minor and Bugfix Releases 

Both minor and bugfix releases imply that there are **no backward
incompatible changes**, as such the process for both release types
is shared.

## Release Process Steps

1. Check that `mainline` builds  
    1. Create a new `brazil` workspace 
        * `brazil ws --create --name IonSQLReleaseMainline --versionSet IonSQLSandbox/development` 
    1. Checkout `IonSQLSandbox` 
        * `brazil ws --use --package IonSQLSandbox`
    1. Checkout the `mainline` branch
        * `git checkout mainline`
    1. Build 
        * `cd IonSQLReleaseMainline/src/IonSQLSandbox/; brazil-build release`
    1. Remove `_dev` suffix from the package version in `mainline` 
        1. Open `Config` and search for `_dev`. For each occurrence update the Brazil package version to the appropriate 
        Brazil package release number. 
            * From `2.x_dev` to `2.x` 
        1. Open `build.xml` and search for `_dev`. For each occurrence update the Brazil package version to the appropriate 
        Brazil package release number. 
            * From `IonSQLSandbox-2.x_dev` to IonSQLSandbox-2.x`
        1. Commit and send a quick CR 
1. Check that `release` builds  
    1. Create a new `brazil` workspace 
        * `brazil ws --create --name IonSQLRelease --versionSet IonSQLSandbox/release` 
    1. Checkout `IonSQLSandbox` 
        * `brazil ws --use --package IonSQLSandbox`
    1. Checkout the `release` branch 
        * `git checkout release`
    1. Build 
        * `cd IonSQLReleaseMainline/src/IonSQLSandbox/; brazil-build release`
1. Get the number of `git` commits that `mainline` is ahead of `release`. 
We will refer to this number as `<N>` below.
    * `git log --pretty=format:"%h"  mainline ^release  | wc -l`
1. Merge `mainline` onto `release` and include all commit messages from `mainline`
    * `git merge --no-ff --log=<N> mainline`
    * **Prepend** to the git merge message the new _Semantic Version_(SV)
      for the package. Leave the rest of the merge message's content
      **intact**
1. Build the code again 
    * `brazil-build clean; brazil-build release`
1. Create a new `git` tag with the new SV version 
    * `git tag -a v<SV> -m "version <SV>"` where `<SV>` is the new semantic version of `IonSQLSandbox`
1. Push your merge commit 
    * `git push` 
1. Push your new tag 
    * `git push origin v<SV>`  where `<SV>` is the semantic version of `IonSQLSandbox`
1. Monitor the release pipeline and ensure that all steps complete successfuly. 
    * [IonSQLSandbox Release Pipeline](https://pipelines.amazon.com/pipelines/IonSQLSandbox-release)
    * The pipeline's last step will build `release` into the `live` version set
1. Update `mainline` to add the `_dev` suffix
    1. Go back to your `mainline` code from step 1
    1. Add `_dev` suffix to the exact same places in `Config`. See your CR that you created that removed the suffix.
    1. Add `_dev`suffix to the exact same paces in `build.xml`. See your CR that you created that removed the suffix.
    1. Commit and send out a quick CR 
1. Send an email to `ionsql-interest@amazon.com` announcing the new release 
    1. Pick the appropriate email template from [ReleaseEmailTemplates](EmailTemplates)
    1. Fill in the template's placeholders 
    1. Send the email out to `ionsql-interest@amazon.com`

# Major releases 

A Major release implies that there **are backwards incompatible** changes
that will break existing clients, as such we must 
* perform extra steps for the release process **and** 
* provide extra documentation.

In this section we will use 

* `old` to refer to the current IonSQL++ version that is in `live`, and, 
* `new` to refer to the new version we are about to release to `live`. 

Before starting the release process make sure you have the following information 

1. The date that the team will **end** support for the `old` version 
    * Make sure you have a date (day, month, year), be specific!
1. Actions that existing clients must take in order to move from `old` to `new` **must** be documented in a separate `.md` file 
under [UpgradeInstructions](UpgradeInstructions)
    * Code changes needed to consume the `new` version (including `Config` changes)
    * API changes that they should be aware of
        * new APIs/Types
        * removed APIs/Types where `<SV>` is the semantic version of `IonSQLSandbox`
        * modified APIs (new/default/removed args, updated return values etc.) 

## Release Process Steps

1. Check that `mainline` builds  
    1. Create a new `brazil` workspace 
        * `brazil ws --create --name IonSQLReleaseMainline --versionSet IonSQLSandbox/development` 
    1. Checkout `IonSQLSandbox` 
        * `brazil ws --use --package IonSQLSandbox`
    1. Checkout the `mainline` branch
        * `git checkout mainline`
    1. Build 
        * `cd IonSQLReleaseMainline/src/IonSQLSandbox/; brazil-build release`
    1. Create a `.md` file under [UpgradeInstructions](UpgradeInstructions)
        1. See the [Readme](UpgradeInstructions/README.md) on naming conventions 
        1. Include all breaking changes and instructions on how to consume the new version in this `.md` file 
    1. Remove `_dev` suffix from the package version in `mainline` 
        1. Open `Config` and search for `_dev`. For each occurrence update the Brazil package version to the appropriate 
        Brazil package release number. 
            * From `3.x_dev` to `3.x` 
        1. Open `build.xml` and search for `_dev`. For each occurrence update the Brazil package version to the appropriate 
        Brazil package release number. 
            * From `IonSQLSandbox-3.x_dev` to IonSQLSandbox-3.x`
    1. Commit and send a CR that includes
        1. The Upgrade Instructions in the `.md` file
        1. The changes to `Config` and `build.xml`
1. Check that `release` builds  
    1. Create a new `brazil` workspace 
        * `brazil ws --create --name IonSQLRelease --versionSet IonSQLSandbox/release` 
    1. Checkout `IonSQLSandbox` 
        * `brazil ws --use --package IonSQLSandbox`
    1. Checkout the `release` branch 
        * `git checkout release`
    1. Build 
        * `cd IonSQLReleaseMainline/src/IonSQLSandbox/; brazil-build release`
1. Get the number of `git` commits that `mainline` is ahead of `release`. 
We will refer to this number as `<N>` below.
    * `git log --pretty=format:"%h"  mainline ^release  | wc -l`
1. Merge `mainline` onto `release` and include all commit messages from `mainline`
    * `git merge --no-ff --log=<N> mainline`
    * **Prepend** to the git merge message the new _Semantic Version_(SV)
      for the package. Leave the rest of the merge message's content
      **intact**
1. Build the code again 
    * `brazil-build clean; brazil-build release`
1. Create a new `git` tag with the new SV version 
    * `git tag -a v<SV> -m "version <SV>"` where `<SV>` is the new semantic version of `IonSQLSandbox`
1. Push your merge commit 
    * `git push` 
1. Push your new tag 
    * `git push origin v<SV>`  where `<SV>` is the semantic version of `IonSQLSandbox`
1. Monitor the release pipeline and ensure that all steps complete successfuly. 
    * [IonSQLSandbox Release Pipeline](https://pipelines.amazon.com/pipelines/IonSQLSandbox-release)
    * The pipeline's last step will build `release` into the `live` version set
1. Update `mainline` to add the `_dev` suffix
    1. Go back to your `mainline` code from step 1
    1. Add `_dev` suffix to the exact same places in `Config`. See your CR that you created that removed the suffix.
    1. Add `_dev`suffix to the exact same paces in `build.xml`. See your CR that you created that removed the suffix.
    1. Commit and send out a quick CR 
1. Update Vendor Guidance for `old` and `new`
    1. Go to [Vendor Guidance](https://code.amazon.com/packages/IonSQLSandbox/releases) 
    1. Mark the `old` release as `NEWER VERSION AVAILABLE`
    1. Mark the `new` release as `RECOMMENDED`
1. Create calendar events for deprecation and send to the team  
    1. Create a calendar event on the day we are going to deprecate the `old` version 
        * On deprecation day
            1. Go to [Vendor Guidance](https://code.amazon.com/packages/IonSQLSandbox/releases) and mark the `old` version as `DEPRECATED`
    1. Create a calendar event 1 month before deprecating the `old` version to send out a reminder email to `ionsql-interest@amazon.com`
1. Send an email to `ionsql-interest@amazon.com` announcing the new release 
    1. Pick the appropriate email template from [ReleaseEmailTemplates](EmailTemplates)
    1. Fill in the template's placeholders 
    1. Send the email out to `ionsql-interest@amazon.com`


# Hot Fixes

In the case when 

* `mainline` has changes that are **not** ready for release, and, 
* there is an issue/bug with the currently released IonSQL++ version, and, 
* we have a code change that we want to apply immediately in order to fix the issue/bug 

we create a _hotfix_. 

1. Create a **new** branch from `release`. The name of the branch typically has the word `hotfix`. 
    * `git checkout -b hotfix-shortcircuiteval`
1. Create the fix in your branch, review and commit to your branch.
1. Merge from your hotfix branch to `release`
    * `git checkout release` 
    * `git merge -no-ff --log=1 hotfix-shortcircuiteval`
1. Build the code on `release` in your workspace 
    * `brazil-build clean; brazil-build release`
1. Create a new `git` tag 
    *  `git tag -a v1.4.1 -m "hotfix 1.4.1"`
1. Push your merge commit 
    * `git push` 
1. Push your new tag 
    * `git push origin v1.4`    
1. Merge from `release` back to `mainline`. This ensures we do not lose the fix in with the next release and updates `mainline` to include the merge event from `release`. 
    * `git checkout mainline`
    * `git merge --no-ff --log=1 release`
1. Ensure that `mainline` builds 
    * `brazil-build clean; brazil-build release` 
1. CR, and then commit and push to `mainline` 
    * `git commit`
    * `git push` 


