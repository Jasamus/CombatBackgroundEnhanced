package data.scripts.Combat;

import com.fs.starfarer.api.util.Misc;
import data.scripts.Util.ChainPoint;
import data.scripts.Util.GenMath;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BackgroundAsteroidSpawner
{
    CombatBackground background;
    Random ran;

    final float ASTEROID_DENSITY_SMALL_MIN = 150.f;
    final float ASTEROID_DENSITY_SMALL_MAX = 105.f;
    final float ASTEROID_DENSITY_MEDIUM_MIN = 225.f;
    final float ASTEROID_DENSITY_MEDIUM_MAX = 175.f;
    final float ASTEROID_DENSITY_LARGE_MIN = 500.f;
    final float ASTEROID_DENSITY_LARGE_MAX = 350.f;

    public BackgroundAsteroidSpawner(CombatBackground background, Random ran)
    {
        this.background = background;
        this.ran = ran;

        InitAsteroidGroups();
    }

    public void GenerateAsteroidFieldLine(float xPos, float yPos, float angle, float distance, float variance, float offset, float densityScale)
    {
        int largeCount = (int)Math.floor(distance / GenMath.Lerp(ASTEROID_DENSITY_LARGE_MIN, ASTEROID_DENSITY_LARGE_MAX, ran.nextFloat())* densityScale);
        int medCount = (int)Math.floor(distance / GenMath.Lerp(ASTEROID_DENSITY_MEDIUM_MIN, ASTEROID_DENSITY_MEDIUM_MAX, ran.nextFloat())* densityScale);
        int smallCount = (int)Math.floor(distance / GenMath.Lerp(ASTEROID_DENSITY_SMALL_MIN, ASTEROID_DENSITY_SMALL_MAX, ran.nextFloat())* densityScale);
        GenerateAsteroidLine(1,3, largeCount, 3, xPos, yPos, angle, distance, variance * 0.6f, offset * 0.5f,0.5f,5f);
        GenerateAsteroidLine(0,4, medCount, 2, xPos, yPos, angle, distance, variance * 0.8f, offset * 0.75f,5f,10f);
        GenerateAsteroidLine( 0,4, smallCount, 1, xPos, yPos, angle, distance, variance, offset,8f,15f);
    }

    public ChainPoint GenerateAsteroidFieldLine(ChainPoint startPoint, float distance, float variance, float offset, float densityScale)
    {
        Vector2f facing = Misc.getUnitVectorAtDegreeAngle(startPoint.angle);
        facing.scale(distance);

        GenerateAsteroidFieldLine(startPoint.x + (facing.x / 2f), startPoint.y + (facing.y / 2f), startPoint.angle, distance, variance, offset, densityScale);

        return new ChainPoint(startPoint.x + facing.x, startPoint.y + facing.y, startPoint.angle);
    }

    public void GenerateAsteroidFieldArc(float xPos, float yPos, float angle, float radius, float arcLength, float variance, float offset, float densityScale)
    {
        float circumference = radius * 6.28318f * (Math.abs(arcLength) / 360f);
        int largeCount = (int)Math.floor(circumference  / GenMath.Lerp(ASTEROID_DENSITY_LARGE_MIN, ASTEROID_DENSITY_LARGE_MAX, ran.nextFloat())* densityScale);
        int medCount = (int)Math.floor(circumference / GenMath.Lerp(ASTEROID_DENSITY_MEDIUM_MIN, ASTEROID_DENSITY_MEDIUM_MAX, ran.nextFloat())* densityScale);
        int smallCount = (int)Math.floor(circumference / GenMath.Lerp(ASTEROID_DENSITY_SMALL_MIN, ASTEROID_DENSITY_SMALL_MAX, ran.nextFloat())* densityScale);
        GenerateAsteroidArc(1,3, largeCount, 3, xPos, yPos, angle, radius, arcLength, variance * 0.5f, offset * 0.5f,0.5f,5f);
        GenerateAsteroidArc(0,4, medCount, 2, xPos, yPos, angle, radius, arcLength, variance * 0.75f, offset * 0.75f,5f,10f);
        GenerateAsteroidArc(0,4, smallCount, 1, xPos, yPos, angle, radius, arcLength, variance, offset,8f,15f);
    }

    public ChainPoint GenerateAsteroidFieldArc(ChainPoint startPoint, float radius, float arcLength, float variance, float offset, float densityScale)
    {
        float angleAdjust = arcLength > 0f ? -90f : 90f;
        float angle = startPoint.angle + angleAdjust;

        Vector2f startDir = Misc.getUnitVectorAtDegreeAngle(angle);
        startDir.scale(-radius);

        GenerateAsteroidFieldArc(startPoint.x + startDir.x, startPoint.y + startDir.y, angle, radius, arcLength, variance, offset, densityScale);

        angle = angle + arcLength;

        Vector2f endPoint = Misc.getUnitVectorAtDegreeAngle(angle);
        endPoint.scale(radius);
        endPoint.translate(startPoint.x + startDir.x, startPoint.y + startDir.y);

        return new ChainPoint(endPoint.x, endPoint.y, startPoint.angle + arcLength);
    }

    public void GenerateAsteroidFieldCircle(float xPos, float yPos, float angle, float radius, float variance, float offset, float densityScale)
    {
        GenerateAsteroidFieldArc(xPos, yPos, angle, radius, 360f, variance, offset, densityScale);
    }

    void GenerateAsteroidLine(int startLayer, int endLayer, int count, int size, float xPos, float yPos, float angle, float distance, float variance, float offset, float minAngularVelocity, float maxAngularVelocity)
    {
        Vector2f facing = Misc.getUnitVectorAtDegreeAngle(angle);
        Vector2f perpendicular = Misc.getUnitVectorAtDegreeAngle(angle + 90f);

        Vector2f startPos = new Vector2f(facing);
        startPos.scale(-distance/2f);
        startPos.translate(xPos, yPos);

        Vector2f alignedOffset = new Vector2f(facing);
        alignedOffset.scale(distance);

        Vector2f perpOffset = new Vector2f(perpendicular);
        perpOffset.scale(offset);

        for(int i = 0; i < count; i++)
        {
            float percentage = ((float)i + 0.5f) / count;
            float alignedVariance = ((ran.nextFloat() - 0.5f) * variance)/count;

            float alignedXPos = alignedOffset.x * (percentage + alignedVariance);
            float alignedYPos = alignedOffset.y * (percentage + alignedVariance);

            float perpXPos = perpOffset.x * (ran.nextFloat() - 0.5f);
            float perpYPos = perpOffset.y * (ran.nextFloat() - 0.5f);

            int layerIndex = startLayer;
            if(endLayer > startLayer)
                layerIndex = ran.nextInt(endLayer - startLayer) + startLayer;

            SpawnAsteroid(layerIndex, size, startPos.x + alignedXPos + perpXPos, startPos.y + alignedYPos + perpYPos, ran.nextFloat() * 360f, GenMath.LerpRandFlip(minAngularVelocity, maxAngularVelocity));
        }
    }

    void GenerateAsteroidArc(int startLayer, int endLayer, int count, int size, float xPosArcCenter, float yPosArcCenter, float startAngle, float radius, float arcLength, float variance, float offset, float minAngularVelocity, float maxAngularVelocity)
    {
        for(int i = 0; i < count; i++)
        {
            float percentage = ((float)i + 0.5f) / count;
            float alignedVariance = ((ran.nextFloat() - 0.5f) * variance)/count;

            Vector2f dir = Misc.getUnitVectorAtDegreeAngle((arcLength * (percentage + alignedVariance) + startAngle) % 360f);

            Vector2f pos = new Vector2f(dir);
            pos.scale(radius);
            pos.translate(xPosArcCenter, yPosArcCenter);

            float perpXPos = dir.x * (ran.nextFloat() - 0.5f) * offset;
            float perpYPos = dir.y * (ran.nextFloat() - 0.5f) * offset;

            int layerIndex = startLayer;
            if(endLayer > startLayer)
                layerIndex = ran.nextInt(endLayer - startLayer) + startLayer;

            SpawnAsteroid(layerIndex, size, pos.x + perpXPos, pos.y + perpYPos, ran.nextFloat() * 360f, GenMath.LerpRandFlip(minAngularVelocity, maxAngularVelocity));
        }
    }

    void SpawnAsteroid(int layerIndex, int size, float xPos, float yPos, float facing, float angularVelocity)
    {
        CombatBackgroundLayer layer = background.GetLayer(layerIndex);

        BackgroundEntity asteroid = new BackgroundEntity("CombatBackgroundTerrain", GetAsteroidSprite(size), new Vector2f(xPos, yPos), facing, angularVelocity);
        layer.AddEntity(asteroid, GetTintColor(), GenMath.LerpRand(0.7f,1.2f));
    }

    static final String ASTEROID_SPRITE_ERROR = "error_sprite";
    static final String ASTEROID_SMALL_SPRITE_1 = "asteroid_small1";
    static final String ASTEROID_SMALL_SPRITE_2 = "asteroid_small2";
    static final String ASTEROID_SMALL_SPRITE_3 = "asteroid_small3";
    static final String ASTEROID_MEDIUM_SPRITE_1 = "asteroid_medium1";
    static final String ASTEROID_MEDIUM_SPRITE_2 = "asteroid_medium2";
    static final String ASTEROID_MEDIUM_SPRITE_3 = "asteroid_medium3";
    static final String ASTEROID_MEDIUM_SPRITE_4 = "asteroid_medium4";
    static final String ASTEROID_LARGE_SPRITE_1 = "asteroid_large1";
    static final String ASTEROID_LARGE_SPRITE_2 = "asteroid_large2";
    static final String ASTEROID_LARGE_SPRITE_3 = "asteroid_large3";
    static final String ASTEROID_LARGE_SPRITE_4 = "asteroid_large4";

    List<List<String>> asteroidSprites = new ArrayList<>();

    void InitAsteroidGroups()
    {
        List<String> defaultDebris = new ArrayList<>(1);
        defaultDebris.add(ASTEROID_SPRITE_ERROR);

        List<String> asteroid1 = new ArrayList<>(1);
        asteroid1.add(ASTEROID_SMALL_SPRITE_1);
        asteroid1.add(ASTEROID_SMALL_SPRITE_2);
        asteroid1.add(ASTEROID_SMALL_SPRITE_3);

        List<String> asteroid2 = new ArrayList<>(1);
        asteroid2.add(ASTEROID_MEDIUM_SPRITE_1);
        asteroid2.add(ASTEROID_MEDIUM_SPRITE_2);
        asteroid2.add(ASTEROID_MEDIUM_SPRITE_3);
        asteroid2.add(ASTEROID_MEDIUM_SPRITE_4);

        List<String> asteroid3 = new ArrayList<>(3);
        asteroid3.add(ASTEROID_LARGE_SPRITE_1);
        asteroid3.add(ASTEROID_LARGE_SPRITE_2);
        asteroid3.add(ASTEROID_LARGE_SPRITE_3);
        asteroid3.add(ASTEROID_LARGE_SPRITE_4);

        asteroidSprites.add(defaultDebris);
        asteroidSprites.add(asteroid1);
        asteroidSprites.add(asteroid2);
        asteroidSprites.add(asteroid3);
    }

    String GetAsteroidSprite(int size)
    {
        List<String> asteroidGroup = asteroidSprites.get(size);
        return asteroidGroup.get(ran.nextInt(asteroidGroup.size()));
    }

    static final Color ASTEROID_TINT_1 = new Color(1f,1f,1f);
    static final Color ASTEROID_TINT_2 = new Color(0.935f,0.91f,0.955f);
    static final Color ASTEROID_TINT_3 = new Color(0.96f,0.94f,0.925f);
    static final Color ASTEROID_TINT_4 = new Color(0.9f,0.935f,0.93f);
    Color GetTintColor()
    {
        switch(ran.nextInt(4))
        {
            default:
            case 0:
                return ASTEROID_TINT_1;
            case 1:
                return ASTEROID_TINT_2;
            case 2:
                return ASTEROID_TINT_3;
            case 3:
                return ASTEROID_TINT_4;
        }
    }
}
