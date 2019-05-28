

export TARGET := tutorial
export SRC    := $(addprefix $(CURDIR)/, $(shell cat outline.txt))

export EXTRASUPPORTFILES := # empty for now 

all:  # default target
	@$(MAKE) -C ../ -f common.mk all


%: force
	@$(MAKE) -C ../ -f common.mk $@
force: ; 
