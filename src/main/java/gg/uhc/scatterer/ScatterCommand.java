package gg.uhc.scatterer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.publicuhc.scatterlib.DefaultScatterer;
import com.publicuhc.scatterlib.Scatterer;
import com.publicuhc.scatterlib.exceptions.ScatterLocationException;
import com.publicuhc.scatterlib.logic.StandardScatterLogic;
import com.publicuhc.scatterlib.zones.CircularDeadZoneBuilder;
import com.publicuhc.scatterlib.zones.DeadZone;
import gg.uhc.flagcommands.commands.OptionCommand;
import gg.uhc.flagcommands.converters.DoubleConverter;
import gg.uhc.flagcommands.converters.IntegerConverter;
import gg.uhc.flagcommands.converters.OnlinePlayerConverter;
import gg.uhc.flagcommands.converters.WorldConverter;
import gg.uhc.flagcommands.joptsimple.OptionSet;
import gg.uhc.flagcommands.joptsimple.OptionSpec;
import gg.uhc.flagcommands.predicates.DoublePredicates;
import gg.uhc.flagcommands.predicates.IntegerPredicates;
import gg.uhc.scatterer.conversion.ScatterStyleConverter;
import gg.uhc.scatterer.teleportation.Teleporter;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Set;

public class ScatterCommand extends OptionCommand {

    protected static final String STARTING_SCATTER = ChatColor.GOLD + "Starting scatter of %d players/teams";
    protected static final String ALREADY_SCATTERING = ChatColor.RED + "There is already a scatter in progress, please wait";
    protected static final String UPDATE_MESSAGE = ChatColor.AQUA + "Scatter in progress: %d of %d players/teams complete";
    protected static final String SCATTERED = ChatColor.GOLD + "Scatter complete";
    protected static final String MUST_PROVIDE_WORLD = ChatColor.RED + "You must provide a world (-w) as you are not in a world";

    protected final Set<Material> materials;
    protected final Teleporter teleporter;

    protected final OptionSpec<Void> helpSpec;
    protected final OptionSpec<Void> useTeamsSpec;
    protected final OptionSpec<World> worldSpawnSpec;
    protected final OptionSpec<Double> centreSpec;
    protected final OptionSpec<Double> minRadiusSpec;
    protected final OptionSpec<Double> radiusSpec;
    protected final OptionSpec<Integer> maxAttemptsSpec;
    protected final OptionSpec<Double> avoidSpawnSpec;
    protected final OptionSpec<ScatterStyle> logicSpec;
    protected final OptionSpec<Integer> reattemptsSpec;
    protected final OptionSpec<Player> playersSpec;
    protected final OptionSpec<Void> anyMaterialSpec;
    protected final OptionSpec<Void> silentSpec;
    protected final OptionSpec<Integer> perTeleportSpec;
    protected final OptionSpec<Integer> ticksPerTeleport;

    public ScatterCommand(Teleporter teleporter, ScatterStyle defaultLogic, Set<Material> materials, int defaultMaxAttempts, int perTeleport, int ticksPer, double minRadius) {
        this.teleporter = teleporter;
        this.materials = materials;

        helpSpec = parser
                .acceptsAll(ImmutableList.of("?", "h", "help", "wtf"), "Help")
                .forHelp();

        useTeamsSpec = parser
                .acceptsAll(ImmutableSet.of("t", "teams"), "Scatter as teams");

        worldSpawnSpec = parser
                .acceptsAll(ImmutableSet.of("w", "world"), "World to scatter into, defaults to the world you are in when sending")
                .withRequiredArg()
                .withValuesConvertedBy(new WorldConverter());

        centreSpec = parser
                .acceptsAll(ImmutableSet.of("c", "centre"), "Coords of the centre of the scatter. If not provided uses world spawn location")
                .withRequiredArg()
                .withValuesSeparatedBy(':')
                .withValuesConvertedBy(new DoubleConverter().setType("coordinate"));

        minRadiusSpec = parser
                .acceptsAll(ImmutableSet.of("m", "min", "minradius"), "Minimum radius between players/teams after scatter, default: " + minRadius)
                .withRequiredArg()
                .withValuesConvertedBy(new DoubleConverter().setPredicate(DoublePredicates.GREATER_THAN_ZERO_INC).setType("Number >= 0"))
                .defaultsTo(minRadius);

        radiusSpec = parser
                .acceptsAll(ImmutableSet.of("r", "radius"), "Radius around the centre to scatter")
                .withRequiredArg()
                .required()
                .withValuesConvertedBy(new DoubleConverter().setPredicate(DoublePredicates.GREATER_THAN_ZERO_INC).setType("Number >= 0"));

        maxAttemptsSpec = parser
                .acceptsAll(ImmutableSet.of("max", "maxAttempts"), "Maximum attempts to find a location per player, default: " + defaultMaxAttempts)
                .withRequiredArg()
                .withValuesConvertedBy(new IntegerConverter().setPredicate(IntegerPredicates.GREATER_THAN_ZERO).setType("Integer > 0"))
                .defaultsTo(defaultMaxAttempts);

        logicSpec = parser
                .acceptsAll(ImmutableSet.of("s", "style"), "Style of scatter to use, default: " + defaultLogic.name())
                .withRequiredArg()
                .withValuesConvertedBy(new ScatterStyleConverter())
                .defaultsTo(defaultLogic);

        avoidSpawnSpec = parser
                .acceptsAll(ImmutableSet.of("spawn", "avoidSpawn"), "Avoid a radius around spawn")
                .withRequiredArg()
                .withValuesConvertedBy(new DoubleConverter().setPredicate(DoublePredicates.GREATER_THAN_ZERO).setType("Number > 0"));

        reattemptsSpec = parser
                .acceptsAll(ImmutableSet.of("reattempts"), "How many times to rerun before giving up, default 1")
                .withRequiredArg()
                .withValuesConvertedBy(new IntegerConverter().setPredicate(IntegerPredicates.GREATER_THAN_ZERO).setType("Integer > 0"))
                .defaultsTo(1);

        anyMaterialSpec = parser
                .acceptsAll(ImmutableSet.of("a", "allowAllBlocks"), "Allows all blocks to be spawned on, ignores config");

        playersSpec = parser.nonOptions("Player/s to scatter, empty = scatter all online")
                .withValuesConvertedBy(new OnlinePlayerConverter());

        silentSpec = parser
                .acceptsAll(ImmutableSet.of("silent"), "Doesn't broadcast scatter to entire server");

        perTeleportSpec = parser
                .acceptsAll(ImmutableSet.of("p", "per", "perTeleport"), "How many players/teams to teleport per teleport set, default: " + perTeleport)
                .withRequiredArg()
                .withValuesConvertedBy(new IntegerConverter().setPredicate(IntegerPredicates.GREATER_THAN_ZERO).setType("Integer > 0"))
                .defaultsTo(perTeleport);

        ticksPerTeleport = parser
                .acceptsAll(ImmutableSet.of("ticks", "ticksPer"), "Amount of ticks between sets of teleports, default: " + ticksPer)
                .withRequiredArg()
                .withValuesConvertedBy(new IntegerConverter().setPredicate(IntegerPredicates.GREATER_THAN_ZERO).setType("Integer > 0"))
                .defaultsTo(ticksPer);
    }

    @Override
    protected boolean runCommand(final CommandSender sender, OptionSet options) {
        if (teleporter.isTeleporting()) {
            sender.sendMessage(ALREADY_SCATTERING);
            return true;
        }

        // grab the required logic
        StandardScatterLogic logic = logicSpec.value(options).provide();

        // if world is provided use it otherwise attempt
        // to use the world of the sender
        World world;
        if (options.has(worldSpawnSpec)) {
            world = worldSpawnSpec.value(options);
        } else {
            if (!(sender instanceof Entity)) {
                sender.sendMessage(MUST_PROVIDE_WORLD);
                return true;
            }

            world = ((Entity) sender).getWorld();
        }

        Location centre;

        // use world spawn if centre coords are not provided
        if (options.has(centreSpec)) {
            List<Double> coords = centreSpec.values(options);

            if (coords.size() != 2) {
                throw new InvalidCoordinatesException(centreSpec.options());
            }

            // use y = 0 as placeholder
            centre = new Location(world, coords.get(0), 0, coords.get(1));
        } else {
            centre = world.getSpawnLocation();
        }

        // setup the logic with our values
        logic.setCentre(centre);
        logic.setMaxAttempts(maxAttemptsSpec.value(options));
        logic.setRadius(radiusSpec.value(options));

        // only set the materials if any materials wasn't set
        // scatterer uses any material if none provided
        if (!options.has(anyMaterialSpec)) {
            logic.setMaterials(materials);
        }

        List<Player> toScatter = playersSpec.values(options);

        // if none provided scatter all players online
        if (toScatter.size() == 0) {
            toScatter = ImmutableList.copyOf(Bukkit.getOnlinePlayers());
        }

        CircularDeadZoneBuilder aroundPlayers = new CircularDeadZoneBuilder(minRadiusSpec.value(options));

        List<DeadZone> initial = Lists.newArrayList();

        // build an area around the spawn if required
        if (options.has(avoidSpawnSpec)) {
            initial.add(new CircularDeadZoneBuilder(avoidSpawnSpec.value(options)).buildForLocation(centre));
        }

        Scatterer scatterer = new DefaultScatterer(logic, initial, aroundPlayers);

        // add a dead zone for every player not being scattered
        scatterer.addDeadZonesForPlayersNotInList(toScatter);

        int reattempts = reattemptsSpec.value(options);

        for (int i = 0; i < reattempts; i++) {
            try {
                Set<Scatterable> scatter = Sets.newHashSet();

                if (options.has(useTeamsSpec)) {
                    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

                    for (Player player : toScatter) {
                        Team team = scoreboard.getPlayerTeam(player);

                        // add as a solo if no team set
                        scatter.add(team == null ? Scatterable.from(player) : Scatterable.from(team));
                    }
                } else {
                    for (Player player : toScatter) {
                        scatter.add(Scatterable.from(player));
                    }
                }

                List<Location> locations = scatterer.getScatterLocations(scatter.size());

                final boolean silent = options.has(silentSpec);

                if (!silent) {
                    Bukkit.broadcastMessage(String.format(STARTING_SCATTER, scatter.size()));
                } else {
                    sender.sendMessage(String.format(STARTING_SCATTER, scatter.size()));
                }

                teleporter.teleport(locations, Lists.newArrayList(scatter), perTeleportSpec.value(options), ticksPerTeleport.value(options), new Teleporter.Callback() {
                    @Override
                    public void onUpdate(int completed, int total) {
                        if (silent) {
                            sender.sendMessage(String.format(UPDATE_MESSAGE, completed, total));
                        } else {
                            Bukkit.broadcastMessage(String.format(UPDATE_MESSAGE, completed, total));
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (silent) {
                            sender.sendMessage(SCATTERED);
                        } else {
                            Bukkit.broadcastMessage(SCATTERED);
                        }
                    }
                });

                return true;
            } catch (ScatterLocationException e) {
                sender.sendMessage(ChatColor.RED + "Failed to find locations for all players, attempt #" + (i + 1));
            }
        }

        sender.sendMessage(ChatColor.RED + "Hit max attempts to scatter. Try a larger radius (-r), smaller minimum distance (-min) or allowing more blocks (-a).");
        return true;
    }
}
