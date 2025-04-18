package data.scripts;

import java.util.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.*;

import com.fs.starfarer.api.util.Misc;
import data.scripts.Combat.*;

import data.scripts.Util.ChainPoint;
import data.scripts.Util.GenMath;
import org.lwjgl.util.vector.Vector2f;

public class CombatBackgroundTerrain extends BaseEveryFrameCombatPlugin
{
    Random ran = new Random();
    CombatBackground background= new CombatBackground();
    BackgroundAsteroidSpawner asteroidSpawner;
    BackgroundShipSpawner shipSpawner;
    BackgroundFleetSpawner fleetSpawner;
    CampaignTerrainAnalyzer terrainAnalyzer;
    float mapWidth;
    float mapHeight;
    float mapWidthHalf;
    float mapHeightHalf;
    float mapWidthThird;
    float mapHeightThird;

    private float elapsedTime = 0f;

    List<ShipVariantAPI> spawnedStations = new ArrayList<>();

    @Override
    public void init(CombatEngineAPI engine)
    {
        // Only run when in Combat or Simulator
        //if(Global.getCurrentState() != GameState.COMBAT && !Global.getCombatEngine().isSimulation())
        if(!Global.getCombatEngine().isInCampaign() && !Global.getCombatEngine().isInCampaignSim())
            return;

        mapWidth = Global.getCombatEngine().getMapWidth();
        mapHeight = Global.getCombatEngine().getMapHeight();
        mapWidthHalf = mapWidth / 2f;
        mapHeightHalf = mapHeight / 2f;
        mapWidthThird = mapWidth / 3f;
        mapHeightThird = mapHeight / 3f;

        //Creates our layered rendering script
        CombatLayeredRenderingPlugin layerRenderer = new CombatBackgroundTerrainRenderer(this);
        engine.addLayeredRenderingPlugin(layerRenderer);

        // Setup generator classes
        GenMath.SetRandom(ran);
        asteroidSpawner = new BackgroundAsteroidSpawner(background, ran);
        shipSpawner = new BackgroundShipSpawner(background, ran);
        fleetSpawner = new BackgroundFleetSpawner(background, ran, shipSpawner);
        terrainAnalyzer = new CampaignTerrainAnalyzer();

        // Spawn in generic visuals
        SpawnAmbientVisuals();
    }


    //*************************************************
    void SpawnAmbientVisuals()
    {
        //System.out.println("!! Map Width: " + mapWidth + ", Map Height: " + mapHeight);

//        ShipVariantAPI variant1 = Global.getSettings().getVariant("swp_cathedral_for");
//        shipSpawner.GenerateHulk(1,variant1, 000f, -1200f, ran.nextFloat(), 0f, false);
//
//        ShipVariantAPI variant2 = Global.getSettings().getVariant("onslaught_mk1_Ancient");
//        shipSpawner.GenerateHulk(1,variant2, 800f, -1200f, ran.nextFloat(), 0f, false);
////
//
//        ShipVariantAPI variant3 = Global.getSettings().getVariant("station3_midline_Standard");
//        shipSpawner.GenerateHulk(1,variant3, 1200f, 0f, ran.nextFloat(), 0f, false);
//
//        ShipVariantAPI variant4 = Global.getSettings().getVariant("station3_hightech_Standard");
//        shipSpawner.GenerateHulk(1,variant4, 0f, 0f, ran.nextFloat(), 0f, false);
//
//        ShipVariantAPI variant5 = Global.getSettings().getVariant("station3_Standard");
//        shipSpawner.GenerateHulk(1,variant5, -1200f, 0f, ran.nextFloat(), 0f, false);

//        ShipVariantAPI variant2 = Global.getSettings().getVariant("buffalo2_Fighter_Support");
//        shipSpawner.GenerateHulk(0,variant2, 0f, 0f, 0f, 0f, false);

        // Spawn Inner Ring
        int innerCount = ran.nextInt(1) + 2;
        SpawnVisualsRing(innerCount, mapWidthHalf * 0.3f, 0.3f, 1200f);
        // Spawn Outer Ring
        int outerCount = ran.nextInt(3) + 3;
        SpawnVisualsRing(outerCount, mapWidthHalf * 0.7f, 0.3f, 1200f);
    }

    void SpawnVisualsRing(int count, float radius, float variance, float offset)
    {
        float startAngle = GenMath.RandFacing();
        //float startAngle = 0f;

        for(int i = 0; i < count; i++)
        {
            float percentage = ((float)i + 0.5f) / count;
            float alignedVariance = ((ran.nextFloat() - 0.5f) * variance)/count;

            Vector2f dir = Misc.getUnitVectorAtDegreeAngle((360f * (percentage + alignedVariance) + startAngle) % 360f);

            Vector2f pos = new Vector2f(dir);
            pos.scale(radius);

            float perpXPos = dir.x * (ran.nextFloat() - 0.5f) * offset;
            float perpYPos = dir.y * (ran.nextFloat() - 0.5f) * offset;

            SpawnAmbientElement(pos.x + perpXPos, pos.y + perpYPos );
        }
    }

    void SpawnAmbientElement(float xPos, float yPos)
    {
        //System.out.println("!! SpawnElement X: " + xPos + ", Y: " + yPos);
        float randResult = ran.nextFloat();
        if(randResult >= 0.6f)
        {
            SpawnAmbientFleet(xPos, yPos);
        }
        else
        {
            if(!terrainAnalyzer.IsInHyperSpace())
                SpawnAmbientAsteroidField(xPos, yPos);
        }
    }

    final int SPAWN_FLEET_BASE_SIZE = 60;
    final int SPAWN_FLEET_VARIABLE_SIZE = 240;
    final double SPAWN_FLEET_SIZE_BIAS = 1.35;
    final float SPAWN_STATION_CHANCE = 0.08f;
    void SpawnAmbientFleet(float xPos, float yPos)
    {
        List<FactionAPI> nearbyFactions = terrainAnalyzer.GetNearbyFactions();
        if(nearbyFactions.isEmpty())
            return;

        FactionAPI selectedFaction = nearbyFactions.get(ran.nextInt(nearbyFactions.size()));
        float randSize = ran.nextFloat();
        float randSizeScaled = (float)Math.pow(randSize, SPAWN_FLEET_SIZE_BIAS);
        int fleetSize = (int)(SPAWN_FLEET_VARIABLE_SIZE * randSizeScaled) + SPAWN_FLEET_BASE_SIZE;
        //System.out.println("!! Fleet size: " + randSize + ", size Scaled: " + randSizeScaled + ", DP: " + fleetSize);

        // If the faction has a station system, small chance of spawning a hulk of the same station.
        ShipVariantAPI stationVariant = terrainAnalyzer.GetNearbyStationForFaction(selectedFaction);
        if(stationVariant != null && !spawnedStations.contains(stationVariant) && ran.nextFloat() < SPAWN_STATION_CHANCE)
        {
            shipSpawner.GenerateHulk(ran.nextInt(5), stationVariant, xPos, yPos, GenMath.RandFacing(), 0f, false);
            // Prevent a station type from spawning multiple times
            spawnedStations.add(stationVariant);
        }
        else
            fleetSpawner.SpawnHulkField(selectedFaction, fleetSize, xPos, yPos);
    }

    void SpawnAmbientAsteroidField(float xPos, float yPos)
    {
        float startAngle = GenMath.RandFacing();

        float variance = 0.5f;
        float radius = 0f;

        float startingDensity = 0f;
        int chainCount = 0;

        //System.out.println("!! Spawn Asteroid Field");
        boolean useAsteroidCircle = GenMath.IsTrue(0.2f);
        if(useAsteroidCircle)
        {
            startingDensity = GenMath.LerpRand(1.75f, 2.25f);
            chainCount = ran.nextInt(3) + 2;
            radius = GenMath.LerpRand(650f, 900f);

//            System.out.println("!! Spawn Asteroid Circle radius: " + radius + ", density: " + startingDensity);
            asteroidSpawner.GenerateAsteroidFieldCircle(xPos, yPos, startAngle, radius, variance, radius * 2.5f, startingDensity);
        }
        else
        {
            startingDensity = GenMath.LerpRand(2.25f, 2.75f);
            chainCount = ran.nextInt(3) + 1;
        }

        for(int i = 0; i < chainCount; i++)
        {
            float percentage = ((float)i + 0.5f) / chainCount;
            float alignedVariance = ((ran.nextFloat() - 0.5f) * variance)/chainCount;

            float facing = (360f * (percentage + alignedVariance) + startAngle) % 360f;
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(facing);

            Vector2f pos = new Vector2f(dir);
            pos.scale(radius);

//            System.out.println("!! Spawn Asteroid Chain Starting Density: " + startingDensity);
            SpawnAsteroidChain(xPos + pos.x, yPos + pos.y, facing, startingDensity);
        }
    }

    void SpawnAsteroidChain(float xPos, float yPos, float facing, float startingDensity)
    {
        ChainPoint chainPoint = new ChainPoint(xPos, yPos, facing);
        float density = startingDensity;

        while(density > 0.0f)
        {
            float shouldCurve = ran.nextFloat();

//            System.out.println("!! Chain Element Density: " + density + ", isCurve: " + (shouldCurve > 0.5f ? "true" : "false"));
            if(shouldCurve > 0.5f)
                chainPoint = asteroidSpawner.GenerateAsteroidFieldArc(chainPoint, GenMath.LerpRand(1000f, 1800f), GenMath.LerpRandFlip(15f, 60f), 0.5f, 500f * density, density);
            else
                chainPoint = asteroidSpawner.GenerateAsteroidFieldLine(chainPoint, GenMath.LerpRand(550f, 900f), 0.5f, 500f * density, density);

            density -= GenMath.LerpRand(0.4f, 0.85f);

            boolean split = GenMath.IsTrue(0.25f);
            if(split && density > 0.f)
                SpawnAsteroidChain(chainPoint.x, chainPoint.y, chainPoint.angle + GenMath.LerpRandFlip(30f,90f), density);
        }
    }

    //******************************
    void render(CombatEngineLayers layer, ViewportAPI view)
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }

        float amount = 0;
        if (!engine.isPaused()) {
            amount = engine.getElapsedInLastFrame();
        }
        elapsedTime += amount;
        Vector2f viewPos = view.getCenter();

        background.Render(view, viewPos, elapsedTime);
    }
}

//********************************************************************
// Render Plugin
class CombatBackgroundTerrainRenderer extends BaseCombatLayeredRenderingPlugin {
    private CombatBackgroundTerrain parentPlugin;

    //Constructor
    CombatBackgroundTerrainRenderer(CombatBackgroundTerrain parentPlugin) {
        this.parentPlugin = parentPlugin;
    }

    //Render function; just here to time rendering and tell the main loop to run with a specific layer
    @Override
    public void render(CombatEngineLayers layer, ViewportAPI view) {
        //Initial checks to see if required components exist
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }

        //Calls our parent plugin's rendering function
        parentPlugin.render(layer, view);
    }

    //We render everywhere, and on all layers (since we can't change these at runtime)
    @Override
    public float getRenderRadius() {
        return 999999999999999f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_PLANETS);
    }
}