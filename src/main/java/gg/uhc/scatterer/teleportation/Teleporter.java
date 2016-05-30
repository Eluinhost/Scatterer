package gg.uhc.scatterer.teleportation;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import gg.uhc.scatterer.Scatterable;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.List;

public class Teleporter {

    public interface Callback {
        void onUpdate(int completed, int total);
        void onComplete();
    }

    protected Optional<Callback> currentCallback = Optional.absent();
    protected Optional<BukkitRunnable> teleportTask = Optional.absent();

    protected LinkedList<List<Location>> locations;
    protected LinkedList<List<Scatterable>> scatterables;

    // keep track of how many are done compared to total
    protected int completed = 0;
    protected int total = 0;

    protected final Plugin plugin;
    protected final ChunkPreparer chunkPreparer;

    public Teleporter(ChunkPreparer chunkPreparer, Plugin plugin) {
        this.chunkPreparer = chunkPreparer;
        this.plugin = plugin;
    }

    public boolean isTeleporting() {
        return teleportTask.isPresent();
    }

    public void teleport(List<Location> locations, List<Scatterable> scatterables, int chunkSize, int ticksPer, Callback callback) {
        Preconditions.checkArgument(locations.size() == scatterables.size());
        Preconditions.checkArgument(locations.size() > 0);
        Preconditions.checkArgument(chunkSize > 0);
        Preconditions.checkArgument(ticksPer > 0);
        Preconditions.checkNotNull(callback);

        currentCallback = Optional.of(callback);
        this.total = locations.size();

        this.locations = Lists.newLinkedList(Lists.partition(locations, chunkSize));
        this.scatterables = Lists.newLinkedList(Lists.partition(scatterables, chunkSize));

        // stop chunk unloading during scatter
        // is turned off on cancel
        chunkPreparer.stopChunkUnload(true);

        // start timer
        BukkitRunnable task = new TeleportTask();
        teleportTask = Optional.of(task);
        task.runTaskTimer(plugin, 0, ticksPer);
    }

    public void cancelTeleport() {
        if (!isTeleporting()) return;

        teleportTask.get().cancel();
        currentCallback.get().onComplete();

        teleportTask = Optional.absent();
        currentCallback = Optional.absent();
        locations = null;
        scatterables = null;
        completed = 0;
        total = 0;

        // make sure to allow chunk unloading again
        chunkPreparer.stopChunkUnload(false);
    }

    class TeleportTask extends BukkitRunnable {
        @Override
        public void run() {
            List<Location> loc = locations.pop();
            List<Scatterable> scatter = scatterables.pop();

            // load all of the chunks we are going to teleport players into
            chunkPreparer.prepareLocations(loc);

            // teleport each
            for (int i = 0; i < loc.size(); i++) {
                scatter.get(i).teleport(loc.get(i).add(0, 2, 0));
            }

            completed += loc.size();

            currentCallback.get().onUpdate(completed, total);

            // if we've ran out then cancel ourselves and cleanup
            if (locations.size() == 0) {
                cancelTeleport();
            }
        }
    }
}
