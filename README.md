# WebComposer

Reader form several web sites to collect interesting info and group just in one place, in this example Weather from Spain

## execution
A shortcut has been created with:
> ./webcomposer.sh

Basically, it cleans the previous files, run the script and open the result.

webcomposer.kts is self-executable so you can use:
> ./webcomposer.kts

it works with *kscript* so alternatively you can use:
> kscript webcomposer.kts

you can ensure kscript is working on your computer with:
> kscript --help

and install it with:
> brew upgrade holgerbrandl/tap/kscript

MORE info about kscript: https://github.com/holgerbrandl/kscript

## Interesting notes
Jsoup is an really useful library to manage html:
Info about Jsoup: https://github.com/jhy/jsoup/


Interesting info about courutines:
https://medium.com/swlh/everything-you-need-to-know-about-kotlin-coroutines-b3d94f2bc982