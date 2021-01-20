# WebComposer

Reader form several web sites to collect interesting info and group just in one place, in this example Weather from Spain

## execution
A shortcut has been created in *webcomposer.sh*, so you can run:
> ./webcomposer.sh

to work with screenshots or:
> ./webcomposer.sh -f

to work with iframes.

Basically, it cleans the previous files, run the script and open the result.

*webcomposer.kts* and *webscreenshots.kts* are self-executables so you can use:
> ./webcomposer.kts

it works with *kscript* so alternatively you can use:
> kscript webcomposer.kts

you can ensure kscript is working on your computer with:
> kscript --help

and install it with:
> brew upgrade holgerbrandl/tap/kscript

MORE info about kscript: https://github.com/holgerbrandl/kscript

## Extension or personalization
### Other places
The current script has villages interesting for the author. You can change to you town just updating the ids on the **Config** object.

### New Websites with screenshots
1. To include new website just update the *queries* on *Config* object. You can take as example the current entries.


### New Websites with iframes
1. To include new website, create a new class that implement WebSource, e.g. AemetDays
2. Add a new entry on **Config** object with a new key and the properly Id for your requested place
3. You should instantiate the new class on *// NOTE: Instantiate your class here* and read the new config on *// NOTE: add your Config entry here*

## Interesting notes
Jsoup is an really useful library to manage html. Info about Jsoup: https://github.com/jhy/jsoup/

To work with screenshot, Selenium (with ChromeDriver) has been chosen. The behaviour changes with a visual browser vs headless one.

Interesting info about corutines:
https://medium.com/swlh/everything-you-need-to-know-about-kotlin-coroutines-b3d94f2bc982