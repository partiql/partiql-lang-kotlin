
####
## SAPP command and related files used for deployment
####
SAPP         := $(shell command -v sapp 2> /dev/null)
SAPP_COMMAND := deploy
SAPP_TARGET  := $(CURDIR)/sapp.json
SAPP_BETA    := $(CURDIR)/sapp-beta.json
SAPP_RELEASE := $(CURDIR)/sapp-release.json


world: html pdf epub 

html: 
	@$(MAKE) -C user -f Makefile html 
	@$(MAKE) -C dev -f Makefile html 

pdf: 
	@$(MAKE) -C user -f Makefile pdf 
	@$(MAKE) -C dev -f Makefile pdf

epub: 
	@$(MAKE) -C user -f Makefile epub
	@$(MAKE) -C dev -f Makefile epub

deploybeta: hassapp world
	@cp $(SAPP_BETA) $(SAPP_TARGET)
	$(SAPP) $(SAPP_COMMAND)
	@rm $(SAPP_TARGET)

deployrelease: hassapp world
	@cp $(SAPP_RELEASE) $(SAPP_TARGET)
	$(SAPP) $(SAPP_COMMAND)
	@rm $(SAPP_TARGET)


clean: 
	-rm $(SAPP_TARGET)
	@$(MAKE) -C user -f Makefile clean
	@$(MAKE) -C dev -f Makefile clean 


.PHONY: clean pdf html epub deployrelease deploybeta hassapp


hassapp: 
ifndef SAPP
	$(error "`sapp` could not be found in your path. Please install sapp")
endif
