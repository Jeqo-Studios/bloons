package net.jeqo.bloons.events.balloon;

import lombok.Getter;
import lombok.Setter;
import net.jeqo.bloons.balloon.SingleBalloon;
import net.jeqo.bloons.events.BloonsEvent;
import org.bukkit.entity.Player;

@Getter @Setter
public class SingleBalloonForceUnequipEvent extends BloonsEvent {
    private Player player;
    private SingleBalloon balloon;

    public SingleBalloonForceUnequipEvent(Player player, SingleBalloon balloon) {
        this.player = player;
        this.balloon = balloon;
    }
}
