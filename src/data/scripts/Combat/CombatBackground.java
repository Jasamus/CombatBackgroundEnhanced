package data.scripts.Combat;

import com.fs.starfarer.api.combat.ViewportAPI;
import data.scripts.Util.GenMath;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CombatBackground
{
    private List<CombatBackgroundLayer> backgroundLayers = new ArrayList<>();

    float minBrightness = 0.20f;
    float maxBrightness = 0.46f;

    final float parallaxMinOffset = 0.92f;
    final float parallaxMaxOffset = 0.74f;

    final float scaleMin = 0.9f;
    final float scaleMax = 0.7f;

    public CombatBackground()
    {
        float brightness = minBrightness;
        backgroundLayers.add(new CombatBackgroundLayer(new Color(brightness, brightness,brightness),
                parallaxMaxOffset, scaleMax));

        brightness = GenMath.Lerp(maxBrightness, minBrightness, 0.75f);
        backgroundLayers.add(new CombatBackgroundLayer(new Color(brightness, brightness,brightness),
                GenMath.Lerp(parallaxMinOffset, parallaxMaxOffset, 0.75f),
                GenMath.Lerp(scaleMin, scaleMax, 0.75f)));

        brightness = GenMath.Lerp(maxBrightness, minBrightness, 0.5f);
        backgroundLayers.add(new CombatBackgroundLayer(new Color(brightness, brightness,brightness),
                GenMath.Lerp(parallaxMinOffset, parallaxMaxOffset, 0.50f),
                GenMath.Lerp(scaleMin, scaleMax, 0.5f)));

        brightness = GenMath.Lerp(maxBrightness, minBrightness, 0.25f);
        backgroundLayers.add(new CombatBackgroundLayer(new Color(brightness, brightness,brightness),
                GenMath.Lerp(parallaxMinOffset, parallaxMaxOffset, 0.25f),
                GenMath.Lerp(scaleMin, scaleMax, 0.25f)));

        brightness = maxBrightness;
        backgroundLayers.add(new CombatBackgroundLayer(new Color(brightness, brightness,brightness),
                parallaxMinOffset, scaleMin));
    }

    public CombatBackgroundLayer GetLayer(int index)
    {
        return backgroundLayers.get(index);
    }

    public boolean IsValidLayer(int index)
    {
        return index >= 0 && index < backgroundLayers.size();
    }

    public void Render(ViewportAPI view, Vector2f viewPos, float elapsedTime)
    {
        for(CombatBackgroundLayer backgroundLayer : backgroundLayers)
        {
            backgroundLayer.Render(view, viewPos, elapsedTime);
        }
    }
}
