Scatterer
=========

A plugin with a single commandto help with scattering players in a world

# /sct

Scatter command, typing `/sct -?` ingame will show the help.

Any non-parameter arguments are taken as player names to be scattered. If no player names are provided all players online
are scattered.

Example: `/sct -at -w UHC -r 1200 -c 0:0`

### Required parameters

`w OR world` - the name of the world to scatter into

`r OR radius` - the radius the scatter should cover

### Optional parameters

`max OR maxAttempts` - maximum times to find a location for a player, default is set in configuration file

`avoidSpawn OR spawn` - radius around spawn to avoid, if not provided does not avoid spawn area

`s OR style` - style of scatter CIRCULAR or SQUARE, default is set in the config file

`reattempts` - number of times to run the command, default 1

`c OR centre` - coordinates of the centre of the scatter `x:z`, defaults to the world spawn

`m OR min OR minradius` - minimum radius to leave between players during a scatter, default is set in the config file

`ticks OR ticksPer` - amount of ticks between sets of scatters, default is set in the config file

`p OR per OR perTeleport` - how many teams/players to teleport per set, default is set in the config file

### Flags

`silent` - Doesn't broadcast the scatter to the whole server

`? OR h OR help OR wtf` - show help

`t OR teams` - scatter players as teams (non-teamed players are teleported solo)

`a OR allowAllBlocks` - Skips the allowed blocks, players can be scattered on to any kind of block

# Configuration

```yaml
default scatter style: SQUARE
default max attempts per player: 250
default teleports per set: 5
default ticks between sets: 20
default minimum radius: 0
allowed blocks:
- DIRT
- LEAVES
- LEAVES_2
- SAND
- STONE
- GRAVEL
- SNOW
- SNOW_BLOCK
- ICE
- GRASS
- LOG
- LOG_2
- DOUBLE_PLANT
- DEAD_BUSH
- CROPS
- LONG_GRASS
- SANDSTONE
- VINE
- WATER_LILY
- WHEAT
- STAINED_CLAY
```

`default scatter style` - SQUARE or CIRCULAR

`default max attempts per player` - at least 1, attempts to get a location per player

`default teleports per set` - at least 1, how many players/teams to teleport per scatter set

`default ticks between sets` - at least 1, how many ticks between scatter sets

`default minimum radius` - at least 0, how much distance to leave between players

`allowed blocks` - list of Bukkit Material names of blocks allowed to be spawned on