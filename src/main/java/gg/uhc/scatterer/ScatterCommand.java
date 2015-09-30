package gg.uhc.scatterer;

import com.google.common.base.Preconditions;
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
import gg.uhc.scatterer.conversion.*;
import gg.uhc.scatterer.conversion.selection.SelectionPredicate;
import joptsimple.*;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Set;

public class ScatterCommand implements CommandExecutor {

    protected static final String STARTING_SCATTER = ChatColor.GOLD + "Starting scatter of %d players/teams";
    protected static final String HELP_HEADER = "" + ChatColor.BOLD + ChatColor.GOLD + "Scatter Parameters. Bold = required.";

    protected final Set<Material> materials;
    protected final ChunkPreparer chunkPreparer;

    protected final OptionParser parser;
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

    public ScatterCommand(ChunkPreparer chunkPreparer, ScatterStyle defaultLogic, Set<Material> materials, int defaultMaxAttempts) {
        this.chunkPreparer = chunkPreparer;
        this.materials = materials;

        parser = new OptionParser();

        helpSpec = parser
                .acceptsAll(ImmutableList.of("?", "h", "help", "wtf"), "Help")
                .forHelp();

        useTeamsSpec = parser
                .acceptsAll(ImmutableSet.of("t", "teams"), "Scatter as teams");

        worldSpawnSpec = parser
                .acceptsAll(ImmutableSet.of("w", "world"), "World to scatter into")
                .withRequiredArg()
                .required()
                .withValuesConvertedBy(new WorldConverter());

        centreSpec = parser
                .acceptsAll(ImmutableSet.of("c", "centre"), "Coords of the centre of the scatter. If not provided uses world spawn location")
                .withRequiredArg()
                .withValuesSeparatedBy(':')
                .withValuesConvertedBy(new DoubleConverter(SelectionPredicate.ANY_DOUBLE));

        minRadiusSpec = parser
                .acceptsAll(ImmutableSet.of("m", "min", "minradius"), "Minimum radius between players/teams after scatter, default 0")
                .withRequiredArg()
                .withValuesConvertedBy(new DoubleConverter(SelectionPredicate.POSITIVE_DOUBLE_INC_ZERO))
                .defaultsTo(0D);

        radiusSpec = parser
                .acceptsAll(ImmutableSet.of("r", "radius"), "Radius around the centre to scatter")
                .withRequiredArg()
                .required()
                .withValuesConvertedBy(new DoubleConverter(SelectionPredicate.POSITIVE_DOUBLE_INC_ZERO));

        maxAttemptsSpec = parser
                .acceptsAll(ImmutableSet.of("max", "maxAttempts"), "Maximum attempts to find a location per player, default: " + defaultMaxAttempts)
                .withRequiredArg()
                .withValuesConvertedBy(new IntegerConverter(SelectionPredicate.POSITIVE_INTEGER))
                .defaultsTo(defaultMaxAttempts);

        logicSpec = parser
                .acceptsAll(ImmutableSet.of("s", "style"), "Style of scatter to use, default: " + defaultLogic.name())
                .withRequiredArg()
                .withValuesConvertedBy(new ScatterStyleConverter())
                .defaultsTo(defaultLogic);

        avoidSpawnSpec = parser
                .acceptsAll(ImmutableSet.of("spawn", "avoidSpawn"), "Avoid a radius around spawn")
                .withRequiredArg()
                .withValuesConvertedBy(new DoubleConverter(SelectionPredicate.POSITIVE_DOUBLE));

        reattemptsSpec = parser
                .acceptsAll(ImmutableSet.of("reattempts"), "How many times to rerun before giving up, default 1")
                .withRequiredArg()
                .withValuesConvertedBy(new IntegerConverter(SelectionPredicate.POSITIVE_INTEGER))
                .defaultsTo(1);

        anyMaterialSpec = parser
                .acceptsAll(ImmutableSet.of("a", "allowAllBlocks"), "Allows all blocks to be spawned on, ignores config");

        playersSpec = parser.nonOptions("Player/s to scatter, empty = scatter all online")
                .withValuesConvertedBy(new OnlinePlayerConverter());

        silentSpec = parser
                .acceptsAll(ImmutableSet.of("silent"), "Doesn't broadcast scatter to entire server");
    }

    public void onHelp(CommandSender sender) {
        sender.sendMessage(HELP_HEADER);

        Set<OptionSpec<?>> specs = Sets.newHashSet(parser.recognizedOptions().values());

        StringBuilder builder = new StringBuilder();
        for (OptionSpec spec : specs) {
            OptionDescriptor desc = (OptionDescriptor) spec;

            builder.setLength(0);
            builder.append(ChatColor.LIGHT_PURPLE);

            if (desc.isRequired()) builder.append(ChatColor.BOLD);

            if (desc.representsNonOptions()) {
                builder.append("Other arguments");
            } else {
                builder.append(desc.options());
            }

            builder.append(ChatColor.RESET);
            builder.append(ChatColor.GRAY);

            builder.append(" - ");

            if (desc.acceptsArguments()) {
                builder.append("Arg: ");
                builder.append(desc.argumentTypeIndicator());
                builder.append(" - ");
            }

            builder.append(desc.description());

            sender.sendMessage(builder.toString());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        try {
            OptionSet options = parser.parse(args);

            if (options.has(helpSpec)) {
                onHelp(sender);
                return true;
            }

            StandardScatterLogic logic = logicSpec.value(options).provide();

            World world = worldSpawnSpec.value(options);
            Location centre;

            // use world spawn if centre coords are not provided
            if (options.has(centreSpec)) {
                List<Double> coords = centreSpec.values(options);

                if (coords.size() != 2) {
                    throw new InvalidCoordinatesException(centreSpec.options());
                }

                centre = new Location(world, coords.get(0), 0, coords.get(1));
            } else {
                centre = world.getSpawnLocation();
            }

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

                    if (!options.has(silentSpec)) {
                        Bukkit.broadcastMessage(String.format(STARTING_SCATTER, scatter.size()));
                    } else {
                        sender.sendMessage(String.format(STARTING_SCATTER, scatter.size()));
                    }

                    startTeleporting(locations, Lists.newArrayList(scatter));
                    return true;
                } catch (ScatterLocationException e) {
                    sender.sendMessage(ChatColor.RED + "Failed to find locations for all players, attempt #" + (i + 1));
                }
            }

            sender.sendMessage(ChatColor.RED + "Hit max attempts to scatter. Try a larger radius (-r), smaller minimum distance (-min) or allowing more blocks (-a).");
            return true;
        } catch (OptionException ex) {
            String message;

            if (ex.getCause() != null) {
                message = ex.getCause().getMessage();
            } else {
                message = ex.getMessage();
            }

            sender.sendMessage(ChatColor.RED + message + ". Type /sct -? for help");
            return true;
        }
    }

    protected void startTeleporting(List<Location> locations, List<Scatterable> scatterables) {
        Preconditions.checkArgument(locations.size() == scatterables.size());

        chunkPreparer.stopChunkUnload(true);
        chunkPreparer.prepareLocations(locations);

        for (int i = 0; i < locations.size(); i++) {
            scatterables.get(i).teleport(locations.get(i).add(0, 2, 0));
        }

        chunkPreparer.stopChunkUnload(false);
    }
}
