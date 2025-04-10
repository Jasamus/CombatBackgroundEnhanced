package data.scripts.Combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.Util.ConfigCache;
import data.scripts.Util.GenMath;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
public class BackgroundShipSpawner
{
    private static boolean bUseShipDamageDecals = true;
    private static boolean bGenerateShipDebris = true;

    public static void ReloadSettings(JSONObject options)
    {
        bUseShipDamageDecals = options.optBoolean("ShipDamageDecals", true);
        bGenerateShipDebris = options.optBoolean("ShipDebris", true);
    }

    CombatBackground background;
    Random ran;

    public BackgroundShipSpawner(CombatBackground background, Random ran)
    {
        this.background = background;
        this.ran = ran;

        InitDebrisGroups();
    }

    final float DEBRIS_DENSITY_MIN = 35f;
    final float DEBRIS_DENSITY_MAX = 35f;
    final float DEBRIS_LARGE_CHANCE = 0.2f;
    final float DEBRIS_BREACH_CHANCE = 0.6f;

    public void GenerateHulk(int layerIndex, ShipVariantAPI shipVariant, float xPos, float yPos, float facing, float angularVelocity, boolean skipWeapons)
    {
        if(shipVariant == null)
            return;

        ShipHullSpecAPI hullSpec = shipVariant.getHullSpec();
        if(!hullSpec.isBaseHull())
        {
            hullSpec = hullSpec.getBaseHull();
        }

        ConfigCache cache = ConfigCache.GetInstance();
        ConfigCache.HullData hullData = cache.GetHullData(hullSpec);

        if(background.IsValidLayer(layerIndex-1))
            GenerateDebrisRing(layerIndex-1, xPos, yPos, 3f, 8f,
                    Math.max(hullData.collisionRadius - 100f, 0f), hullData.collisionRadius + 15f, 55f, 90f, 0.3f);

        GenerateDamagedShip(layerIndex, shipVariant, xPos, yPos, facing, angularVelocity, skipWeapons);
        GenerateDebrisRing(layerIndex, xPos, yPos, 3f, 8f,
                Math.max(hullData.collisionRadius - 75f, 25f), hullData.collisionRadius + 20f, 55f, 90f, 0.3f);

        if(background.IsValidLayer(layerIndex + 1))
            GenerateDebrisRing(layerIndex + 1, xPos, yPos, 3f, 8f,
                    0f, Math.max(hullData.collisionRadius - 45f, 0f), 40f, 70f, 0.4f);
    }

    public void GenerateDebrisField(int layerIndex, float xPos, float yPos, float size, float density)
    {
        if(background.IsValidLayer(layerIndex - 1))
            GenerateDebrisRing(layerIndex - 1, xPos, yPos, 3f, 8f,
                    0f, size * 0.8f, 55f, 90f, density);

        GenerateDebrisRing(layerIndex, xPos, yPos, 3f, 8f,
                0f, size, 30f, 120f, density);

        if(background.IsValidLayer(layerIndex + 1))
            GenerateDebrisRing(layerIndex + 1, xPos, yPos, 3f, 8f,
                    0f, size * 0.8f, 55f, 90f, density);
    }

    void GenerateDebrisRing(int layerIndex, float xPos, float yPos, float minAngularVelocity, float maxAngularVelocity, float minRadius, float maxRadius, float minVariance, float maxVariance, float densityScale)
    {
        if(!bGenerateShipDebris)
            return;

        float facing = GenMath.RandFacing();

        Vector2f forward = Misc.getUnitVectorAtDegreeAngle(facing);
        Vector2f side = Misc.getUnitVectorAtDegreeAngle(facing + 90f);

        Vector2f origin = new Vector2f();
        Vector2f.add(forward, side, origin);
        origin.scale(-maxRadius);

        forward.scale(maxRadius * 2f);
        side.scale(maxRadius * 2f);

        int forwardCount = (int)Math.ceil((maxRadius * 2f * densityScale) / GenMath.Lerp(DEBRIS_DENSITY_MIN, DEBRIS_DENSITY_MAX, ran.nextFloat()));

        int fieldCount = forwardCount * forwardCount;

        float minTest = minRadius / maxRadius;
        minTest = minTest * minTest;
        minTest /= 2f;
        for(int i = 0; i < fieldCount; i++)
        {
            int align = i / forwardCount;
            int perp = i % forwardCount;

            float alignPercentage = ((float)align + 0.5f) / forwardCount;
            float perpPercentage = ((float)perp + 0.5f) / forwardCount;

            float distTest = (alignPercentage - 0.5f) * (alignPercentage - 0.5f) + (perpPercentage - 0.5f) * (perpPercentage - 0.5f);
            if(distTest > 0.25f || distTest < minTest)
                continue;

            float debrisXPos = forward.x * alignPercentage + side.x * perpPercentage;
            float debrisYPos = forward.y * alignPercentage + side.y * perpPercentage;

            Vector2f offset = GenMath.RandOffset(minVariance, maxVariance);

            SpawnDebris(layerIndex, GenMath.IsTrue(DEBRIS_LARGE_CHANCE) ? 3 : 2,
                    xPos + debrisXPos + origin.x + offset.x,
                    yPos + debrisYPos + origin.y + offset.y,
                    GenMath.RandFacing(), GenMath.LerpRandFlip(minAngularVelocity, maxAngularVelocity));
        }
    }

    void GenerateDebrisCone(int layerIndex, float xPos, float yPos, float facing, float length, float startWidth, float endWidth,
                            float startVariance, float endVariance, float minAngularVelocity, float maxAngularVelocity, float densityScale)
    {
        Vector2f side = Misc.getUnitVectorAtDegreeAngle(facing + 90f);
        Vector2f dir = Misc.getUnitVectorAtDegreeAngle(facing);
        dir.scale(length);

        int debrisCount = (int)((length * densityScale) / GenMath.Lerp(DEBRIS_DENSITY_MIN, DEBRIS_DENSITY_MAX, ran.nextFloat()));

        for(int i = 0; i < debrisCount; i++)
        {
            float lengthPercent = ((float)i + 0.5f)/debrisCount;

            float sideOffset = (ran.nextFloat() - 0.5f) * GenMath.Lerp(startWidth, endWidth, lengthPercent);

            Vector2f randOffset = Misc.getUnitVectorAtDegreeAngle(GenMath.RandFacing());
            randOffset.scale(GenMath.LerpRand(0f, GenMath.Lerp(startVariance, endVariance, lengthPercent)));

            float offsetX = dir.x * lengthPercent + side.x * sideOffset + randOffset.x;
            float offsetY = dir.y * lengthPercent + side.y * sideOffset + randOffset.y;

            SpawnDebris(layerIndex, GenMath.IsTrue(DEBRIS_BREACH_CHANCE) ? 1 : 2,
                    xPos + offsetX, yPos + offsetY,
                    GenMath.RandFacing(), GenMath.LerpRandFlip(minAngularVelocity, maxAngularVelocity));
        }
    }

    void SpawnDebris(int layerIndex, int size, float xPos, float yPos, float facing, float angularVelocity)
    {
        CombatBackgroundLayer layer = background.GetLayer(layerIndex);

        BackgroundEntity debris = new BackgroundEntity("CombatBackgroundTerrain", GetDebrisSprite(size), new Vector2f(xPos, yPos), facing, angularVelocity);
        layer.AddEntity(debris);
    }

    //********************************************************************
    // Damaged Ship
    // Ship Generation with an additional pass using default damage decals.
    public BackgroundEntity GenerateDamagedShip(int layerIndex, ShipVariantAPI shipVariant, float xPos, float yPos, float facing, float angularVelocity, boolean skipWeapons)
    {
        BackgroundEntity ship = GenerateShip(layerIndex, shipVariant, xPos, yPos, facing, angularVelocity, skipWeapons);
        if(ship == null)
            return null;

        ShipHullSpecAPI hullSpec = shipVariant.getHullSpec();
        if(!hullSpec.isBaseHull())
        {
            hullSpec = hullSpec.getBaseHull();
        }

        if(bUseShipDamageDecals)
        {
            ApplySurfaceDamage(ship, hullSpec);
            //ApplyMajorDamage(layerIndex, ship, hullSpec);
        }

        return ship;
    }

    void ApplyMajorDamage(int layerIndex, BackgroundEntity ship,  ShipHullSpecAPI hullSpec)
    {
        ConfigCache cache = ConfigCache.GetInstance();
        ConfigCache.HullData hullData = cache.GetHullData(hullSpec);

        Vector2f shipSize = ship.GetSpriteSize();

        int count = 0;
        ShipAPI.HullSize hullSize = hullSpec.getHullSize();

        switch(hullSize)
        {
            case CAPITAL_SHIP:
                count = ran.nextInt(4)+1;
                break;
            case CRUISER:
                count = ran.nextInt(3)+1;
                break;
            case DESTROYER:
                count = ran.nextInt(2)+1;
                break;
            case FRIGATE:
            default:
                count = ran.nextInt(3);
        }

        for(int i = 0; i < count; i++)
        {
            float xPos = GenMath.LerpRandFlip(0.05f, 0.3f);
            float yPos = GenMath.LerpRandFlip(0.05f, 0.3f);

            Vector2f offset = new Vector2f(xPos, yPos);

            Vector2f offsetNormal = new Vector2f();
            offset.normalise(offsetNormal);
            float offsetAngle = Misc.getAngleInDegrees(offsetNormal);

            offset.x = offset.x * shipSize.x;
            offset.y = offset.y * shipSize.y;

            GenerateHullBreach(layerIndex, ship, hullSize, offset, offsetAngle);
        }
    }

    void GenerateHullBreach(int layerIndex, BackgroundEntity ship, ShipAPI.HullSize hullSize, Vector2f position, float facing)
    {
        int breachSize = GetBreachSize(hullSize);

        // Align the breach to the ship.
        ship.AddOverlaySprite("CombatBackgroundTerrain", GetBreachSprite(breachSize), position, 90f * ran.nextInt(4));

        Vector2f posOffset = new Vector2f();
        position.normalise(posOffset);
        posOffset.scale(75f);
        Vector2f.add(posOffset, position, posOffset);

        float shipFacing = ship.GetFacing();
        Vector2f worldPos = GenMath.VecRotate(posOffset, shipFacing);

        Vector2f shipLocation = ship.GetLocation();
        Vector2f.add(worldPos, shipLocation, worldPos);

        GenerateBreachDebris(layerIndex, worldPos.x, worldPos.y, facing + shipFacing, breachSize);
    }

    void GenerateBreachDebris(int layerIndex, float xPos, float yPos, float facing, int breachSize)
    {
        if(breachSize == 2)
            GenerateDebrisCone(layerIndex, xPos, yPos, facing, 250f, 50f, 200f,0f, 2f, 5f,8f, 2.5f);
        else if(breachSize == 1)
            GenerateDebrisCone(layerIndex, xPos, yPos, facing, 160f, 50f, 100f,0f, 2f, 5f,8f, 2.5f);
    }

    int GetBreachSize(ShipAPI.HullSize hullSize)
    {
        switch(hullSize)
        {
            case CAPITAL_SHIP:
                return 2;
            case CRUISER:
                return ran.nextFloat() > 0.75f ? 2 : 1;
            case DESTROYER:
                return ran.nextFloat() > 0.3f ? 2 : 1;
            case FRIGATE:
            default:
                return 1;
        }
    }

    String GetBreachSprite(int size)
    {
        switch(size)
        {
            case 2:
                return GetBreachLargeSprite();
            case 1:
                return GetBreachSmallSprite();
            case 0:
            default:
                return "error_sprite";
        }
    }

    String GetBreachSmallSprite()
    {
        switch(ran.nextInt(2))
        {
            case 0:
                return "ship_breach_small1";
            case 1:
            default:
                return "ship_breach_small2";
        }
    }

    String GetBreachLargeSprite()
    {
        switch(ran.nextInt(2))
        {
            case 0:
                return "ship_breach_large1";
            case 1:
            default:
                return "ship_breach_large2";
        }
    }

    void ApplySurfaceDamage(BackgroundEntity ship, ShipHullSpecAPI hullSpec)
    {
        ConfigCache cache = ConfigCache.GetInstance();
        ConfigCache.HullData hullData = cache.GetHullData(hullSpec);
        ConfigCache.HullStyleData hullStyleData = cache.GetHullStyleData(hullData.hullStyle);

        Vector2f shipSize = ship.GetSpriteSize();

        // Armor Cell size
        // Ships 150 units or fewer: 15 units
        // Ships 300 units or more: 30 units
        // In between: 1/10 of the ship length
        // Based on ship length (height) value, not the largest of height and width
        float armorSize = GenMath.Lerp(15f, 30f, (Math.min(shipSize.y, 300f) - 150f) / 150f);

        // Generate "armor" grid
        int armorXCount = (int)Math.ceil(shipSize.x / armorSize);
        int armorYCount = (int)Math.ceil(shipSize.y / armorSize);

        // Width seems to always been an even number of cells.
        if(armorXCount % 2 != 0)
            armorXCount += 1;

        // Cell value is percent of armor damage.
        // Add 1 cell padding around the edge.
        float[][] armorGrid = new float[armorXCount + 2][armorYCount + 2];

        int initialDamagePoints = GetInitialDamagePointCountForHullSize(hullSpec.getHullSize());
        GenerateInitialArmorDamagePoints(armorGrid, armorXCount, armorYCount, initialDamagePoints);

        int iterations = 3;
        ExpandArmorDamage(armorGrid, armorXCount, armorYCount, iterations);

        // Apply Decals
        // Damage decals appear to be approx 70% larger than the armor cells
        // Increased to 150% larger for better readability
        float decalSize = armorSize * 2.5f;

        float armorXOffset = -((armorSize * (armorXCount - 1)) / 2f);
        float armorYOffset = -((armorSize * (armorYCount - 1)) / 2f);

        armorXOffset = armorXOffset + shipSize.x/2f - hullData.centerX;
        armorYOffset = armorYOffset + shipSize.y/2f - hullData.centerY;

        for(int i = 0; i < armorXCount; i++)
        {
            for(int j = 0; j < armorYCount; j++)
            {
                float damageStrength = armorGrid[i+1][j+1];
                if(damageStrength > 0.05f)
                {
                    float xDamagePos = armorSize * i;
                    float yDamagePos = armorSize * j;

                    AddHullDamageOverlay(ship, hullStyleData.damageDecalSheet, decalSize, damageStrength, xDamagePos + armorXOffset, yDamagePos + armorYOffset);
                }
            }
        }
    }

    void GenerateInitialArmorDamagePoints(float[][] armorGrid, int sizeX, int sizeY, int damagePoints)
    {
        for(int i = 0; i < damagePoints; i++)
        {
            int randX = ran.nextInt(sizeX) + 1;
            int randY = ran.nextInt(sizeY) + 1;

            armorGrid[randX][randY] = 1.0f;
        }
    }

    int GetInitialDamagePointCountForHullSize(ShipAPI.HullSize size)
    {
        switch (size)
        {
            case CAPITAL_SHIP:
                return 6;
            case CRUISER:
                return 4;
            case DESTROYER:
                return 3;
            case FRIGATE:
            default:
                return 2;
        }
    }

    void ExpandArmorDamage(float[][] armorGrid, int sizeX, int sizeY, int iterations)
    {
        float[][] resultGrid = new float[sizeX + 2][sizeY + 2];

        for(int it = 0; it < iterations; it++)
        {
            for(int i = 1; i < sizeX; i++)
            {
                for (int j = 1; j < sizeY; j++)
                {
                    float highest = GetHighestNeighbor(armorGrid, i, j);

                    if(ran.nextFloat() * highest > 0.2f)
                        resultGrid[i][j] = GenMath.LerpRand(0.8f, 1.0f) * highest;
                }
            }

            // Swap the grids for each iteration.
            float[][] swapSlot = resultGrid;
            resultGrid = armorGrid;
            armorGrid = swapSlot;
        }

        // Do a final swap if its odd.
        if(iterations % 2 == 1)
        {
            float[][] swapSlot = resultGrid;
            resultGrid = armorGrid;
            armorGrid = swapSlot;
        }
    }

    private float GetHighestNeighbor(float[][] armorGrid, int i, int j) {
        float highest = 0f;
        highest = Math.max(highest, armorGrid[i -1][j -1]);
        highest = Math.max(highest, armorGrid[i -1][j]);
        highest = Math.max(highest, armorGrid[i -1][j +1]);
        highest = Math.max(highest, armorGrid[i][j -1]);
        highest = Math.max(highest, armorGrid[i][j +1]);
        highest = Math.max(highest, armorGrid[i +1][j -1]);
        highest = Math.max(highest, armorGrid[i +1][j]);
        highest = Math.max(highest, armorGrid[i +1][j +1]);
        return highest;
    }

    final float DAMAGE_DECAL_PIXELSIZE = 48.f;
    void AddHullDamageOverlay(BackgroundEntity ship, String damageDecalSheet, float size, float damageStrength, float xPos, float yPos)
    {
        SpriteAPI damageDecal = Global.getSettings().getSprite(damageDecalSheet);

        float height = DAMAGE_DECAL_PIXELSIZE / damageDecal.getHeight();
        float width = DAMAGE_DECAL_PIXELSIZE / damageDecal.getWidth();

        int decalIndex = ran.nextInt(6);

        int xDecal = decalIndex / 3;
        int yDecal = decalIndex % 3;

        damageDecal.setTexX(width * xDecal);
        damageDecal.setTexY(height * yDecal);

        damageDecal.setTexHeight(height);
        damageDecal.setTexWidth(width);

        damageDecal.setWidth(size);
        damageDecal.setHeight(size);

        damageDecal.setAlphaMult(damageStrength);

        ship.AddOverlaySprite(damageDecal, new Vector2f(xPos, yPos), GenMath.RandFacing());
    }

    //********************************************************************
    // Ships

    // Variant that generates all mounted weapons and mount covers.
    public BackgroundEntity GenerateShip(int layerIndex, ShipVariantAPI shipVariant, float xPos, float yPos, float facing, float angularVelocity, boolean skipWeapons)
    {
        ConfigCache cache = ConfigCache.GetInstance();

        CombatBackgroundLayer layer = background.GetLayer(layerIndex);

        ShipHullSpecAPI hullSpec = shipVariant.getHullSpec();
        if(!hullSpec.isBaseHull())
        {
            hullSpec = hullSpec.getBaseHull();
        }

        ConfigCache.HullData hullData = cache.GetHullData(hullSpec);
        if(hullData == null)
            return null;

        // Don't currently support module based ships, skip generation.
        if(hullData.hasModules)
            return null;

        ConfigCache.HullStyleData hullStyleData = cache.GetHullStyleData(hullData.hullStyle);

        BackgroundEntity ship = new BackgroundEntity(shipVariant.getHullSpec().getSpriteName(), new Vector2f(xPos, yPos), hullData.centerX, hullData.centerY, facing, angularVelocity);
        layer.AddEntity(ship);

        Collection<String> fittedWeaponSlots = shipVariant.getFittedWeaponSlots();
        List<WeaponSlotAPI> allWeaponSlots = hullSpec.getAllWeaponSlotsCopy();

        for(WeaponSlotAPI slot : allWeaponSlots)
        {
            // Skip non-weapon slots.
            if(!slot.isWeaponSlot())
                continue;

            String slotId = slot.getId();

            String weaponId = shipVariant.getWeaponId(slotId);
            WeaponSpecAPI weaponSpec = Global.getSettings().getWeaponSpec(weaponId);

            Vector2f slotPos = new Vector2f(slot.getLocation());
            GenMath.VecRotate(slotPos, 90f);

            // Empty slot, add a cover for it.
            if(skipWeapons || !fittedWeaponSlots.contains(slotId))
            {
                if(slot.isHardpoint())
                    ship.AddChildSprite(hullStyleData.GetHardpointcover(slot.getSlotSize()), slotPos, slot.getAngle(), 0.5f,0.25f);
                else
                    ship.AddChildSprite(hullStyleData.GetTurretCover(slot.getSlotSize()), slotPos, slot.getAngle());

                continue;
            }

            ConfigCache.WeaponData weaponData = cache.GetWeaponData(weaponId);

            if(slot.isHardpoint())
            {
                if(!weaponData.renderBarrelBelow)
                {
                    ship.AddChildSprite(weaponSpec.getHardpointSpriteName(), slotPos, slot.getAngle(), 0.5f,0.25f);
                }

                if(weaponSpec instanceof ProjectileWeaponSpecAPI)
                {
                    ProjectileWeaponSpecAPI projectileWeaponSpecAPI = (ProjectileWeaponSpecAPI) weaponSpec;
                    ship.AddChildSprite(projectileWeaponSpecAPI.getHardpointGunSpriteName(), slotPos, slot.getAngle(), 0.5f,0.25f);
                }

                if(weaponData.renderBarrelBelow)
                {
                    ship.AddChildSprite(weaponSpec.getHardpointSpriteName(), slotPos, slot.getAngle(), 0.5f,0.25f);
                }
            }
            else
            {
                // Bias angle towards default, using even distribution looks strange
                float blendValue = (float)Math.pow(ran.nextFloat(), 2f);

                float angleOffset = GenMath.Lerp(0f, slot.getArc()/2f, blendValue) * (ran.nextBoolean() ? 1.f : -1f);

                if(!weaponData.renderBarrelBelow)
                {
                    ship.AddChildSprite(weaponSpec.getTurretSpriteName(), slotPos, slot.getAngle() + angleOffset);
                }
                if(weaponSpec instanceof ProjectileWeaponSpecAPI) {
                    ProjectileWeaponSpecAPI projectileWeaponSpecAPI = (ProjectileWeaponSpecAPI) weaponSpec;
                    ship.AddChildSprite(projectileWeaponSpecAPI.getTurretGunSpriteName(), slotPos, slot.getAngle() + angleOffset);
                }

                if(weaponData.renderBarrelBelow)
                {
                    ship.AddChildSprite(weaponSpec.getTurretSpriteName(), slotPos, slot.getAngle() + angleOffset);
                }
            }

        }

        return ship;
    }

    final String DEBRIS_SPRITE_ERROR = "error_sprite";
    final String DEBRIS_BREACH_SPRITE_1 = "ship_debris_breach1";
    final String DEBRIS_BREACH_SPRITE_2 = "ship_debris_breach2";
    final String DEBRIS_BREACH_SPRITE_3 = "ship_debris_breach3";
    final String DEBRIS_SMALL_SPRITE_1 = "ship_debris_small1";
    final String DEBRIS_SMALL_SPRITE_2 = "ship_debris_small2";
    final String DEBRIS_SMALL_SPRITE_3 = "ship_debris_small3";
    final String DEBRIS_SMALL_SPRITE_4 = "ship_debris_small4";
    final String DEBRIS_SMALL_SPRITE_5 = "ship_debris_small5";
    final String DEBRIS_SMALL_SPRITE_6 = "ship_debris_small6";
    final String DEBRIS_LARGE_SPRITE_1 = "ship_debris_large1";
    final String DEBRIS_LARGE_SPRITE_2 = "ship_debris_large2";
    final String DEBRIS_LARGE_SPRITE_3 = "ship_debris_large3";
    final String DEBRIS_LARGE_SPRITE_4 = "ship_debris_large4";

    List<List<String>> debrisSprites = new ArrayList<>();

    void InitDebrisGroups()
    {
        List<String> defaultDebris = new ArrayList<>(1);
        defaultDebris.add(DEBRIS_SPRITE_ERROR);

        List<String> breachDebris = new ArrayList<>(3);
        breachDebris.add(DEBRIS_BREACH_SPRITE_1);
        breachDebris.add(DEBRIS_BREACH_SPRITE_2);
        breachDebris.add(DEBRIS_BREACH_SPRITE_3);

        List<String> smallDebris = new ArrayList<>(3);
        smallDebris.add(DEBRIS_SMALL_SPRITE_1);
        smallDebris.add(DEBRIS_SMALL_SPRITE_2);
        smallDebris.add(DEBRIS_SMALL_SPRITE_3);
        smallDebris.add(DEBRIS_SMALL_SPRITE_4);
        smallDebris.add(DEBRIS_SMALL_SPRITE_5);
        smallDebris.add(DEBRIS_SMALL_SPRITE_6);

        List<String> largeDebris = new ArrayList<>(3);
        largeDebris.add(DEBRIS_LARGE_SPRITE_1);
        largeDebris.add(DEBRIS_LARGE_SPRITE_2);
        largeDebris.add(DEBRIS_LARGE_SPRITE_3);
        largeDebris.add(DEBRIS_LARGE_SPRITE_4);

        debrisSprites.add(defaultDebris);
        debrisSprites.add(breachDebris);
        debrisSprites.add(smallDebris);
        debrisSprites.add(largeDebris);
    }

    String GetDebrisSprite(int size)
    {
        List<String> debrisGroup = debrisSprites.get(size);
        return debrisGroup.get(ran.nextInt(debrisGroup.size()));
    }
}
