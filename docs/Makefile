
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

clean: 
	@$(MAKE) -C user -f Makefile clean
	@$(MAKE) -C dev -f Makefile clean 


.PHONY: clean pdf html epub 
