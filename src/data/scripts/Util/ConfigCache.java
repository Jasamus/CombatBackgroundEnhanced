package data.scripts.Util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

// A cache for retrieving config data that isn't API exposed and must be fetched manually
public class ConfigCache
{
    private static ConfigCache instance = new ConfigCache();
    public static ConfigCache GetInstance() { return instance; }

    private Map<String, HullData> shipCache = new HashMap<>();
    private Map<String, HullStyleData> hullStyleCache = new HashMap<>();
    private Map<String, WeaponData> weaponCache = new HashMap<>();

    HullData defaultHullData;
    HullStyleData defaultHullStyleData;
    WeaponData defaultWeaponData;

    public ConfigCache()
    {
        defaultHullData = new HullData(0f,0f,"LOW_TECH", 0f);

        defaultHullStyleData = new HullStyleData("graphics/weapons/covers/cover_turret_lowtech_small.png",
                "graphics/weapons/covers/cover_hardpoint_lowtech_small.png",
                "graphics/weapons/covers/cover_turret_lowtech_medium.png",
                "graphics/weapons/covers/cover_hardpoint_lowtech_medium.png",
                "graphics/weapons/covers/cover_turret_lowtech_large.png",
                "graphics/weapons/covers/cover_hardpoint_lowtech_large.png",
                "graphics/damage/damage_decal_sheet_base.png",
                "graphics/damage/damage_decal_sheet_glow.png");

        defaultWeaponData = new WeaponData(false);
    }

    // Hull Config ************************************************
    HullData LoadHullConfig(ShipHullSpecAPI hullSpec)
    {
        float centerX = 0f;
        float centerY = 0f;
        String hullStyle = null;
        float collisionRadius = 0f;

        try {
            Path path = Paths.get(hullSpec.getShipFilePath());
            JSONObject obj = Global.getSettings().loadJSON("data/hulls/" + path.getFileName(), true);
            JSONArray center = obj.getJSONArray("center");
            if (center != null) {
                for (int i = 0; i < center.length(); i++) {
                    if(i == 0)
                        centerX = (float)center.getDouble(i);
                    else if(i == 1)
                        centerY = (float)center.getDouble(i);
                }
            }

            hullStyle = obj.getString("style");

            collisionRadius = (float)obj.getInt("collisionRadius");

            HullData data = new HullData(centerX, centerY, hullStyle, collisionRadius);

            shipCache.put(hullSpec.getHullId(), data);
            return data;
        }
        catch (Exception e) {

        }

        return defaultHullData;
    }

    public HullData GetHullData(ShipHullSpecAPI hullSpec)
    {
        HullData data = shipCache.get(hullSpec.getHullId());
        if(data == null)
            data = LoadHullConfig(hullSpec);

        return data;
    }

    // Hull Style Config ************************************************
    HullStyleData LoadHullStyleConfig(String hullStyle)
    {
        String slotCoverSmallTurret = null;
        String slotCoverSmallHardpoint = null;
        String slotCoverMediumTurret = null;
        String slotCoverMediumHardpoint = null;
        String slotCoverLargeTurret = null;
        String slotCoverLargeHardpoint = null;

        String damageDecalSheet = null;
        String damageDecalGlowSheet = null;

        try {
            JSONObject obj = Global.getSettings().getMergedJSON("data/config/hull_styles.json");
            JSONObject style = obj.getJSONObject(hullStyle);
            if(style != null)
            {
                slotCoverSmallTurret = style.getString("slotCoverSmallTurret");
                slotCoverSmallHardpoint = style.getString("slotCoverSmallHardpoint");
                slotCoverMediumTurret = style.getString("slotCoverMediumTurret");
                slotCoverMediumHardpoint = style.getString("slotCoverMediumHardpoint");
                slotCoverLargeTurret = style.getString("slotCoverLargeTurret");
                slotCoverLargeHardpoint = style.getString("slotCoverLargeHardpoint");

                damageDecalSheet = style.getString("damageDecalSheet");
                damageDecalGlowSheet = style.getString("damageDecalGlowSheet");

                HullStyleData data = new HullStyleData(slotCoverSmallTurret, slotCoverSmallHardpoint,
                        slotCoverMediumTurret, slotCoverMediumHardpoint,
                        slotCoverLargeTurret, slotCoverLargeHardpoint,
                        damageDecalSheet, damageDecalGlowSheet);

                hullStyleCache.put(hullStyle, data);

                return data;
            }

        }
        catch (Exception e) {

        }

        return defaultHullStyleData;
    }

    public HullStyleData GetHullStyleData(String hullStyle)
    {
        HullStyleData data = hullStyleCache.get(hullStyle);
        if(data == null)
            data = LoadHullStyleConfig(hullStyle);

        return data;
    }

    // Weapon Config ************************************************
    WeaponData LoadWeaponConfig(String weaponId)
    {
        boolean renderBarrelBelow = false;
        try {
            JSONObject obj = Global.getSettings().loadJSON("data/weapons/" + weaponId + ".wpn", true);
            if(obj.has("renderHints"))
            {
                JSONArray hints = obj.getJSONArray("renderHints");
                if (hints != null)
                {
                    for (int i = 0; i < hints.length(); i++)
                    {
                        if(hints.getString(i).equalsIgnoreCase("RENDER_BARREL_BELOW"))
                            renderBarrelBelow = true;
                    }
                }
            }

            WeaponData data = new WeaponData(renderBarrelBelow);
            weaponCache.put(weaponId, data);

            return data;
        }
        catch (Exception e) {

        }

        return defaultWeaponData;
    }

    public WeaponData GetWeaponData(String weaponId)
    {
        WeaponData data = weaponCache.get(weaponId);
        if(data == null)
            data = LoadWeaponConfig(weaponId);

        return data;
    }

    // Data ************************************************
    public class HullData
    {
        public float centerX;
        public float centerY;
        public String hullStyle;
        public float collisionRadius;

        public HullData(float centerX, float centerY, String hullStyle, float collisionRadius)
        {
            this.centerX = centerX;
            this.centerY = centerY;
            this.hullStyle = hullStyle;
            this.collisionRadius = collisionRadius;
        }
    }

    public class HullStyleData
    {
        public String slotCoverSmallTurret;
        public String slotCoverSmallHardpoint;
        public String slotCoverMediumTurret;
        public String slotCoverMediumHardpoint;
        public String slotCoverLargeTurret;
        public String slotCoverLargeHardpoint;
        public String damageDecalSheet;
        public String damageDecalGlowSheet;

        public HullStyleData(String slotCoverSmallTurret, String slotCoverSmallHardpoint,
                             String slotCoverMediumTurret, String slotCoverMediumHardpoint,
                             String slotCoverLargeTurret, String slotCoverLargeHardpoint,
                             String damageDecalSheet, String damageDecalGlowSheet)
        {
            this.slotCoverSmallTurret = slotCoverSmallTurret;
            this.slotCoverSmallHardpoint = slotCoverSmallHardpoint;
            this.slotCoverMediumTurret = slotCoverMediumTurret;
            this.slotCoverMediumHardpoint = slotCoverMediumHardpoint;
            this.slotCoverLargeTurret = slotCoverLargeTurret;
            this.slotCoverLargeHardpoint = slotCoverLargeHardpoint;

            this.damageDecalSheet = damageDecalSheet;
            this.damageDecalGlowSheet = damageDecalGlowSheet;
        }

        public String GetTurretCover(WeaponAPI.WeaponSize size)
        {
            switch (size)
            {
                case LARGE:
                    return slotCoverLargeTurret;
                case MEDIUM:
                    return slotCoverMediumTurret;
                case SMALL:
                default:
                    return slotCoverSmallTurret;
            }
        }

        public String GetHardpointcover(WeaponAPI.WeaponSize size)
        {
            switch (size)
            {
                case LARGE:
                    return slotCoverLargeHardpoint;
                case MEDIUM:
                    return slotCoverMediumHardpoint;
                case SMALL:
                default:
                    return slotCoverSmallHardpoint;
            }
        }
    }

    public class WeaponData
    {
        public boolean renderBarrelBelow;

        public WeaponData(boolean renderBarrelBelow)
        {
            this.renderBarrelBelow = renderBarrelBelow;
        }
    }
}
