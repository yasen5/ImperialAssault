package game.campaign;

import java.io.Serializable;

public enum CampaignStage implements Serializable {
    SETUP,
    MISSION_STAGE,
    POST_MISSION_CLEANUP,
    REBEL_UPGRADE_STAGE,
    IMPERIAL_UPGRADE_STAGE,
    COMPLETE
}
