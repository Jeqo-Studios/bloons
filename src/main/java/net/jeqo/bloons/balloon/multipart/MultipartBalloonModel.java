package net.jeqo.bloons.balloon.multipart;

import lombok.Getter;
import lombok.Setter;
import net.jeqo.bloons.balloon.model.BalloonModel;
import net.jeqo.bloons.balloon.model.BalloonModelType;
import net.jeqo.bloons.logger.Logger;
import net.jeqo.bloons.utils.ColorManagement;
import net.jeqo.bloons.utils.LanguageManagement;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * A class to aid in the creation of the models used in the multipart balloons
 */
@Getter @Setter
public class MultipartBalloonModel {
    private BalloonModelType modelType;
    private String material;
    private String color;
    private Integer customModelData;

    /**
     *                          Creates a new model for a multipart balloon
     * @param modelType         The type of model (head, body, tail), type net.jeqo.bloons.balloon.model.BalloonModelType
     * @param material          The name of the Bukkit Material used to create the item, type java.lang.String
     * @param color             The color of the model as a hex color code value, type java.lang.String
     * @param customModelData   The custom model data value stored in the item metadata, type int
     */
    public MultipartBalloonModel(BalloonModelType modelType, String material, String color, int customModelData) {
        this.setModelType(modelType);
        this.setMaterial(material);
        this.setColor(color);
        this.setCustomModelData(customModelData);
    }

    /**
     *              Gets the finalized item of a multipart balloon model with the specified metadata
     * @return      The finalized item model with the specified metadata, type org.bukkit.inventory.ItemStack
     */
    public ItemStack getFinalizedModel() {
        // Gets the Bukkit Material type from the string
        Material material = Material.getMaterial(this.getMaterial().toUpperCase());

        // Check if the material is valid
        if (material == null) {
            Logger.logError(String.format(LanguageManagement.getMessage("material-is-not-valid"), this.getMaterial()));
            return null;
        }

        // Check if the color is null and the custom model data is null, return only the raw ItemStack
        if (this.getColor() == null && this.getCustomModelData() == null) {
            return new ItemStack(material);

        // Check if the color is null and the custom model data is not null, return the model with the custom model data
        } else if (this.getColor() == null) {
            return BalloonModel.createBlankModel(material, this.getCustomModelData());

        // If everything provided isn't null and is valid, create the model with the specified color and custom model data
        } else if (this.getColor().startsWith("#")) {
            // Check if valid hex code via the utility in net.jeqo.bloons.utils.ColorManagement
            if (!ColorManagement.isHexCode(this.getColor())) {
                Logger.logError(String.format(LanguageManagement.getMessage("invalid-hex-code"), this.getColor()));
                return null;
            }

            // Convert the hex code to a valid org.bukkit.Color object
            Color color = ColorManagement.hexToColor(this.getColor());
            return BalloonModel.createColouredModel(material, color, this.getCustomModelData());

        // If the color is not a hex code and there is no custom model data, only return a raw org.bukkit.inventory.ItemStack
        } else if (this.getCustomModelData() == null) {
            return new ItemStack(material);
        }

        // Return null if otherwise nothing is specified
        return null;
    }
}