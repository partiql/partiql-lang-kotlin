# Docker 

This repository uses a Docker image that contains pandoc and pandoc filters
used to translate the PartiQL documentation into html and pdf.

The Docker image is uploaded to Docker Hub https://hub.docker.com/r/partiqlteam/pandoc

## Building the documentation 

While in this folder execute 

```
cd .. 
docker run -it --entrypoint "/bin/bash" -v `pwd`:/build partiqlteam/pandoc:1.0
cd /build
make
exit
```

The above steps will 

1. `cd ..` move to the parent directory; the root of all documentation
1. ``docker run -it --entrypoint "/bin/bash" -v `pwd`:/build
partiqlteam/pandoc:1.0`` run docker such that
  1. `--entrypoint "/bin/bash"` start up a shell in the docker image
  1. ``-v `pwd`:/build`` mount the current directory, `` `pwd` ``,
  in the docker image as folder `/build`
  1. `partiqlteam/pandoc:1.0` the name of the image 
1. `cd /build` this command is executed once you are inside the docker
image and takes you to the build folder
1. `make` run make to build the documentation 
1. `exit` if make completed successfully, exit the docker image 

All generated documentation is inside the folder `webapp`. The folder
exists in both the docker image as `/build/webapp` as well as your
local filesystem as `<REPO_ROOT>/docs/webapp`.
