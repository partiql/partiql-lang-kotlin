

export TARGET := tutorial
export SRC    := $(addprefix $(CURDIR)/,tutorial.md)

export EXTRASUPPORTFILES := # empty for now 

all:  # default target
	@$(MAKE) -C ../ -f common.mk all


%: force
	@$(MAKE) -C ../ -f common.mk $@
force: ; 
