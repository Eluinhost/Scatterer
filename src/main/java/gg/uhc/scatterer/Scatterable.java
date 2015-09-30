package gg.uhc.scatterer;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public abstract class Scatterable {

    protected static final String NOTICE = ChatColor.AQUA + "You were scattered to %d:%d:%d %s";

    static class PlayerScatterable extends Scatterable {

        protected final Player player;

        PlayerScatterable(Player player) {
            this.player = player;
        }

        @Override
        public void teleport(Location location) {
            player.teleport(location);
            player.sendMessage(String.format(NOTICE, location.getBlockX(), location.getBlockY(), location.getBlockZ(), "by yourself"));
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof PlayerScatterable && ((PlayerScatterable) object).player.equals(this.player);
        }

        @Override
        public int hashCode() {
            return player.hashCode();
        }
    }

    static class TeamScatterable extends Scatterable {

        protected final Team team;

        TeamScatterable(Team team) {
            this.team = team;
        }

        @Override
        public void teleport(Location location) {
            String message = String.format(NOTICE, location.getBlockX(), location.getBlockY(), location.getBlockZ(), "with team " + team.getName());

            for (OfflinePlayer player : team.getPlayers()) {
                if (player.isOnline()) {
                    player.getPlayer().teleport(location);
                    player.getPlayer().sendMessage(message);
                }
            }
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof TeamScatterable && ((TeamScatterable) object).team.equals(this.team);
        }

        @Override
        public int hashCode() {
            return team.hashCode();
        }
    }

    public static Scatterable from(Player player) {
        return new PlayerScatterable(player);
    }

    public static Scatterable from(Team team) {
        return new TeamScatterable(team);
    }


    public abstract void teleport(Location location);
}
