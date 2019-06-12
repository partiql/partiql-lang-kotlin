####
## Shared makefile vars and targets 
####
ROOTDIR := $(CURDIR)

####
## Commands that need to be installed. See README-AUTHORS.md for instructions. 
####
PANDOC  := $(shell command -v pandoc 2> /dev/null)
XELATEX := $(shell command -v xelatex 2> /dev/null)


####
## Templates, target dir and files needed for output formats (html)
####
PANDOC_TEMPLATE_DIR := $(ROOTDIR)/pandoc-templates
CSSFILES            := $(shell find $(PANDOC_TEMPLATE_DIR) -name *.css)
JSFILES             := $(shell find $(PANDOC_TEMPLATE_DIR) -name *.js)
SUPPORTFILES        := $(CSSFILES) $(JSFILES)
SUPPORTFILES        += $(EXTRASUPPORTFILES) #defined in subdir Makefiles 
OUTPUTDIRNAME       := webapp
OUTPUTPATH          := $(ROOTDIR)/$(OUTPUTDIRNAME)
USERSL              := $(ROOTDIR)/user/$(OUTPUTDIRNAME)
DEVSL               := $(ROOTDIR)/dev/$(OUTPUTDIRNAME)
TUTORIALSL          := $(ROOTDIR)/tutorial/$(OUTPUTDIRNAME)


####
## Vars specific for document builds and deploy to sapp
####
ID                  := $(shell git rev-parse --short HEAD)
DATE                := `date +'%Y/%m/%d (%H:%M:%S)'`

####
## Pandoc Related command line options for each output format 
## See Pandoc docs (https://pandoc.org/MANUAL.html)
####
STANDALONE  := -s
DATEGITHASH := --metadata date="$(DATE) git@$(ID)"
DATADIR     := --data-dir=$(PANDOC_TEMPLATE_DIR)

HTMLTEMPLATE := --template=standalone.html
CSSSTYLE     := --css=style.css
HTMLOPTIONS  := $(STANDALONE) $(DATEGITHASH) -t html5  --toc
HTMLOPTIONS += $(DATADIR)
HTMLOPTIONS += $(HTMLTEMPLATE)
HTMLOPTIONS += $(CSSSTYLE)

TEXPREABLE   := -H final.tex
TEXTEMPLATE  := --template=custom
TEXLISTINGS  := --listings
RESOURCEPATH := --resource-path=$(CURDIR):$(CURDIR)/dev:$(CURDIR)/user
PDFOPTIONS   := $(STANDALONE) $(DATEGITHASH) --pdf-engine=xelatex
PDFOPTIONS += $(DATADIR)
PDFOPTIONS += $(TEXPREABLE)
PDFOPTIONS += $(TEXTEMPLATE)
PDFOPTIONS += $(TEXLISTINGS)
PDFOPTIONS += $(RESOURCEPATH)

PANDOCFILTERS := --filter pandoc-include --filter pandoc-include-code

BIBOPTIONS := --filter pandoc-citeproc

MARKDOWNOPTIONS := -f markdown+tex_math_single_backslash

EPUBOPTIONS = # TBD


all: html pdf epub
	@echo "Done!"

# generate tex output and stop. useful for debugging issues with PDF 
tex: outputdirs haspandoc hasxetex
	$(PANDOC) $(SRC) $(PDFOPTIONS) $(PANDOCFILTERS) $(BIBOPTIONS)  -o $(OUTPUTPATH)/$(TARGET).tex

pdf: outputdirs haspandoc hasxetex
	$(PANDOC) $(SRC) $(PDFOPTIONS) $(PANDOCFILTERS)  $(BIBOPTIONS)  -o $(OUTPUTPATH)/$(TARGET).pdf

epub: outputdirs haspandoc 
	$(PANDOC) $(SRC) $(EPUBOPTIONS) $(PANDOCFILTERS) $(BIBOPTIONS) -o $(OUTPUTPATH)/$(TARGET).epub

html: outputdirs haspandoc
	$(PANDOC) $(SRC) $(HTMLOPTIONS) $(MARKDOWNOPTIONS) $(PANDOCFILTERS) $(BIBOPTIONS) -o $(OUTPUTPATH)/$(TARGET).html
	cp -r $(SUPPORTFILES) $(OUTPUTPATH)/.

# generate md file 
md: outputdirs haspandoc
	$(PANDOC) $(SRC) $(MARKDOWNOPTIONS) $(PANDOCFILTERS) $(BIBOPTIONS) -o $(OUTPUTPATH)/$(TARGET).text


outputdirs: $(OUTPUTPATH) $(USERSL) $(DEVSL) $(TUTORIALSL)

$(OUTPUTPATH): 
	mkdir -p $(OUTPUTPATH)

$(USERSL): $(OUTPUTPATH)
	ln -s $(OUTPUTPATH) $(USERSL)

$(DEVSL): $(OUTPUTPATH)
	ln -s $(OUTPUTPATH) $(DEVSL)

$(TUTORIALSL): $(OUTPUTPATH)
	ln -s $(OUTPUTPATH) $(TUTORIALSL)

.PHONY: clean haspandoc hassapp outputdirs

clean:
	-rm -rf $(OUTPUTPATH)
	-rm dev/$(OUTPUTDIRNAME) user/$(OUTPUTDIRNAME)

haspandoc: 
ifndef PANDOC
	$(error "`pandoc` could not be found in your path. Please install pandoc")
endif

hasxetex: 
ifndef XELATEX
	$(error "`xelatex` could not be found in your path. Please install xetex")
endif

