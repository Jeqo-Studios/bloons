package net.jeqo.bloons.utils;

import net.jeqo.bloons.Bloons;
import net.jeqo.bloons.logger.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LanguageManagement {

    /**
     * Copies all language files over from the languages directory
     */
    public static void copyLanguageFiles() {
        Bloons.getInstance().saveResource("languages/en_US.yml", false);
    }

    /**
     *                  Gets a message from the language file specified in the config
     * @param message   The message to get from the language file, type java.lang.String
     * @return          The message from the language file, type java.lang.String/null
     */
    public static String getMessage(String message) {
        File folder = new File(Bloons.getInstance().getDataFolder() + File.separator + "languages");

        // Check if the folder exists
        if (!folder.exists() || !folder.isDirectory()) {
            Logger.logWarning("Language folder not found: " + folder.getPath());
            return "";
        }

        // List files in the folder
        File[] files = folder.listFiles();
        if (files == null) {
            Logger.logWarning("No language files in folder folder: " + folder.getPath());
            return "";
        }

        // Process each file
        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();

                if (!fileName.startsWith(Bloons.getInstance().getConfig().getString("language"))) {
                    continue;
                }

                // Load the configuration file
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                // Get the configuration section
                ConfigurationSection section = config.getConfigurationSection("");

                if (section == null) {
                    Logger.logWarning("Configuration section not found for file: " + fileName);
                    continue;
                }

                // Process each key in the section
                for (String key : section.getKeys(false)) {
                    if (key.equals(message)) {
                        return config.getString(key);
                    }
                }
            }
        }

        return "";
    }
}
