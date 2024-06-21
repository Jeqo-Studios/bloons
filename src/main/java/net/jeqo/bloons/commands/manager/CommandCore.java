package net.jeqo.bloons.commands.manager;

import lombok.Getter;
import net.jeqo.bloons.Bloons;
import net.jeqo.bloons.balloon.single.SingleBalloonType;
import net.jeqo.bloons.commands.*;
import net.jeqo.bloons.commands.manager.types.CommandAccess;
import net.jeqo.bloons.configuration.PluginConfiguration;
import net.jeqo.bloons.gui.menus.BalloonMenu;
import net.jeqo.bloons.logger.Logger;
import net.jeqo.bloons.utils.ColorManagement;
import net.jeqo.bloons.utils.LanguageManagement;
import net.jeqo.bloons.utils.MessageTranslations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static net.jeqo.bloons.commands.utils.ErrorHandling.usage;

@Getter
public class CommandCore implements CommandExecutor {
    private final ArrayList<Command> commands;
    private final JavaPlugin plugin;
    private final MessageTranslations messageTranslations;

    /**
     *                          Creates a new instance of the command core
     * @param providedPlugin    The plugin instance, type org.bukkit.plugin.java.JavaPlugin
     */
    public CommandCore(JavaPlugin providedPlugin) {
        this.plugin = providedPlugin;
        this.commands = new ArrayList<>();
        this.messageTranslations = new MessageTranslations(this.getPlugin());

        // Add any commands you want registered here
        addCommand(new CommandEquip(this.getPlugin()));
        addCommand(new CommandForceEquip(this.getPlugin()));
        addCommand(new CommandForceUnequip(this.getPlugin()));
        addCommand(new CommandReload(this.getPlugin()));
        addCommand(new CommandUnequip(this.getPlugin()));

        // Register all commands staged
        registerCommands();

        Objects.requireNonNull(this.getPlugin().getCommand(PluginConfiguration.COMMAND_BASE)).setTabCompleter(new CommandTabCompleter());
    }

    /**
     * Registers all commands in the commands list
     */
    public void registerCommands() {
        Objects.requireNonNull(this.getPlugin().getCommand(PluginConfiguration.COMMAND_BASE)).setExecutor(this);
    }

    /**
     *                      Gets a commands description by its alias
     * @param commandAlias  The alias of the command, type java.lang.String
     * @return              The description of the command, type java.lang.String
     */
    public String getCommandDescription(String commandAlias) {
        for (Command command : this.getCommands()) {
            if (command.getCommandAliases().contains(commandAlias)) {
                return command.getCommandDescription();
            }
        }
        return null;
    }

    /**
     *                  Adds a command to the commands list
     * @param command   The command to add, type net.jeqo.bloons.commands.manager.Command
     */
    public void addCommand(Command command) {
        this.getCommands().add(command);
    }

    /**
     *                  Executes the command
     * @param sender    Source of the command, type org.bukkit.command.CommandSender
     * @param command   Command which was executed, type org.bukkit.command.Command
     * @param label     Alias of the command which was used, type java.lang.String
     * @param args      Passed command arguments, type java.lang.String[]
     * @return          Whether the command was executed successfully, type boolean
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1) {
            Player player = (Player) sender;

            if (!player.hasPermission("bloons.menu")) {
                Component noPermission = this.getMessageTranslations().getSerializedString(this.getMessageTranslations().getMessage("prefix"), LanguageManagement.getMessage("no-permission"));
                player.sendMessage(noPermission);
                return true;
            }

            ArrayList<ItemStack> items = new ArrayList<>();
            ArrayList<SingleBalloonType> singleBalloonTypes = Bloons.getBalloonCore().getSingleBalloonTypes();

            if (singleBalloonTypes == null) {
                Logger.logError(LanguageManagement.getMessage("no-balloons-registered"));

                return false;
            }

            // For every single balloon registered, add it to the GUI
            for (SingleBalloonType singleBalloon : singleBalloonTypes) {
                if (singleBalloon == null) continue;

                if (shouldAddSingleBalloon(player, singleBalloon)) {
                    ItemStack item = createBalloonItem(singleBalloon);
                    items.add(item);
                }
            }

            // Formulate a menu with the items
            new BalloonMenu(items, this.getMessageTranslations().getString("menu-title"), player);
            return true;
        }


        // Define what a subcommand really is
        String subcommand = args[0].toLowerCase();
        String[] subcommandArgs = Arrays.copyOfRange(args, 1, args.length);

        // Loop over every command registered, check the permission, and execute the command
        for (Command currentCommand : getCommands()) {
            if (currentCommand.getCommandAliases().contains(subcommand)) {
                // Check if the sender has the permission to execute the command
                if (!meetsRequirements(currentCommand, sender)) {
                    sender.sendMessage(this.getMessageTranslations().getSerializedString(this.getMessageTranslations().getMessage("prefix"), LanguageManagement.getMessage("no-permission")));
                    return false;
                }

                // Check if the command is disabled
                if (currentCommand.getRequiredAccess() == CommandAccess.DISABLED) {
                    sender.sendMessage(this.getMessageTranslations().getSerializedString(this.getMessageTranslations().getMessage("prefix"), LanguageManagement.getMessage("command-disabled")));
                    return false;
                }

                // Execute the command
                try {
                    currentCommand.execute(sender, subcommandArgs);
                } catch (Exception ignored) {
                }
                return true;
            }
        }

        // If the command isn't here, show the usage menu
        usage(sender);
        return false;
    }

    /**
     *                  Checks if the player sending the command meets the requirements to execute the command
     * @param command   The command to check, type net.jeqo.bloons.commands.manager.Command
     * @param sender    The sender of the command, type org.bukkit.command.CommandSender
     * @return          Whether the user meets the requirements, type boolean
     */
    public boolean meetsRequirements(Command command, CommandSender sender) {
        return command.hasRequirement(sender, command.getRequiredPermission());
    }

    /**
     *                              Checks if we should add the balloon to the menu
     * @param player                The player to check, type org.bukkit.entity.Player
     * @param singleBalloonType     The key of the balloon, type java.lang.String
     * @return                      Whether we should add the balloon to the menu, type boolean
     */
    private boolean shouldAddSingleBalloon(Player player, SingleBalloonType singleBalloonType) {
        if (this.getMessageTranslations().getString("hide-balloons-without-permission").equalsIgnoreCase("true")) {
            return player.hasPermission(singleBalloonType.getPermission());
        }
        return true;
    }

    /**
     *                              Creates an ItemStack for a balloon
     * @param singleBalloonType     The instance of the object which contains the balloon's configuration, type net.jeqo.bloons.balloon.single.SingleBalloonType
     * @return                      The ItemStack of the balloon, type org.bukkit.inventory.ItemStack
     */
    private ItemStack createBalloonItem(SingleBalloonType singleBalloonType) {
        Material material = Material.matchMaterial(singleBalloonType.getMaterial());
        if (material == null) {
            Logger.logError(String.format(LanguageManagement.getMessage("material-not-valid"), singleBalloonType.getMaterial()));
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            Logger.logError(String.format(LanguageManagement.getMessage("invalid-item-meta"), singleBalloonType.getMaterial()));
            return null;
        }

        meta.setLocalizedName(singleBalloonType.getKey());
        setBalloonLore(meta, singleBalloonType);
        setBalloonDisplayName(meta, singleBalloonType);
        meta.setCustomModelData(singleBalloonType.getCustomModelData());
        setBalloonColor(meta, singleBalloonType);

        item.setItemMeta(meta);
        return item;
    }

    /**
     *                              Sets the lore of the balloon
     * @param meta                  The ItemMeta of the balloon, type org.bukkit.inventory.meta.ItemMeta
     * @param singleBalloonType     The instance of the object which contains the balloon's configuration, type net.jeqo.bloons.balloon.single.SingleBalloonType
     */
    private void setBalloonLore(ItemMeta meta, SingleBalloonType singleBalloonType) {
        if (singleBalloonType.getLore() != null) {
            List<String> lore = new ArrayList<>(List.of(singleBalloonType.getLore()));
            lore.replaceAll(ColorManagement::fromHex);
            meta.setLore(lore);
        }
    }

    /**
     *                             Sets the display name of the balloon
     * @param meta                 The ItemMeta of the balloon, type org.bukkit.inventory.meta.ItemMeta
     * @param singleBalloonType    The instance of the object which contains the balloon's configuration, type net.jeqo.bloons.balloon.single.SingleBalloonType
     */
    private void setBalloonDisplayName(ItemMeta meta, SingleBalloonType singleBalloonType) {
        String name = singleBalloonType.getName();
        MessageTranslations messageTranslations = new MessageTranslations(this.getPlugin());
        if (name != null) {
            meta.displayName(messageTranslations.getSerializedString(name));
        }
    }

    /**
     *                             Sets the color of the balloon
     * @param meta                 The ItemMeta of the balloon, type org.bukkit.inventory.meta.ItemMeta
     * @param singleBalloonType    The configuration section of the balloon, type org.bukkit.configuration.ConfigurationSection
     */
    private void setBalloonColor(ItemMeta meta, SingleBalloonType singleBalloonType) {
        String color = singleBalloonType.getColor();

        if (color != null && !color.equalsIgnoreCase("potion")) {
            if (meta instanceof LeatherArmorMeta) {
                ((LeatherArmorMeta) meta).setColor(ColorManagement.hexToColor(color));
            } else {
                Logger.logWarning(String.format(LanguageManagement.getMessage("material-not-dyeable"), singleBalloonType.getMaterial()));
            }
        }
    }
}
