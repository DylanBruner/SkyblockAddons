package codes.biscuit.skyblockaddons.features.discordrpc;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.core.Location;
import codes.biscuit.skyblockaddons.core.SkyblockDate;
import codes.biscuit.skyblockaddons.utils.EnumUtils;
import com.google.gson.JsonObject;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

public class DiscordRPCManager implements IPCListener {

    @Getter @Setter private EnumUtils.DiscordStatusEntry currentEntry;

    private static final long APPLICATION_ID = 653443797182578707L;
    private static final long UPDATE_PERIOD = 4200L;

    private static final SkyblockAddons main = SkyblockAddons.getInstance();
    private static final Logger logger = SkyblockAddons.getLogger();

    private IPCClient client;
    private DiscordStatus detailsLine;
    private DiscordStatus stateLine;
    private long startTimestamp;

    private Timer updateTimer;
    private boolean connected;

    public void start() {
        SkyblockAddons.runAsync(() -> {
            try {
                logger.info("Starting Discord RPC...");
                if (isActive()) {
                    return;
                }

                stateLine = main.getConfigValues().getDiscordStatus();
                detailsLine = main.getConfigValues().getDiscordDetails();
                startTimestamp = System.currentTimeMillis();
                client = new IPCClient(APPLICATION_ID);
                client.setListener(this);
                try {
                    client.connect();
                } catch (Exception ex) {
                    logger.warn("Failed to connect to Discord RPC!");
                    logger.catching(ex);
                }
            } catch (Throwable ex) {
                logger.error("Discord RPC has thrown an unexpected error while trying to start...");
                logger.catching(ex);
            }
        });
    }

    public void stop() {
        SkyblockAddons.runAsync(() -> {
            if (isActive()) {
                connected = false;
                client.close();
            }
        });
    }

    public boolean isActive() {
        return client != null && connected;
    }

    public void updatePresence() {
        Location location = SkyblockAddons.getInstance().getUtils().getLocation();
        SkyblockDate skyblockDate = SkyblockAddons.getInstance().getUtils().getCurrentDate();
        String skyblockDateString = skyblockDate != null ? skyblockDate.toString() : "";

        // Early Winter 10th, 12:10am - Village
        String largeImageDescription = String.format("%s - %s", skyblockDateString, location.getScoreboardName());
        String smallImageDescription = String.format("Using SkyblockAddons v%s", SkyblockAddons.VERSION+" by Biscuit (fixes by Frost19) | Icons by Hypixel Packs HQ");
        RichPresence presence = new RichPresence.Builder()
                .setState(stateLine.getDisplayString(EnumUtils.DiscordStatusEntry.STATE))
                .setDetails(detailsLine.getDisplayString(EnumUtils.DiscordStatusEntry.DETAILS))
                .setStartTimestamp(startTimestamp)
                .setLargeImage(location.getDiscordIconKey(), largeImageDescription)
                .setSmallImage("skyblockicon", smallImageDescription)
                .build();
        client.sendRichPresence(presence);
    }

    public void setStateLine(DiscordStatus status) {
        this.stateLine = status;
        if (isActive()) {
            updatePresence();
        }
    }

    public void setDetailsLine(DiscordStatus status) {
        this.detailsLine = status;
        if (isActive()) {
            updatePresence();
        }
    }

    @Override
    public void onReady(IPCClient client) {
        logger.info("Discord RPC started.");
        connected = true;
        updateTimer = new Timer();
        updateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updatePresence();
            }
        }, 0, UPDATE_PERIOD);
    }

    @Override
    public void onClose(IPCClient client, JsonObject json) {
        logger.info("Discord RPC closed.");
        this.client = null;
        connected = false;
        cancelTimer();
    }

    @Override
    public void onDisconnect(IPCClient client, Throwable t) {
        logger.warn("Discord RPC disconnected.");
        this.client = null;
        connected = false;
        cancelTimer();
    }

    private void cancelTimer() {
        if(updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }
}