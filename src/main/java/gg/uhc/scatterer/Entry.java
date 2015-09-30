package gg.uhc.scatterer;

import com.google.common.collect.Sets;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;

public class Entry extends JavaPlugin {

    @Override
    public void onEnable() {
        FileConfiguration configuration = getConfig();
        configuration.options().copyDefaults(true);
        saveConfig();

        try {
            Set<Material> materials = getAllowedMaterials(configuration);
            int max = configuration.getInt("max attempts per player");
            ScatterStyle style = getScatterStyle(configuration);

            ScatterCommand command = new ScatterCommand(new ChunkPreparer(), style, materials, max);
            getCommand("sct").setExecutor(command);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            setEnabled(false);
        }
    }


    protected Set<Material> getAllowedMaterials(ConfigurationSection section) throws InvalidConfigurationException {
        List<String> matStrings = section.getStringList("allowed blocks");

        Set<Material> materials = Sets.newHashSetWithExpectedSize(matStrings.size());
        for (String mat : matStrings) {
            try {
                materials.add(Material.valueOf(mat));
            } catch (IllegalArgumentException e) {
                throw new InvalidConfigurationException("Invalid material name: " + mat);
            }
        }

        return materials;
    }

    protected ScatterStyle getScatterStyle(ConfigurationSection section) throws InvalidConfigurationException {
        String name = section.getString("default scatter style");
        try {
            return ScatterStyle.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigurationException("Invalid scatter style: " + name);
        }
    }
}
