# ps5-bdjb-modified-ISOs
<br />
<br />

## Auto Jailbreak and HEN ALL-IN-ONE ISO for 3.xx-7.xx (up to 12.00 with BDJ-UNPATCH)

<br />
<br />
<br />
<br />
i am on 7.61, 10.00 and 11.40 so those are the firmware versions i test on
<br />
<br />
Contact me on discord: Viktorious_x for feedback
<br />
<br />
<br />

### If you would like to support what I do, here is my donation page:
<br />

[![ko-fi](https://github.com/CostasAK/react-kofi-button/blob/main/screenshots/default_preset.gif?raw=true)](https://ko-fi.com/viktorious)

<br />

#### Notice! If you donate do not mention jailbreaking or hacking or anything associated with that!
<br />
Thanks!
<br />
<br />
<br /> 

## Beta ISO versions:
Some of my ISO betas are available on my [ko-fi](https://ko-fi.com/viktorious/shop)
<br /> 

My latest ISO beta: [v20BETAv2](https://ko-fi.com/s/ce844a3276) (OUTDATED) (MORE BETAS SOON)
<br /> 

These are outdated, I havent't done beta testing for the past 6 versions
<br /> 
<br /> 

## Custom ISO versions:
I can create you a custom iso version for your liking
<br /> 

What I can change:
- Icons (Pipeline, Disc, USB, Remote)
- Thumbnail (the AUTO JAILBREAK ICON and Title too)
- All the text overall - I can translate the iso to another language etc
- customize pipelines 
- and more!
<br /> 

DM me on discord: viktorious_x for pricing and details.
<br />
<br /> 
<br /> 


## Tutorial: 
<br />
Burn the latest iso on your BD disk using imgburn (or any other preferred software), run the disk on your ps5 and select any pipeline (i reccomend 1. pipeline), Once the process is finished you will be on the home menu and etahen will be automatically loaded as soon as you are on the home screen. 
<br />
Enjoy!
<br />
<br />

[Modded Warfare's tutorial](https://www.youtube.com/watch?v=r6BAxgGJxcE) (outdated ISO in this video, but the tutorial is useful)
<br />

[Michael Crump's video overview](https://www.youtube.com/watch?v=bmr3Ybz8olU) (latest version v19 in this video)
<br />

<br />

If you have a BD-r and you still want to edit files on the iso in the future here is a tutorial by [nas7536](https://www.youtube.com/@nastechdsi):
<br />
It's built into Windows
Double click the disc in Explorer, select "like a USB flash drive", copy the files from the ISO (do not copy the ISO itself), then right click on the disc -> Eject
<br />

[Tutorial Video](https://www.youtube.com/watch?v=skGlofv9uxg) 
<br />
download source code zip instead of iso if you wanna use this method!
<br />
<br />

## etaHEN configs & other stuff
<br />
<br />
Etahen is loaded from the disc normally, but if you dont want to update iso to update etahen you can either have etaHEN.bin on your usb ROOT formatted in exfat or in your data folder on your ps5.
<br />

The way it checks where to load from is in this order: USB, /data on ps5 and then if those two dont have etaHEN.bin on there it will load it from the disc itself - Huge thanks to
 [BenNox_XD](https://github.com/BenNoxXD) for making this a thing, he is the developer behind it

<br />
<br />
<br />
<br />

### Here is the full list of configs for etahen/kstuff
<br />
<br />
<br />
<br />

```
1.Default: etaHEN will be loaded from the disc, and you don't have to change anything.

2. etaHEN only: Copy an etaHEN.bin file to the root of a USB drive (priority 1) or to the /data/ directory (priority 2) on the PS5, and it will be loaded instead of the one from the disc.

3. Kstuff only: Same as above, but with kstuff. Copy a kstuff.elf to the root of a USB drive (priority 1) or to the /data/ directory (priority 2) on the PS5, and it will be loaded instead of etaHEN.

4. Combined: Place both etaHEN.bin and kstuff.elf on the root of a USB drive (priority 1) or in the /data/ directory (priority 2) on the PS5, and place a no_kstuff file on the USB root or in /data/etaHEN/. Now etaHEN will be loaded, followed by kstuff.
It is also supported for etaHEN and kstuff to be in different locations. For example, etaHEN can be on the USB and kstuff in the /data/ directory.
```

<br />
<br />
<br />

## Explanation of what you can do with this ISO

<br />
Pipelines are a sequence of payloads run so it will run the payloads all in one sequence so you dont have to do it yourself, its easier, faster and more automated that way.
<br />
there are 6 pipelines in total, I will divide it into 6 categories: ALL-IN-ONE, NormalJailbreak, etahen, kstuff, elf-arsenal and linux (poopsploit in the future)
<br />
<br />

__________________________________________________________________________________________________________________________________________________________________________________
### All-In-One pipelines:

<br />
These pipelines have everything you would want
<br />

#### What it runs: UMTX - ELF LOADER- WEB SERVER - BACKPORK -  SHADOWMOUNT - ETAHEN (kstuff included) 

__________________________________________________________________________________________________________________________________________________________________________________
### Jailbreak-ONLY pipelines

<br />
These pipelines are more simplified and has the minimal you need as fast as possible
<br />

#### What it runs: UMTX - ELF LOADER - ETAHEN (kstuff included) 

__________________________________________________________________________________________________________________________________________________________________________________
### kstuff pipelines

<br />
All-In-One and Jailbreak-ONLY pipelines with kstuff being loaded instead of etaHEN
<br />

#### What it runs: Whatever the All-In-One-etaHEN pipeline runs, but with kstuff instead of etaHEN.

__________________________________________________________________________________________________________________________________________________________________________________

### elf-arsenal pipelines

<br />
All-In-One and Jailbreak-ONLY pipelines with elf-arsenal being loaded instead of etaHEN
<br />

#### What it runs: Whatever the All-In-One-etaHEN pipeline runs, but with elf arsenal instead of etaHEN.

__________________________________________________________________________________________________________________________________________________________________________________

### Linux pipeline
<br />
Loads the UMTX exploit, then exits the BDJ enviornment, and launches the Linux Loader
<br />

#### What it runs: UMTX - ELF LOADER - Linux-Loader

__________________________________________________________________________________________________________________________________________________________________________________
<br />

## This iso includes 4 options: pipeline loader, jar loader, usb loader (for elf/bin/jar payloads) and remote jar loader
<br />
<br />
<br />

### jar files on the iso:
<br />
<br />

umtx1.jar (modified umtx1 jar by [me](https://github.com/Viktorious-x) to be significantly faster)
<br />

jailbreak.jar (required to load this payload to be able to load payloads from USB loader)
<br />

elfloader.jar ([modified elf loader jar payload](https://github.com/Viktorious-x/bdjb-elfloader) by that has updated [elfloader.elf](https://github.com/ps5-payload-dev/elfldr))
<br />

etahen.jar - [BD-J Hen Loader](https://github.com/BenNoxXD/PS5-BDJ-HEN-loader) by [BenNox_XD](https://github.com/BenNoxXD) (bundled with [etaHEN](https://github.com/etaHEN/etaHEN) by [LightningMods](https://github.com/lightningmods)) (requires Elf Loader running)
<br />

kstuff.jar - [BD-J Hen Loader](https://github.com/BenNoxXD/PS5-BDJ-HEN-loader) by [BenNox_XD](https://github.com/BenNoxXD) (bundled with [kstuff-lite](https://github.com/EchoStretch/kstuff-lite) by [LightningMods](https://github.com/EchoStretch)) (requires Elf Loader running)
<br />

websrv.jar - [websrv.elf](https://github.com/ps5-payload-dev/websrv/) by [John Törnblom aka SB](https://github.com/john-tornblom) (requires Elf Loader running)
<br />

ftpsrv.jar - ftp server, included in etahen, [ftpsrv.elf](https://github.com/ps5-payload-dev/ftpsrv/) (by [John Törnblom aka SB](https://github.com/john-tornblom)) (requires Elf Loader running)
<br />

airpsx.jar - Remote management software for Jailbroken PlayStation 5, [airpsx.elf](https://github.com/barisyild/airpsx) (by [barisyild](https://github.com/barisyild)) (requires Elf Loader running)
<br />

ps5debug.jar - Debugger for the PlayStation 5, included in etahen, [ps5debug-NG](https://github.com/OpenSourcereR-dev/ps5debug-NG) (by github.com/OpenSourcereR-dev) (requires Elf Loader running)
<br />

linux-loader.jar - autocloses Disc Player, then runs [Linux-Loader](https://github.com/ps5-linux/ps5-linux-loader) by [TheFlow](https://github.com/TheOfficialFloW) (uses modified [BDJ Henloader](https://github.com/BenNoxXD/PS5-BDJ-HEN-loader/tree/main) code (requires Elf Loader running)
<br />

shadowmount.jar - [ShadowMount](https://ko-fi.com/s/42233d1d5a) by [VoidWhisper](https://ko-fi.com/B0B11RW1NE/shop) (requires Elf Loader running)
<br />

backpork.jar - [BackPork](https://github.com/BestPig/BackPork) by [BestPig](https://github.com/BestPig) (requires Elf Loader running)
<br />

np-fake-signin.jar - [np-fake-signin](https://github.com/earthonion/np-fake-signin) by [earthonion](https://github.com/earthonion) (requires Elf Loader running)
<br />

ghostcontrol.jar - [GhostControl](https://github.com/StonedModder/Ghostcontrol-PS5-USB-Controller-Patcher) by [StonedModder](https://github.com/StonedModder)
<br />

klogsrv.jar - [klogsrv](https://github.com/ps5-payload-dev/klogsrv) by [John Törnblom aka SB](https://github.com/john-tornblom) (requires Elf Loader running)
<br />

gdbsrv.jar - [gdbsrv](https://github.com/ps5-payload-dev/gdbsrv) by [John Törnblom aka SB](https://github.com/john-tornblom) (requires Elf Loader running)
<br />

shsrv.jar - [shsrv](https://github.com/ps5-payload-dev/shsrv) by [John Törnblom aka SB](https://github.com/john-tornblom) (requires Elf Loader running)
<br />

ps5dpiv2.jar - [ps5-dpi-v2](https://git.etawen.dev/soniciso/ps5-dpi-v2) by [Sonic-Iso](https://git.etawen.dev/soniciso) (requires Elf Loader running)
<br />

fancontrol.jar - [fan-control](https://git.etawen.dev/soniciso/fan-control) by [Sonic-Iso](https://git.etawen.dev/soniciso) (requires Elf Loader running)
<br />

uds-trophy-unlocker.jar and uds-trophy-unlocker-probe.jar - [uds-trophy-unlocker](https://git.etawen.dev/soniciso/uds-trophy-unlocker) by [Sonic-Iso](https://git.etawen.dev/soniciso) (requires Elf Loader running)
<br />

ps5fwspoof.jar - ps5-fw-spoof by [Sonic-Iso](https://git.etawen.dev/soniciso) (requires Elf Loader running)
<br />

backuphelper(snapshot+restore).jar - [ps5-backup-helper](https://git.etawen.dev/soniciso/ps5-backup-helper) by [Sonic-Iso](https://git.etawen.dev/soniciso) (requires Elf Loader running)
<br />


[CloseDisk.jar](https://github.com/BenNoxXD/PS5-BDJ-HEN-loader/tree/main/ClosePlayer) (Closes Disk Player by [BenNox_XD](https://github.com/BenNoxXD)) (requires Elf Loader running)
<br />
<br />
<br />
__________________________________________________________________________________________________________________________________________________________________________________

## Videos & articles about my ISO:

<br />
<br />

### English:
<br />


[Michael Crump](https://www.youtube.com/@mbcrump): [video overview](https://www.youtube.com/watch?v=bmr3Ybz8olU) (v19)
<br />

[Modded Warfare](https://www.youtube.com/@MODDEDWARFARE): [video tutorial](https://www.youtube.com/watch?v=r6BAxgGJxcE) (v8) , [video overview](https://www.youtube.com/watch?v=lAAQJOywD-8&t=678s) (v26) 
<br />
<br />
Modded Warfare's tutorial about my iso is outdatded, but still useful!
<br />
<br />

[Viktorious](https://www.youtube.com/@viktor7628) (oh wait that's me :) ): [Linux with Viktorious v26 overview video](https://www.youtube.com/watch?v=wecJHEraKaU) (v26)
<br />

[Goldengames](https://www.youtube.com/@goldengames7890): [video](https://www.youtube.com/watch?v=UqUQXR0NEAQ) (v20)
<br />

[Dravszoo](https://www.youtube.com/@dravszoo): [video](https://www.youtube.com/watch?v=4tCXfmOhLd8) (v20)
<br />

[GameConsoleFish](https://www.youtube.com/@GameConsolefish): [video tutorial](https://www.youtube.com/watch?v=I47r8fKjTec) (v19)
<br />

[Mc Kuc](https://www.youtube.com/@McKuc/) in collaboration with me: [video overview + custom icons](https://www.youtube.com/watch?v=0kusnCdyYuc) (v19 BETA Version)
<br />
<br />

### Spanish:
<br />

[TheWizWiki](https://www.youtube.com/@TheWizWiki): [video](https://www.youtube.com/watch?v=Asrdx3uHj4w) (v20)
 
<br />
<br />

### Portuguese:
<br />

[Explosão do Game](https://www.youtube.com/@Explosaodogame): [video](https://www.youtube.com/watch?v=zXCYS4ULb34), [short video](https://www.youtube.com/shorts/yImqPuz7toU) (v19)
<br />

[Exploit BR](https://www.youtube.com/@GAMERAMBX): [video](https://www.youtube.com/watch?v=22ihSgf5rko) (v19)
<br />
<br />

### Italian:
<br />

[BiteYourConsole](https://x.com/BiteYourConsole): [article](https://www.biteyourconsole.net/2025/09/05/scena-ps5-rilasciato-ps5-bdjb-modified-iso-v1-20-v20) (v20)
<br />

[GamesAndConsoles](https://x.com/GAMESANDCON): [article](https://www.gamesandconsoles.net/ps5-ps5-bd-jb-modified-isos-v1-20-la-guida-completa-alliso-all-in-one-offline-piu-aggiornata/) (v20)
<br />
<br />
 
### Filipino:
<br />

[JhanGamingZone](https://www.youtube.com/@JhanGamingZone): [video tutorial](https://www.youtube.com/watch?v=oYA566BPz7c) (v19)
<br />

[TITOJOFTV](https://www.youtube.com/@jofernmacawili2313/): [video](https://www.youtube.com/watch?v=ko1yFdUXfRw) (v19)
<br />

***
<br />
<br />
<br />

## Credits
### Thanks to...
<br />

[Hammer-83](https://github.com/hammer-83) and all the other Contributors ([here](https://github.com/hammer-83/ps5-jar-loader/blob/main/README.md#credits)) for the Original [Ps5 Jar Loader](https://github.com/hammer-83/ps5-jar-loader) project
<br />

[TheFlow](https://github.com/theofficialflow) for many contirbutions to the Ps5 Scene, including multiple exploits and [Ps5 linux](https://github.com/ps5-linux/ps5-linux-loader)
<br />

[Echostretch](https://github.com/EchoStretch) and all the other Contributors for [kstuff](https://github.com/EchoStretch/kstuff) ([ko-fi](https://ko-fi.com/echostretch/))
<br />

[LightningMods](https://github.com/LightningMods) and all the other Contributors ([here](https://github.com/etaHEN/etaHEN?tab=readme-ov-file#contributors)) for [etaHEN](https://github.com/etaHEN/etaHEN) ([ko-fi](https://ko-fi.com/lightningmods)) and [Itemzflow](https://pkg-zone.com/details/ITEM00001)
<br />

[iakdev](https://github.com/iakdev) for USB loader and Pipeline runner ([here](https://github.com/iakdev/ps5-jar-loader))
<br />

[John Törnblom](https://github.com/john-tornblom) (aka SB) and all the other Contributors for [PS5SDK](https://github.com/PS5Dev/PS5SDK), [websrv](https://github.com/ps5-payload-dev/websrv/), [ftpsrv](https://github.com/ps5-payload-dev/ftpsrv), [gdbsrv](https://github.com/ps5-payload-dev/gdbsrv), [klogsrv](https://github.com/ps5-payload-dev/klogsrv) and [shsrv](https://github.com/ps5-payload-dev/shsrv)
<br />

[CryoNumb](https://github.com/cryonumb/) for [BD-J Elf Loader](https://github.com/cryonumb/elfloader)
<br />

[Jaime](https://x.com/Jaime_Cyber/) for the poopsploit port, for hammer83s BDJB API
<br />

[barisyild](https://github.com/barisyild/) for [airpsx](https://github.com/barisyild/airpsx)
<br />

[BenNox_XD](https://github.com/BenNoxXD) for [BD-J Hen Loader](https://github.com/BenNoxXD/PS5-BDJ-HEN-loader), Modified [websrv.jar](https://github.com/BenNoxXD/PS5-BDJ-HEN-loader/releases/tag/2.1b) for extra stability and help regarding this project
<br />

[DriveRick](https://github.com/DriveRick) for [the Original Modded Menu for the Ps5 Jar Loader](https://github.com/DriveRick/PS5-ToolDisc) and help regarding this project ([ko-fi](https://ko-fi.com/firlefanz))
<br />

[Sonic-Iso](https://git.etawen.dev/soniciso) for [elf-arsenal](https://git.etawen.dev/soniciso/elf-arsenal) and all his other cool projects
<br />

[StonedModder](https://github.com/StonedModder) for [GhostControl](https://github.com/StonedModder/Ghostcontrol-PS5-USB-Controller-Patcher)
<br />

[BestPig](https://github.com/BestPig) for [BackPork](https://github.com/BestPig/BackPork)
<br />

[VoidWhisper](https://ko-fi.com/B0B11RW1NE/shop) for [ShadowMount](https://ko-fi.com/s/42233d1d5a) and [VoidShell](https://ko-fi.com/s/99aac5c463)
<br />

[EarthOnion](https://github.com/earthonion/) for [np-fake-signin](https://github.com/earthonion/np-fake-signin)
<br />

[Viktorious (Me) ](https://github.com/Viktorious-x) and a Anonymous developer (for the help with figuring it out at first) for modified umtx1.jar that is significantly faster and more stable than the original umtx1.jar ([ko-fi](https://ko-fi.com/viktorious))
<br />

[ctn123](https://github.com/ctn123) and [SiSTRo](https://github.com/SiSTR0) ([ko-fi](https://ko-fi.com/SiSTRo) for [ps5debug](https://github.com/GoldHEN/ps5debug)

[Safety (aka 54f3ty aka Elon Musk)](https://github.com/54f3ty) for helping me start this project
<br />

[Dr.Oid](https://x.com/RepassyMate) for the elf payload injector java code
<br />

[Lazycode](https://github.com/iamLazyCode) for the idea of making a github page for this!
<br />

All the other devs who contributed to this project!
<br />

All the testers and people who helped me test this project!
<br />
<br />
<br />

## License

This project is licensed under the [Apache License 2.0](LICENSE.md).  
You are free to use, modify, and distribute this project, as long as proper credit is given to all the Developers who contributed to this.

<br />
<br />
<br />
<br />
<br />
<br />

```
██╗░░░██╗██╗██╗░░██╗████████╗░█████╗░██████╗░██╗░█████╗░██╗░░░██╗░██████╗
██║░░░██║██║██║░██╔╝╚══██╔══╝██╔══██╗██╔══██╗██║██╔══██╗██║░░░██║██╔════╝
╚██╗░██╔╝██║█████═╝░░░░██║░░░██║░░██║██████╔╝██║██║░░██║██║░░░██║╚█████╗░
░╚████╔╝░██║██╔═██╗░░░░██║░░░██║░░██║██╔══██╗██║██║░░██║██║░░░██║░╚═══██╗
░░╚██╔╝░░██║██║░╚██╗░░░██║░░░╚█████╔╝██║░░██║██║╚█████╔╝╚██████╔╝██████╔╝
░░░╚═╝░░░╚═╝╚═╝░░╚═╝░░░╚═╝░░░░╚════╝░╚═╝░░╚═╝╚═╝░╚════╝░░╚═════╝░╚═════╝░
```
