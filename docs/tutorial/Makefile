

export TARGET := tutorial
export SRC    := $(addprefix $(CURDIR)/, $(shell cat outline.txt))

export EXTRASUPPORTFILES := # empty for now 

all:  # default target
	@$(MAKE) -C ../ -f common.mk all
	cp -r code ../../cli/archive                        # TODO Better way to get these into the archive folder
	cp  webapp/tutorial.html ../../cli/archive/tutorial.html
	cp  webapp/tutorial.pdf ../../cli/archive/tutorial.pdf


%: force
	@$(MAKE) -C ../ -f common.mk $@
force: ; 
