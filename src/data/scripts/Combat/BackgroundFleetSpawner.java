package data.scripts.Combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.ShipFilter;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.ListMap;
import com.fs.starfarer.api.util.Misc;
import data.scripts.Util.ConfigCache;
import data.scripts.Util.GenMath;
import org.lwjgl.Sys;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class BackgroundFleetSpawner
{
    CombatBackground background;
    Random ran;
    BackgroundShipSpawner shipSpawner;

    List<String> fleetTypes = new ArrayList<>();
    public BackgroundFleetSpawner(CombatBackground background, Random ran, BackgroundShipSpawner shipSpawner)
    {
        this.background = background;
        this.ran = ran;
        this.shipSpawner = shipSpawner;

        fleetTypes.add(FleetTypes.PATROL_SMALL);
        fleetTypes.add(FleetTypes.PATROL_MEDIUM);
        fleetTypes.add(FleetTypes.PATROL_LARGE);
        fleetTypes.add(FleetTypes.TRADE);
        fleetTypes.add(FleetTypes.TRADE_SMUGGLER);
        fleetTypes.add(FleetTypes.TRADE_SMALL);
        fleetTypes.add(FleetTypes.TRADE_LINER);
        fleetTypes.add(FleetTypes.TASK_FORCE);
        fleetTypes.add(FleetTypes.SCAVENGER_SMALL);
        fleetTypes.add(FleetTypes.SCAVENGER_MEDIUM);
        fleetTypes.add(FleetTypes.SCAVENGER_LARGE);
    }

    // Public ************************************************
    public void SpawnHulkField(FactionAPI faction, int fleetSize, float xPos, float yPos)
    {
        String fleetType = fleetTypes.get(ran.nextInt(fleetTypes.size()));
        SpawnHulkField(faction, fleetType, fleetSize, xPos, yPos);
    }

    final float FLEET_RADIUS_SCALE = 35f;
    final float FLEET_OFFSET_MIN = 20f;
    final float FLEET_OFFSET_MAX = 35f;
    public void SpawnHulkField(FactionAPI faction, String fleetType, int fleetSize, float xPos, float yPos)
    {
        FactionDoctrineAPI doctrine = faction.getDoctrine();

        float freighterFP = fleetSize * doctrine.getCombatFreighterProbability(); // TEMP
        FleetParamsV3 params = new FleetParamsV3(
                null, // Hyperspace location
                faction.getId(), // Faction ID
                null, // Quality override (null disables)
                fleetType, // Fleet type
                fleetSize, // Combat FP
                freighterFP * .3f, // Freighter FP
                freighterFP * .3f, // Tanker FP
                freighterFP * .1f, // Transport FP
                freighterFP * .1f, // Liner FP
                freighterFP * .1f, // Utility FP
                0f); // Quality bonus

        params.random = ran;

        List<ShipVariantAPI> toSpawn = BackgroundFleetSelector.createFleet(params, faction);

        int DPActual = 0;
        for(ShipVariantAPI ship : toSpawn)
        {
            DPActual += ship.getHullSpec().getFleetPoints();
        }
        //System.out.println("!! Spawn fleet for faction: " + faction.getDisplayName() + ", Fleet Type: " + fleetType + ", Requested Fleet DP:  " + fleetSize + ", Actual Fleet DP: " + DPActual + ", ship count: " + toSpawn.size());

        float arcLength = 360f;
        float radius = FLEET_RADIUS_SCALE;
        float offsetMin = FLEET_OFFSET_MIN;
        float offsetMax = FLEET_OFFSET_MAX;

        int sizeScale = Math.max(toSpawn.size(), 3);
        radius = radius * sizeScale;
        offsetMin = offsetMin * sizeScale;
        offsetMax = offsetMax * sizeScale;

        int count = toSpawn.size();
        for(int i = 0; i < count; i++)
        {
            float percentage = ((float)i + 0.5f) / count;

            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(arcLength * percentage);

            Vector2f pos = new Vector2f(dir);
            pos.scale(radius);
            pos.translate(xPos, yPos);

            Vector2f posOffset = GenMath.RandOffset(offsetMin, offsetMax);

            Vector2f.add(pos, posOffset, pos);

            ShipVariantAPI variant = Global.getSettings().getVariant(toSpawn.get(i).getHullVariantId());
            variant.getHullSpec().getFleetPoints();
            shipSpawner.GenerateHulk(ran.nextInt(5), variant, pos.x, pos.y, GenMath.RandFacing(), 0f, false);
        }
    }

}
