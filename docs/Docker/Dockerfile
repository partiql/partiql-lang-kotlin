FROM ubuntu:18.04 AS base


# Add packages 
ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get -yqq update &&  apt-get -yqq install texlive \
        texlive-xetex \
        texlive-fonts-extra \
        make \ 
        python3 \
        python3-pip \
        libc6-dev \
        libffi-dev \
        libgmp-dev \
        xz-utils \
        zlib1g-dev \
        git \
        gnupg \
        netbase \ 
        curl \ 
        && rm -rf /var/lib/apt/lists/* \
        && apt-get clean

FROM base AS builder

RUN apt-get -yqq update &&  apt-get -yqq install g++ \
        gcc \ 
        git \
        cabal-install\
        && cabal user-config update \ 
        && cabal update \ 
        && cabal install cabal-install \
        && cp /root/.cabal/bin/cabal /usr/bin/cabal \
        && cabal user-config update

# copy and use our cobal config
COPY my.cabal.root.config /root/.cabal/config 

FROM builder AS binaries

RUN cabal update \ 
    && cabal install -fembed_data_files pandoc \ 
    pandoc-citeproc \ 
    pandoc-include-code 

FROM base AS final 
RUN pip3 install pandoc-include 
COPY --from=binaries /root/.cabal/bin/pandoc* /usr/local/bin/
COPY attribution.txt /root/attribution.txt
