
export TARGET := dev-guide
export SRC    := $(addprefix $(CURDIR)/, $(shell cat outline.txt)) # order of md files to be appended in order to create final book

IMAGES              := $(addprefix $(CURDIR)/, img)
EMAILTEMPLATES      := $(addprefix $(CURDIR)/, EmailTemplates)
UPGRADEINSTRUCTIONS := $(addprefix $(CURDIR)/, UpgradeInstructions)
RELEASENOTES        := $(addprefix $(CURDIR)/, ReleaseNotes)


export EXTRASUPPORTFILES = $(IMAGES) $(EMAILTEMPLATES) $(UPGRADEINSTRUCTIONS) $(RELEASENOTES)

all:  # default target
	@$(MAKE) -C ../ -f common.mk all
    

%: force
	@$(MAKE) -C ../ -f common.mk $@
force: ; 
