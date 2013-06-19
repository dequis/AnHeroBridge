# AnHeroBridge

A new Herochat5 Bridge for CraftIRC3. The config file has the sme format as
uircbridge but everything else has been rewritten from scratch for no good
reason at all. This one should have less bugs, though. Should.

Mostly a project to get used to coding in java, since I think herochat sucks.
Feel free to say my code sucks too, and provide constructive criticism.

## Features

* Basic chat
* Completely compatible with uircbridge
* Shares some of the bugs too, because herochat sucks.
* Will get angry at users that do stupid things.
* Reloads itself properly when using /ircreload for craftirc
* "Display name", prefix and suffix support.
* It works.

## Configuration

Will write something more detailed later. For now just follow the instructions here:

http://dev.bukkit.org/bukkit-plugins/uircbridge/pages/configuration-example/

But change uIRCBridge with AnHeroBridge as the root node.

## Future

The current feature set seems to be pretty much everything the herochat api lets
me do. Since it's not open source I can't send them patches to expand the API,
so the plan is to just patch it in runtime with reflection. Fun.

Also, CraftIRC4 soon(tm). Looks like I won't be able to do anything interesting
with the new api design if I don't patch herochat first.

## Etc.

MIT licensed.

Based off javadocs here: http://mythcraft.dyndns.org/javadoc/

There's a maven repo in the "deps-repo" dir because herocraft has maven repos
with everything EXCEPT herocraft related stuff. Seriously, they seem to host
projects by other people in that repo and nothing else.
