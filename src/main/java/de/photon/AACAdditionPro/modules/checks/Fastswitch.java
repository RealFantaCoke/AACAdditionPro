package de.photon.AACAdditionPro.modules.checks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PacketListenerModule;
import de.photon.AACAdditionPro.modules.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.server.ServerUtil;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;

public class Fastswitch extends PacketAdapter implements PacketListenerModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 120);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;

    @LoadFromConfiguration(configPath = "max_ping")
    private double max_ping;

    @LoadFromConfiguration(configPath = "switch_milliseconds")
    private int switch_milliseconds;

    public Fastswitch()
    {
        super(AACAdditionPro.getInstance(), PacketType.Play.Client.HELD_ITEM_SLOT);
    }

    @Override
    public void onPacketReceiving(final PacketEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Tps are high enough
        if (ServerUtil.getTPS() > 19 &&
            event.getPacket().getBytes().readSafely(0) != null &&
            // Prevent the detection of scrolling
            !canBeLegit(user.getPlayer().getInventory().getHeldItemSlot(), event.getPacket().getBytes().readSafely(0)))
        {
            // Already switched in the given timeframe
            if (user.getFastSwitchData().recentlyUpdated(0, switch_milliseconds)) {

                // The ping is valid and in the borders that are set in the config
                if (max_ping < 0 || ServerUtil.getPing(user.getPlayer()) < max_ping) {
                    vlManager.flag(user.getPlayer(),
                                   cancel_vl,
                                   () -> event.setCancelled(true),
                                   () -> InventoryUtils.syncUpdateInventory(user.getPlayer()));
                }
            }

            user.getFastSwitchData().updateTimeStamp(0);
        }
    }

    /**
     * Used to acknowledge if somebody can be legit.
     * I.e. that players can scroll very fast, but then the neighbor slot is always the one that gets called next.
     */
    private static boolean canBeLegit(final int oldSlot, final int newHeldItemSlot)
    {
        return (oldSlot == 0 && newHeldItemSlot == 8) ||
               (oldSlot == 8 && newHeldItemSlot == 0) ||
               MathUtils.roughlyEquals(oldSlot, newHeldItemSlot, 1);
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.FASTSWITCH;
    }
}
