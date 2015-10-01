package gg.uhc.scatterer.teleportation;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Collection;

public class ChunkPreparer {

    protected static final int DISTANCE = Bukkit.getViewDistance();

    protected boolean stoppingChunkUnload = false;

    public void stopChunkUnload(boolean stop) {
        this.stoppingChunkUnload = stop;
    }

    public void prepareLocations(Collection<Location> locations) {
        Chunk chunk;
        World world;
        int startX, endX, startZ, endZ, x, z;
        for (Location location : locations) {
            chunk = location.getChunk();
            world = chunk.getWorld();

            startX = chunk.getX() - DISTANCE;
            endX = chunk.getX() + DISTANCE;
            startZ = chunk.getZ() - DISTANCE;
            endZ = chunk.getZ() + DISTANCE;

            for (x = startX; x <= endX; x++) {
                for (z = startZ; z < endZ; z++) {
                    world.loadChunk(x, z, true);
                }
            }
        }
    }

    @EventHandler
    public void on(ChunkUnloadEvent event) {
        if (!stoppingChunkUnload) return;

        event.setCancelled(true);
    }
}
