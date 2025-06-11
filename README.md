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
PS! If you donate do not mention jailbreaking or hacking or anything associated with that!
<br />
Thanks!
<br />
<br />
<br /> 

## Tutorial: 
<br />
Burn the latest iso on your BD disk using imgburn (or any other preferred software), run the disk on your ps5 and select any pipeline (i reccomend 1. pipeline), Once the process is finished you will be on the home menu and etahen will be automatically loaded as soon as you are on the home screen. Enjoy! (if etahen toolbox doesnt appear re-inject it through itemzflow!) 
<br />
<br />
Modded Warfare Vid: https://www.youtube.com/watch?v=r6BAxgGJxcE 
<br />
<br />
If you have a BD-r and you still want to edit files on the iso in the future here is a tutorial by nas7536:
<br />
It's built into Windows
Double click the disc in Explorer, select "like a USB flash drive", copy the files from the ISO (do not copy the ISO itself), then right click on the disc -> Eject
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

***

<br />
***

As always thank you [EchoStretch](https://github.com/Echostretch) and [LM](https://github.com/LightningMods) for kstuff and Etahen! Also thanks [hammer 83](https://github.com/hammer-83) for jar loader, [iakdev](https://github.com/iakdev) for usb loader and pipeline loader! thanks to [cryonumb](https://github.com/cryonumb) for bdj elf loader! Thanks [John Törnblom](https://github.com/john-tornblom) for ftpsrv and websrv!
 Hope this helps and have fun! Thanks for all the people who made this possible! Also thanks [driverick](https://github.com/DriveRick) (original modded iso creator) and [54f3ty (Elon Musk)](https://github.com/54f3ty) for helping me make this!
<br />
<br />
Also a huge thanks to [BenNox_XD](https://github.com/BenNoxXD) for helping me make offline work and help with the biggest issue so far: etahen causing crashes in bdj, He is a genius for what he made and without him I wouldnt have this working perfectly. Big thanks!
<br />
<br />
<br />
__________________________________________________________________________________________________________________________________________________________________________________
<br />
Pipelines are a sequence of payloads run so it will run the payloads all in one sequence so you dont have to do it yourself, its easier and faster and automated that way.
<br />
theres 4 pipelines in total, I will divide it into 3 categories: ALl-IN-ONE, Jailbreak-ONLY, NO-etaHEN
<br />
All the pipelines have 2 versions of them, umtx1 and umtx2 (umtx2 was removed because on bdj its useless, its mostly slower and causes more kp than umtx1. It is not gonna be added back until umtx2 becomes more stable and faster.) . I highly reccomend umtx1, b̶u̶t̶ i̶f̶ y̶o̶u̶ d̶o̶n̶t̶ c̶a̶r̶e̶ a̶b̶o̶u̶t̶ a̶ h̶i̶g̶h̶e̶r̶ c̶h̶a̶n̶c̶e̶ o̶f̶ k̶p̶ a̶n̶d̶ w̶a̶n̶t̶ f̶a̶s̶t̶e̶r̶ t̶i̶m̶e̶s̶ u̶s̶e̶ t̶h̶e̶ u̶m̶t̶x̶2̶ v̶e̶r̶s̶i̶o̶n̶s̶.

__________________________________________________________________________________________________________________________________________________________________________________
All-In-One pipelines:
<br />
These pipelines have everything you would want
What it runs: UMTX - ELF LOADER- WEB SERVER - ETAHEN (kstuff included) 

__________________________________________________________________________________________________________________________________________________________________________________
Jailbreak-ONLY pipelines
<br />
These pipelines are more simplified and has the minimal you need as fast as possible
What it runs: UMTX - ELF LOADER - ETAHEN (kstuff included) 

__________________________________________________________________________________________________________________________________________________________________________________
NO-etaHEN pipelines
<br />
Notice! This is no longer needed since BenNox_XD fixed etahen issue with etahen crashing bdj, so the no etahen pipelines can be still used for testing purpuses
<br />
These pipelines are the same versions of the ones before, but without the etaHEN in the end, so theres no need for timing closing disk player so you wouldnt get kp.
What it runs: Everything until etahen.
w̶i̶t̶h̶ ̶t̶h̶i̶s̶ ̶y̶o̶u̶ ̶c̶a̶n̶ ̶l̶o̶a̶d̶ ̶e̶t̶a̶h̶e̶n̶ ̶e̶i̶t̶h̶e̶r̶ ̶v̶i̶a̶ ̶i̶n̶j̶e̶c̶t̶i̶n̶g̶ ̶i̶t̶ ̶o̶r̶ ̶u̶s̶i̶n̶g̶ ̶t̶h̶i̶s̶ ̶w̶e̶b̶s̶r̶v̶ ̶a̶p̶p̶:̶ ̶h̶t̶t̶p̶s̶:̶/̶/̶g̶i̶t̶h̶u̶b̶.̶c̶o̶m̶/̶c̶y̶3̶3̶h̶c̶/̶p̶s̶5̶-̶p̶a̶y̶l̶o̶a̶d̶-̶l̶o̶a̶d̶e̶r̶ ̶
̶a̶n̶d̶ ̶s̶i̶n̶c̶e̶ ̶w̶e̶b̶s̶r̶v̶ ̶i̶s̶ ̶i̶n̶c̶l̶u̶d̶e̶d̶ ̶i̶n̶ ̶t̶h̶e̶ ̶A̶L̶L̶-̶I̶N̶-̶O̶N̶E̶ ̶p̶i̶p̶e̶l̶i̶n̶e̶,̶ ̶i̶f̶ ̶y̶o̶u̶ ̶w̶a̶n̶n̶a̶ ̶d̶o̶ ̶t̶h̶i̶s̶ ̶m̶e̶t̶h̶o̶d̶ ̶u̶s̶e̶ ̶t̶h̶e̶ ̶A̶L̶L̶-̶I̶N̶-̶O̶N̶E̶-̶N̶O̶e̶t̶a̶H̶E̶N̶ ̶p̶i̶p̶e̶l̶i̶n̶e̶!̶

__________________________________________________________________________________________________________________________________________________________________________________
<br />
This iso includes 4 options: pipeline loader, jar loader, usb elf loader and remote jar loader
<br />
jar files on the iso:
<br />
umtx1.jar (modified umtx1 jar by me to be significantly faster)
<br />
umtx2.jar
<br />

 elfloader.jar (modified elf loader jar by me that has updated elfloader.elf)
<br />
 etahen.jar (modified etahen workaround jar by [BenNox_XD](https://github.com/BenNoxXD) that would fix etahen on bdj crashing) 
<br />
USB_etahen.jar (same as etahen.jar but loads etahen from USB instead of disk, usb should be in the front usb-A slot on your ps5 and the etaHEN file should be on your root and be called etaHEN.bin, this is for people who wanna have updated versions of etahen on their USB instead of reburning a new iso.
<br />
websrv.jar (modified websrv jar by [BenNox_XD](https://github.com/BenNoxXD) that would make websrv a little more stable)
<br />
ftpsrv_fortesting.jar (included in etahen)
<br />
CloseDisk.jar (by BenNox_XD) Closes disk player.


__________________________________________________________________________________________________________________________________________________________________________________

Videos and articles about my ISO:
<br />
*
<br />
English:
<br />
[Modded Warfare](https://www.youtube.com/@MODDEDWARFARE): [video tutorial](https://www.youtube.com/watch?v=r6BAxgGJxcE), [video overview
<br />](https://youtu.be/7MhVjhboDkU?t=766) Warning! This video does contain an older version of the iso
[Goldengames](https://www.youtube.com/@goldengames7890): [video](https://www.youtube.com/watch?v=D8-2zQ6Q-6o)
<br />
[Dravszoo](https://www.youtube.com/@dravszoo): [video](https://www.youtube.com/watch?v=a7BSuP_oIEo)
<br />
*
<br />
Spanish:
<br />
[TheWizWiki](https://www.youtube.com/@TheWizWiki): [video](https://www.youtube.com/watch?v=_ONNqFJh66w)
<br />
*
<br />
Portuguese:
<br />
[Explosão do Game](https://www.youtube.com/@Explosaodogame): [video](https://www.youtube.com/watch?v=QO_XatW6CF0) (v18)
<br />
*
<br />
Italian:
<br />
[BiteYourConsole](https://x.com/BiteYourConsole): [article](https://www.biteyourconsole.net/2025/05/02/scena-ps5-rilasciato-ps5-bdjb-modified-iso-v1-10-nuova-iso-modificata-per-i-firmware-6-xx-e-7-xx-della-console-playstation-5/?fsp_sid=24213)
<br />
[GamesAndConsoles](https://x.com/GAMESANDCON): [article](https://www.gamesandconsoles.net/ps5-rilasciato-ps5-bdjb-modified-iso-v1-11-jailbreak-offline-senza-connessione-internet/)
***
<br />
Thanks to Lazycode for the idea of making a github page for this!
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
