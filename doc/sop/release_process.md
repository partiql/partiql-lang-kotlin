# Release Process

The goal of the release process is to merge new changes into IonSQL++ release branch and build it into
live for consumption. There are three main release process:
* Backward compatible release
* Backward incompatible release
* Patching for a bug fix

## New backward compatible features

### Update version
In mainline branch update `version.info` by bumping the Minor version and commit this change with the following message: 
`Updating version from <previous> to <current>`.   

### Merge into release branch
Merge `mainline` `HEAD` into the `release` branch squashing all commits in a single merge commit: 
```bash
$ git checkout release
$ git merge --squash mainline
$ git commit
``` 

When running `git commit` you will be prompted to edit the commit message, change the message to: 
`Release <semantic version> ()brazil version: <brazil version>), changes:`, 
Example: `Release 1.123.0 (brazil version: 1.x), changes:`.

Keep the body of the message as is, it contains all commit messages from mainline squashed together and serves as a 
complete release note of what's included  

**Note**: The merge squash is performed to ensure that all new commits from mainline appear in the release branch as one 
distinct commit per release candidate. Each release candidate appears as 1 git commit on the release branch. This allows 
clients to safely roll back to a previous release in package builder by selecting a commit from release instead of 
cherry picking multiple commits. 

### Test the candidate commit 
Although [IonSQLSandbox-development][dev pipeline] runs tests for all mainline commits it's important to double check that 
the merge is still passing all tests. Run the tests against a clean workspace, for example by running:
```bash
$ brazil ws clean 
$ brazil-build clean 
$ brazil-build
``` 

### Building it into live
Push the release branch by running `git push origin release`. 
Doing so will trigger [IonSQLSandbox-release][release pipeline] which will take care of pushing the release branch into 
live. Keep an eye on it to see if the there is any issue with the build

### Push mainline
Push the update version change into `mainline`, `git push origin mainline`. This should be the only change in mainline

### Communicate the new release
Send an e-mail to *ionsql-interest@amazon.com* to communicate the new release, include a link to the release notes.

**TODO** is the squashed message enough or do we need more here?

### Update the website   
Go through the process of updating https://sapp.amazon.com/IonSqlpp/, [documentation][website doc] 





## Backward incompatible release
Same process as a non backward compatible change, follow the same steps as above if they are not specified bellow

### Update version
Update the **Major** version in the following places: 
* `version.info` 
* Brazil `Config` file
* `build.xml` project name

Commit these changes with the following message: `Updating version from <previous> to <current>`. Push this change into 
mainline, `git push origin mainline`  

### Update Brazil release guidance 
After pushing the new release into live update the [release guidance][ionsql release] by: 
* marking the new one as `RECOMMENDED`
* marking the old one with `APPROVED` and `NEWER VERSION AVAILABLE`

### Communicate the new release
Send an e-mail to *ionsql-interest@amazon.com* to communicate the new release, include a link to the release notes. 
The e-mail should highlight the fact that the release contains backward incompatible changes and:
* A migration guide 
* Time line to end support on the previous version





## Patching a bug fix
Ideally bug fix changes are done on top of the release branch and then cherry picked into mainline. This is to guarantee
that only changes related to the bug fix are included in the patch release minimizing risk. 

### Test the candidate commit 
If possible add a regression test related to the bug including a SIM or TT reference. Run a full build against a clean
workspace 
```bash
$ brazil ws clean 
$ brazil-build clean 
$ brazil-build
``` 

### Update version
Update `version.info` by bumping the Patch version if it's a backward compatible change or the Major if it's a backward 
incompatible version. Include the version.info change with the rest of bug fix change.   

### Building it into live
Push the release branch by running `git push origin release`. 
Doing so will trigger [IonSQLSandbox-release][release pipeline] which will take care of pushing the release branch into 
live. Keep an eye on it to see if the there is any issue with the build

### Merge the bug fix into mainline
Cherry pick the bug fix commit into mainline and push after resolving any potential conflicts:
```bash
$ git checkout mainline
$ git cherry-pick <commit_hash> 
$ git push origin mainline
```

### Communicate the new release
Send an e-mail to *ionsql-interest@amazon.com* to communicate the bug fix, should include a short bug description,  
SIM or TT reference and the recommendation to perform merge from live to pick it up the fix 

### Update the website   
Go through the process of updating https://sapp.amazon.com/IonSqlpp/. 


[dev pipeline]: https://pipelines.amazon.com/pipelines/IonSQLSandbox-development
[release pipeline]: https://pipelines.amazon.com/pipelines/IonSQLSandbox-release
[ionsql release]: https://code.amazon.com/packages/IonSQLSandbox/releases
[website doc]: https://code.amazon.com/packages/ChargedParticleProtoIonSQLJS/blobs/master/--/docs/deploy.md
