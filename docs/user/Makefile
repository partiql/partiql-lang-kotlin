

export TARGET := user-guide
export SRC    := $(addprefix $(CURDIR)/, $(shell cat outline.txt)) # order of md files to be appended in order to create final book

export EXTRASUPPORTFILES := # empty for now 

all: html pdf epub   # allows `make` to work in this dir
    

%: force
	@$(MAKE) -C ../ -f common.mk $@
force: ; 
