
# Toolchain 

You will need to install the following software 

* Make 
* Pandoc 
    * https://pandoc.org/installing.html
      * use the latest [`cabal`](https://www.haskell.org/cabal/#install-upgrade) installation for your system. 
* Pandoc Filters 
    * [pandoc-include](https://github.com/DCsunset/pandoc-include). Include other md files 
    * [pandoc-include-code](https://github.com/owickstrom/pandoc-include-code). Include files from inside md code blocks. 
    * [pandoc-citeproc](https://hackage.haskell.org/package/pandoc-citeproc). Adds citations. 
      * use `cabal install pandoc-citeproc` to install 
* texlive 
    * https://www.tug.org/texlive/


To build the documentation use `make` under any subfolder of `docs`

* `docs`. Typing `make` will build all documentation for user and dev guides 
* `docs/user`. Typing `make` builds the user guide
* `docs/dev`. Typing `make` builds the dev guide

For more detail see `docs/Makefile`. 

# Authoring Documentation 

All documentation is written in [`pandoc`
markdown](https://pandoc.org/MANUAL.html#pandocs-markdown) stored in
files whose extension is `.md`.

Each guide (user and dev) contain a file `outline.txt` that lists the
`.md` files that will be concatenated together (in the order that they
appear in `outline.txt`) to create the complete guide. `pandoc` is then used 
to translated the complete guide into the output formats (html, pdf, etc). 
