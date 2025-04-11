package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import data.scripts.Combat.BackgroundEntity;
import data.scripts.Combat.BackgroundShipSpawner;
import org.apache.log4j.Level;
import org.json.JSONObject;

public class CombatBackgroundEnhanced_modPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() throws Exception {
        super.onApplicationLoad();

        ApplySettings();
    }

    @Override
    public void onNewGame() {
        super.onNewGame();

    }

    private static final String SETTINGS_FILE = "data/config/CombatBackgroundEnhanced_Settings.json";
    void ApplySettings()
    {
        try
        {
            JSONObject settings = Global.getSettings().loadJSON(SETTINGS_FILE);
            BackgroundShipSpawner.ReloadSettings(settings.getJSONObject("PerfSettings"));
            BackgroundEntity.ReloadSettings(settings.getJSONObject("PerfSettings"));
        }
        catch (Exception e)
        {
            Global.getLogger(CombatBackgroundEnhanced_modPlugin.class).log(Level.ERROR, "CombatBackgroundEnhanced load failed: " + e.getMessage());
        }
    }
}
