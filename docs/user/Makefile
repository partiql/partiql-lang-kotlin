

export TARGET := user-guide
export SRC    := $(addprefix $(CURDIR)/, $(shell cat outline.txt)) # order of md files to be appended in order to create final book

export EXTRASUPPORTFILES := # empty for now 

all:  # default target
	@$(MAKE) -C ../ -f common.mk all


%: force
	@$(MAKE) -C ../ -f common.mk $@
force: ; 
