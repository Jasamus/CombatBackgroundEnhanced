package data.scripts.Combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class CampaignTerrainAnalyzer
{
    Vector2f playerFleetLocation;

    boolean inHyperspace;

//    boolean isAsteroidBeltNearby = false;
//    boolean isAsteroidFieldNearby = false;
//    boolean isPlanetRingNearby = false;
//    boolean isDebrisFieldNearby = false;
//    List<FactionAPI> combatFactions = new ArrayList<>();
//    CombatFleetData enemyCombatData = new CombatFleetData();
//    CombatFleetData allyCombatData = new CombatFleetData();
//    List<FleetMemberAPI> enemyCombatFleet = new ArrayList<>();
//    List<FleetMemberAPI> allyCombatFleet = new ArrayList<>();
//    Vector2f enemyAveragePos = new Vector2f();
//    Vector2f allyAveragePos = new Vector2f();
    List<FactionAPI> inSystemFactions = new ArrayList<>();
//    List<CampaignFleetAPI> nearbyFleets = new ArrayList<>();
//    List<DerelictData> nearbyDerelicts = new ArrayList<>();
//    List<DebrisFieldData> nearbyDebrisFields = new ArrayList<>();

    //****************************************************************************************************************************
    public boolean IsInHyperSpace() { return inHyperspace; }
//    public boolean IsExistingFight() { return !allyCombatData.factions.isEmpty(); }
////    public List<FactionAPI> GetCombatFactions() { return combatFactions; }
//    public CombatFleetData GetEnemyCombatFleet() { return enemyCombatData; }
//    public CombatFleetData GetAllyCombatFleet() { return allyCombatData; }
    // In system it's all factions, in hyperspace, it's all fleet factions within a certain range.
    public List<FactionAPI> GetNearbyFactions() { return inSystemFactions; }
//    public List<CampaignFleetAPI> GetNearbyFleets() { return nearbyFleets; }
//    public List<DerelictData> GetNearbyDerelicts() { return nearbyDerelicts; }
//    public List<DebrisFieldData> GetNearbyDebridFields() { return nearbyDebrisFields; }

//    public boolean IsAsteroidBeltNearby()  { return isAsteroidBeltNearby; }
//    public boolean IsAsteroidFieldNearby() { return isAsteroidFieldNearby; }
//    public boolean IsPlanetRingNearby() { return isPlanetRingNearby;  }
//    public boolean IsDebrisFieldNearby() {  return isDebrisFieldNearby; }

    //****************************************************************************************************************************
    public CampaignTerrainAnalyzer()
    {
        LocationAPI currentLocation = Global.getSector().getCurrentLocation();
        playerFleetLocation = Global.getSector().getPlayerFleet().getLocation();

        inHyperspace = currentLocation.isHyperspace();
        if(inHyperspace)
            GetNearbyFleetFactions(currentLocation);
        else
            GetSystemPlanetFactions(currentLocation);

//        GetNearbyFleets(currentLocation);
//        GetLocalFeatures(currentLocation);
//        GetNearbyDerelicts(currentLocation);
    }

//    public void DelayedInit()
//    {
//        GetFactionsAndShipsInCombat();
//        GetCombatFleetStats();
//    }

    //****************************************************************************************************************************
//    final float DEBRIS_FIELD_CHECK_DISTANCE = 5000f;
//    final float RING_SYSTEM_CHECK_DISTANCE = 5000f;
//
//    final float ASTEROID_FIELD_CHECK_DISTANCE = 5000f;
//    final float ASTEROID_BELT_CHECK_DISTANCE = 5000f;
//    private void GetLocalFeatures(LocationAPI currentLocation)
//    {
//        List<CampaignTerrainAPI> terrainEntities = currentLocation.getEntities(CampaignTerrainAPI.class);
//        for(CampaignTerrainAPI terrainEntity : terrainEntities)
//        {
//            CampaignTerrainPlugin campPlugin = terrainEntity.getPlugin();
//
//            if(campPlugin == null)
//                continue;
//
//            float dist = Misc.getDistance(terrainEntity.getLocation(), playerFleetLocation);
//            if(campPlugin instanceof DebrisFieldTerrainPlugin)
//            {
//                if(dist < DEBRIS_FIELD_CHECK_DISTANCE)
//                {
//                    isDebrisFieldNearby = true;
//
//                    DebrisFieldTerrainPlugin debrisPlugin = (DebrisFieldTerrainPlugin) campPlugin;
//                    if(debrisPlugin.getParams() == null)
//                        continue;
//
//                    float density = debrisPlugin.getParams().density;
//                    float baseDensity = debrisPlugin.getParams().baseDensity;
//
//                    nearbyDebrisFields.add(new DebrisFieldData(density * baseDensity, GetNormalizedOffset(terrainEntity.getLocation(), playerFleetLocation, DEBRIS_FIELD_CHECK_DISTANCE)));
//                }
//            }
//            else if(campPlugin instanceof RingSystemTerrainPlugin)
//            {
//                if(dist < RING_SYSTEM_CHECK_DISTANCE)
//                    isPlanetRingNearby = true;
////                RingSystemTerrainPlugin ringSystemPlugin = (RingSystemTerrainPlugin) campPlugin;
////                System.out.println("!!   Ring System radius: " + ringSystemPlugin.params.middleRadius);
//            }
//            // Check Field first since it's a child of belt for some reason and doesn't possess the same param types
//            else if(campPlugin instanceof AsteroidFieldTerrainPlugin)
//            {
//                if(dist < ASTEROID_FIELD_CHECK_DISTANCE)
//                    isAsteroidFieldNearby = true;
////                AsteroidFieldTerrainPlugin fieldPlugin = (AsteroidFieldTerrainPlugin) campPlugin;
////
////                if(fieldPlugin.params != null)
////                {
//////                    System.out.println("!!   Asteroid Field size: " + fieldPlugin.params.numAsteroids);
////                }
//            }
//            else if(campPlugin instanceof AsteroidBeltTerrainPlugin)
//            {
//                if(dist < ASTEROID_BELT_CHECK_DISTANCE)
//                    isAsteroidBeltNearby = true;
////                AsteroidBeltTerrainPlugin beltPlugin = (AsteroidBeltTerrainPlugin) campPlugin;
////                System.out.println("!!   Asteroid Ring Radius: " + beltPlugin.params.middleRadius);
//            }
//        }
//
//    }

//    final float DERELICT_SHIP_CHECK_DISTANCE = 5000f;
//    void GetNearbyDerelicts(LocationAPI currentLocation)
//    {
//        List<CustomCampaignEntityAPI> customEntities = currentLocation.getEntities(CustomCampaignEntityAPI.class);
//        for(CustomCampaignEntityAPI customEntity : customEntities)
//        {
//            float dist = Misc.getDistance(customEntity.getLocation(), playerFleetLocation);
//            if(dist > DERELICT_SHIP_CHECK_DISTANCE)
//                continue;
//
//            CustomCampaignEntityPlugin campPlugin = customEntity.getCustomPlugin();
//
//            if(campPlugin == null)
//                continue;
//
//            if(campPlugin instanceof DerelictShipEntityPlugin)
//            {
//                DerelictShipEntityPlugin derelictShipPlugin = (DerelictShipEntityPlugin) campPlugin;
//
//                if(derelictShipPlugin.getData() != null)
//                {
//                    if(derelictShipPlugin.getData().ship != null)
//                    {
//                        //System.out.println("!! Found Derelict Dist: " + dist + ", Hull:" + derelictShipPlugin.getData().ship.getVariant().getHullSpec().getHullName() + ", Variant: " + derelictShipPlugin.getData().ship.getVariant().getDisplayName());
//                        nearbyDerelicts.add(new DerelictData(derelictShipPlugin.getData().ship.getVariant(), GetNormalizedOffset(customEntity.getLocation(), playerFleetLocation, DERELICT_SHIP_CHECK_DISTANCE)));
//                    }
//                }
//            }
//        }
//
//    }

//    final float FLEET_CHECK_DISTANCE = 5000f;
//    void GetNearbyFleets(LocationAPI currentLocation)
//    {
//        List<CampaignFleetAPI> fleets = currentLocation.getFleets();
//        for( CampaignFleetAPI fleet : fleets)
//        {
//            // Skip stations
//            if(fleet.isStationMode())
//                continue;
//
//            // Skip player fleet
//            if(fleet.isPlayerFleet())
//                continue;
//
//            Vector2f location = fleet.getLocation();
//            float dist = Misc.getDistance(location, playerFleetLocation);
//            if(dist < FLEET_CHECK_DISTANCE )
//                nearbyFleets.add(fleet);
//        }
//    }

    void GetSystemPlanetFactions(LocationAPI currentLocation)
    {
        // Planet Factions
        List<PlanetAPI> planets = currentLocation.getPlanets();
        for(PlanetAPI planet : planets)
        {
            if(planet.getFaction().isNeutralFaction())
                continue;

            FactionAPI planetFaction = planet.getFaction();
            if(!inSystemFactions.contains(planetFaction))
                inSystemFactions.add(planetFaction);
        }

        // Station Factions
        List<CampaignFleetAPI> fleets = currentLocation.getFleets();
        for( CampaignFleetAPI fleet : fleets)
        {
            //System.out.println("!! Fleet: " + fleet.getName() + ", Faction: " + fleet.getFaction().getDisplayName() + ", IsStation: " + fleet.isStationMode());
            if(!fleet.isStationMode())
                continue;

            FactionAPI stationFaction = fleet.getFaction();
            if(!inSystemFactions.contains(stationFaction))
                inSystemFactions.add(stationFaction);
        }
    }

    final float FLEET_FACTION_CHECK_DISTANCE = 15000f;
    void GetNearbyFleetFactions(LocationAPI currentLocation)
    {
        List<CampaignFleetAPI> fleets = currentLocation.getFleets();
        for( CampaignFleetAPI fleet : fleets)
        {
            // Skip stations
            if(fleet.isStationMode())
                continue;

            // Skip player fleet
            if(fleet.isPlayerFleet())
                continue;

            FactionAPI faction = fleet.getFaction();
            if(inSystemFactions.contains(faction))
               continue;

            Vector2f location = fleet.getLocation();
            float dist = Misc.getDistance(location, playerFleetLocation);
            if(dist < FLEET_FACTION_CHECK_DISTANCE )
            {
                //System.out.println("!! Added Hyperspace nearby fleet faction : " + faction.getDisplayName());
                inSystemFactions.add(faction);
            }
        }
    }

    public ShipVariantAPI GetNearbyStationForFaction(FactionAPI faction)
    {
        LocationAPI currentLocation = Global.getSector().getCurrentLocation();
        List<CampaignFleetAPI> fleets = currentLocation.getFleets();
        for( CampaignFleetAPI fleet : fleets)
        {
            // Skip player fleet
            if(fleet.isPlayerFleet())
                continue;

            if(!fleet.isStationMode())
                continue;

            FactionAPI fleetFaction = fleet.getFaction();
            if(fleetFaction == faction)
            {
                FleetDataAPI fleetData = fleet.getFleetData();
                List<FleetMemberAPI> membersListCopy = fleetData.getMembersListCopy();
                for(FleetMemberAPI fleetMember : membersListCopy)
                {
                    ShipVariantAPI variant = fleetMember.getVariant();
                    ShipHullSpecAPI hullSpec = variant.getHullSpec();
                    if(!hullSpec.isBaseHull())
                        hullSpec = hullSpec.getBaseHull();

                    // Most stations use the hint STATION. There are some cases of stations that don't so they will end up being missed but I've not found any reliable way of telling if a ship is a station or not.
                    if(hullSpec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION))
                        return variant;
                }
            }
        }

        return null;
    }

//    // NOTE: Won't create accurate results on the first frame of combat.
//    void GetFactionsAndShipsInCombat()
//    {
//        CombatFleetManagerAPI enemyFleetManager = Global.getCombatEngine().getFleetManager(FleetSide.ENEMY);
//        if(enemyFleetManager != null)
//        {
//            List<PersonAPI> fleetCommanders = enemyFleetManager.getAllFleetCommanders();
//            for (PersonAPI fleetCommander : fleetCommanders)
//            {
//                FactionAPI faction = fleetCommander.getFaction();
//                if (!faction.isPlayerFaction())
//                {
//                    if(!enemyCombatData.factions.contains(faction))
//                        enemyCombatData.factions.add(faction);
//                }
//            }
//
//            List<FleetMemberAPI> deployedCopy = enemyFleetManager.getDeployedCopy();
//            if(deployedCopy == null || deployedCopy.isEmpty())
//                return;
//
//            List<FleetMemberAPI> reservesCopy = enemyFleetManager.getReservesCopy();
//            if(reservesCopy == null)
//                return;
//
//            FleetDataAPI fleetData = deployedCopy.get(0).getFleetData();
//            if(fleetData == null)
//                return;
//
//            int fleetSize = 0;
//            List<FleetMemberAPI> membersListCopy = fleetData.getMembersListCopy();
//            for(FleetMemberAPI fleetMember : membersListCopy)
//            {
//                fleetSize += fleetMember.getHullSpec().getFleetPoints();
//                //System.out.println("!! Combat Fleet Enemy Hull: " + fleetMember.getVariant().getHullSpec().getHullName() + ", Variant: " + fleetMember.getVariant().getDisplayName());
//                enemyCombatData.ships.add(fleetMember);
//
//                // Keep track of ships that are in reserve
//                if(reservesCopy.contains(fleetMember))
//                    allyCombatData.reserveShips.add(fleetMember);
//            }
//            enemyCombatData.fleetSize = fleetSize;
//        }
//
//        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
//        List<FleetMemberAPI> playerFleetMembers = playerFleet.getFleetData().getMembersListCopy();
//
//        CombatFleetManagerAPI allyFleetManager = Global.getCombatEngine().getFleetManager(FleetSide.PLAYER);
//        if(allyFleetManager != null)
//        {
//            List<PersonAPI> fleetCommanders = allyFleetManager.getAllFleetCommanders();
//            for (PersonAPI fleetCommander : fleetCommanders)
//            {
//                FactionAPI faction = fleetCommander.getFaction();
//                if (!faction.isPlayerFaction())
//                {
//                    if(!allyCombatData.factions.contains(faction))
//                        allyCombatData.factions.add(faction);
//                }
//            }
//
//            List<FleetMemberAPI> deployedCopy = allyFleetManager.getDeployedCopy();
//            if(deployedCopy == null || deployedCopy.isEmpty())
//                return;
//
//            List<FleetMemberAPI> reservesCopy = allyFleetManager.getReservesCopy();
//            if(reservesCopy == null)
//                return;
//
//            FleetDataAPI fleetData = deployedCopy.get(0).getFleetData();
//            if(fleetData == null)
//                return;
//
//            int fleetSize = 0;
//            List<FleetMemberAPI> membersListCopy = fleetData.getMembersListCopy();
//            for(FleetMemberAPI fleetMember : membersListCopy)
//            {
//                if(playerFleetMembers.contains(fleetMember))
//                    continue;
//
//                fleetSize += fleetMember.getHullSpec().getFleetPoints();
//                //System.out.println("!! Combat Fleet Ally Hull: " + fleetMember.getVariant().getHullSpec().getHullName() + ", Variant: " + fleetMember.getVariant().getDisplayName());
//                allyCombatData.ships.add(fleetMember);
//
//                // Keep track of ships that are in reserve
//                if(reservesCopy.contains(fleetMember))
//                    allyCombatData.reserveShips.add(fleetMember);
//            }
//
//            allyCombatData.fleetSize = fleetSize;
//        }
//    }
//
//    void GetCombatFleetStats()
//    {
//        List<ShipAPI> ships = Global.getCombatEngine().getShips();
//        int enemyCount = 0;
//        int allyCount = 0;
//        Vector2f enemyTotal = new Vector2f();
//        Vector2f allyTotal = new Vector2f();
//        for(ShipAPI ship : ships)
//        {
//            if(ship.isFighter())
//                continue;
//
//            if(ship.getFleetMember().isAlly())
//            {
//                allyCount++;
//                Vector2f.add(allyTotal, ship.getLocation(), allyTotal);
//                allyCombatData.deployedShips.add(ship);
//            }
//            else
//            {
//                enemyCount++;
//                Vector2f.add(enemyTotal, ship.getLocation(), enemyTotal);
//                enemyCombatData.deployedShips.add(ship);
//            }
//        }
//
//        enemyTotal.scale(1f/enemyCount);
//        allyTotal.scale(1f/allyCount);
//
//        enemyCombatData.deployedAveragePos.set(enemyTotal);
//        allyCombatData.deployedAveragePos.set(allyTotal);
//    }
//
//    Vector2f GetNormalizedOffset(Vector2f pos1, Vector2f pos2, float dist)
//    {
//        Vector2f result = new Vector2f();
//        Vector2f.sub(pos1, pos2, result);
//        result.scale(1.f/dist);
//        return result;
//    }
//
//    public class DerelictData
//    {
//        public ShipVariantAPI variant;
//        public Vector2f normalizedOffset;
//        public DerelictData(ShipVariantAPI variant, Vector2f normalizedOffset)
//        {
//            this.variant = variant;
//            this.normalizedOffset = new Vector2f(normalizedOffset);
//        }
//    }
//
//    public class DebrisFieldData
//    {
//        public float density;
//        public Vector2f normalizedOffset;
//
//        public DebrisFieldData(float density, Vector2f normalizedOffset)
//        {
//            this.density = density;
//            this.normalizedOffset = normalizedOffset;
//        }
//    }
//
//    public class CombatFleetData
//    {
//        public int fleetSize = 0;
//        public List<FactionAPI> factions = new ArrayList<>();
//        public List<FleetMemberAPI> ships = new ArrayList<>();
//        public List<ShipAPI> deployedShips = new ArrayList<>();
//        public List<FleetMemberAPI> reserveShips = new ArrayList<>();
//        public Vector2f deployedAveragePos = new Vector2f();
//    }
}
