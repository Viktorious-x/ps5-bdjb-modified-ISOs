# ps5-bdjb-modified-ISOs
<br />
<br />

## Auto Jailbreak and HEN ALL-IN-ONE ISO for 3.xx-7.xx

<br />
<br />
OFFLINE ISO NOW - NO NEED FOR NETWORK CONNECTION ANYMORE BABYYYY
<br />
<br />
i am on 7.61 so thats the firmware version i test on
<br />
<br />
Contact me on discord: Viktorious_x for feedback
<br />
<br />

## If you would like to support what I do, here is my donation page:
<br />

[![ko-fi](https://www.ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/viktorious)

<br />
Paypal: Ask me on discord or Twitter(X)
<br />
<br />

#### Notice! If you donate do not mention jailbreaking or hacking or anything associated with that!
<br />
Thanks!
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

## Etahen configs & other stuff
<br />
<br />
Etahen is loaded from the disc normally, but if you dont want to update iso to update etahen you can either have etaHEN.bin on your usb ROOT formatted in exfat or in your data folder on your ps5.
<br />

The way it checks where to load from is in this order: USB, /data on ps5 and then if those two dont have etaHEN.bin on there it will load it from the disc itself - Huge thanks to
 [BenNox_XD](https://github.com/BenNoxXD) for making this a thing, he is the coder behind it

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
Pipelines are a sequence of payloads run so it will run the payloads all in one sequence so you dont have to do it yourself, its easier and faster and automated that way.
<br />
theres 4 pipelines in total, I will divide it into 3 categories: ALL-IN-ONE, NormalJailbreak, NOetaHEN
<br />
<br />

__________________________________________________________________________________________________________________________________________________________________________________
### All-In-One pipelines:

<br />
These pipelines have everything you would want
<br />

#### What it runs: UMTX - ELF LOADER- WEB SERVER - ETAHEN (kstuff included) 

__________________________________________________________________________________________________________________________________________________________________________________
### Jailbreak-ONLY pipelines

<br />
These pipelines are more simplified and has the minimal you need as fast as possible
<br />

#### What it runs: UMTX - ELF LOADER - ETAHEN (kstuff included) 

__________________________________________________________________________________________________________________________________________________________________________________
### NOetaHEN pipelines

<br />

#### Notice! This is no longer needed since BenNox_XD fixed etahen issue with etahen crashing bdj, but the no etahen pipelines can be still used for other purposes, for example testing purpuses
<br />

#### These pipelines are the same versions of the ones before, but without the etaHEN in the end, so theres no need for timing closing disk player so you wouldnt get kp.
#### What it runs: Everything until etahen.
<br />

w̶i̶t̶h̶ ̶t̶h̶i̶s̶ ̶y̶o̶u̶ ̶c̶a̶n̶ ̶l̶o̶a̶d̶ ̶e̶t̶a̶h̶e̶n̶ ̶e̶i̶t̶h̶e̶r̶ ̶v̶i̶a̶ ̶i̶n̶j̶e̶c̶t̶i̶n̶g̶ ̶i̶t̶ ̶o̶r̶ ̶u̶s̶i̶n̶g̶ ̶t̶h̶i̶s̶ ̶w̶e̶b̶s̶r̶v̶ ̶a̶p̶p̶:̶ ̶h̶t̶t̶p̶s̶:̶/̶/̶g̶i̶t̶h̶u̶b̶.̶c̶o̶m̶/̶c̶y̶3̶3̶h̶c̶/̶p̶s̶5̶-̶p̶a̶y̶l̶o̶a̶d̶-̶l̶o̶a̶d̶e̶r̶ ̶
̶a̶n̶d̶ ̶s̶i̶n̶c̶e̶ ̶w̶e̶b̶s̶r̶v̶ ̶i̶s̶ ̶i̶n̶c̶l̶u̶d̶e̶d̶ ̶i̶n̶ ̶t̶h̶e̶ ̶A̶L̶L̶-̶I̶N̶-̶O̶N̶E̶ ̶p̶i̶p̶e̶l̶i̶n̶e̶,̶ ̶i̶f̶ ̶y̶o̶u̶ ̶w̶a̶n̶n̶a̶ ̶d̶o̶ ̶t̶h̶i̶s̶ ̶m̶e̶t̶h̶o̶d̶ ̶u̶s̶e̶ ̶t̶h̶e̶ ̶A̶L̶L̶-̶I̶N̶-̶O̶N̶E̶-̶N̶O̶e̶t̶a̶H̶E̶N̶ ̶p̶i̶p̶e̶l̶i̶n̶e̶!̶

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

umtx2.jar (slower and less stable than umtx1, highly dont reccomend this)
<br />

elfloader.jar ([modified elf loader jar](https://github.com/Viktorious-x/bdjb-elfloader) by [me](https://github.com/Viktorious-x) that has updated [elfloader.elf](https://github.com/ps5-payload-dev/elfldr))
<br />

etahen.jar - [BD-J Hen Loader](https://github.com/BenNoxXD/PS5-BDJ-HEN-loader) by [BenNox_XD](https://github.com/BenNoxXD) that would fix [etaHEN](https://github.com/etaHEN/etaHEN) on bdj crashing) (requires Elf Loader running)
<br />

websrv.jar - [websrv.elf](https://github.com/ps5-payload-dev/websrv/) turned into a JAR (by [John Törnblom aka SB](https://github.com/john-tornblom)) (requires Elf Loader running)
<br />

ftpsrv.jar - ftp server, included in etahen turned into a JAR (by [John Törnblom aka SB](https://github.com/john-tornblom)) (requires Elf Loader running)
<br />

airpsx.jar -  Remote management software for Jailbroken PlayStation 5 turned into a JAR (by [barisyild](https://github.com/barisyild)) (requires Elf Loader running)
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

[Modded Warfare](https://www.youtube.com/@MODDEDWARFARE): [video tutorial](https://www.youtube.com/watch?v=r6BAxgGJxcE) (v8) , [video overview](https://www.youtube.com/watch?v=6plH2ADkZi0&t=486s) (v19) 
<br />
<br />
Modded Warfare's tutorial about my iso is outdatded, but still useful!
<br />
<br />

[GameConsoleFish](https://www.youtube.com/@GameConsolefish): [video tutorial](https://www.youtube.com/watch?v=I47r8fKjTec) (v19)
<br />

[Goldengames](https://www.youtube.com/@goldengames7890): [video](https://www.youtube.com/watch?v=D8-2zQ6Q-6o) (v16)
<br />

[Dravszoo](https://www.youtube.com/@dravszoo): [video](https://www.youtube.com/watch?v=a7BSuP_oIEo) (v16)
<br />

[Mc Kuc](https://www.youtube.com/@McKuc/) in collaboration with me: [video overview + custom icons](https://www.youtube.com/watch?v=0kusnCdyYuc) (v19 BETA Version)
<br />
<br />

### Spanish:
<br />

[TheWizWiki](https://www.youtube.com/@TheWizWiki): [video](https://www.youtube.com/watch?v=_ONNqFJh66w) (v16)
<br />

[NanospeedGamer](https://www.youtube.com/@NanospeedGamer): [video](https://www.youtube.com/watch?v=SPx_GvqlDqw) (v19) 
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

[BiteYourConsole](https://x.com/BiteYourConsole): [article](https://www.biteyourconsole.net/2025/07/09/scena-ps5-rilasciato-ps5-bdjb-modified-iso-v1-19/) (v19)
<br />

[GamesAndConsoles](https://x.com/GAMESANDCON): [article](https://www.gamesandconsoles.net/ps5-ps5-bdjb-modified-isos-v1-19-v19-novita-miglioramenti-e-tutorial/) (v19)
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

[Echostretch](https://github.com/EchoStretch) and all the other Contributors for [kstuff](https://github.com/EchoStretch/kstuff) on all firmwares that support BD-JB ([ko-fi](https://ko-fi.com/echostretch/))
<br />

[LightningMods](https://github.com/LightningMods) and all the other Contributors ([here](https://github.com/etaHEN/etaHEN?tab=readme-ov-file#contributors)) for [etaHEN](https://github.com/etaHEN/etaHEN) ([ko-fi](https://ko-fi.com/lightningmods))
<br />

[iakdev](https://github.com/iakdev) for USB loader and Pipeline runner ([here](https://github.com/iakdev/ps5-jar-loader))
<br />

[John Törnblom](https://github.com/john-tornblom) (aka SB) and all the other Contributors for [websrv](https://github.com/ps5-payload-dev/websrv/) and [ftpsrv](https://github.com/ps5-payload-dev/ftpsrv)
<br />

[CryoNumb](https://github.com/cryonumb/) for [BD-J Elf Loader](https://github.com/cryonumb/elfloader)
<br />

[barisyild](https://github.com/barisyild/) for [airpsx](https://github.com/barisyild/airpsx)
<br />

[BenNox_XD](https://github.com/BenNoxXD) for [BD-J Hen Loader](https://github.com/BenNoxXD/PS5-BDJ-HEN-loader), Modified [websrv.jar](https://github.com/BenNoxXD/PS5-BDJ-HEN-loader/releases/tag/2.1b) for extra stability and help regarding this project
<br />

[DriveRick](https://github.com/DriveRick) for [the Original Modded Menu for the Ps5 Jar Loader](https://github.com/DriveRick/PS5-ToolDisc) and help regarding this project ([ko-fi](https://ko-fi.com/firlefanz))
<br />

[Viktorious (Me) ](https://github.com/Viktorious-x) and a Anonymous developer (for the help with figuring it out at first) for modified umtx1.jar that is significantly faster and more stable than the original umtx1.jar ([ko-fi](https://ko-fi.com/viktorious))
<br />

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
