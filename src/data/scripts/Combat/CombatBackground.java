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

    final float parallaxMinOffset = 0.92f;
    final float parallaxMaxOffset = 0.74f;

    final float scaleMin = 0.9f;
    final float scaleMax = 0.7f;

    public CombatBackground()
    {
        backgroundLayers.add(new CombatBackgroundLayer(new Color(0.15f, 0.15f,0.15f),
                parallaxMaxOffset, scaleMax));

        backgroundLayers.add(new CombatBackgroundLayer(new Color(0.22f, 0.22f,0.22f),
                GenMath.Lerp(parallaxMinOffset, parallaxMaxOffset, 0.75f),
                GenMath.Lerp(scaleMin, scaleMax, 0.75f)));

        backgroundLayers.add(new CombatBackgroundLayer(new Color(0.30f, 0.30f,0.30f),
                GenMath.Lerp(parallaxMinOffset, parallaxMaxOffset, 0.50f),
                GenMath.Lerp(scaleMin, scaleMax, 0.5f)));

        backgroundLayers.add(new CombatBackgroundLayer(new Color(0.36f, 0.36f,0.36f),
                GenMath.Lerp(parallaxMinOffset, parallaxMaxOffset, 0.25f),
                GenMath.Lerp(scaleMin, scaleMax, 0.25f)));

        backgroundLayers.add(new CombatBackgroundLayer(new Color(0.42f, 0.42f,0.42f),
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
