package data.scripts.Combat;

import com.fs.starfarer.api.combat.ViewportAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CombatBackgroundLayer
{
    private List<BackgroundEntity> entityList = new ArrayList<>();
    private Color tint;
    float[] layerTintRGB = new float[4];
    private float parallaxScale;
    private float scale;

    public CombatBackgroundLayer(Color tint, float parallaxScale, float scale)
    {
        this.tint = tint;
        tint.getRGBComponents(layerTintRGB);

        this.parallaxScale = parallaxScale;
        this.scale = scale;

        entityList.clear();
    }

    public void AddEntity(BackgroundEntity entity)
    {
        entityList.add(entity);

        entity.SetTint(tint);
        entity.SetParallaxScale(parallaxScale);
        entity.SetScale(scale);
    }

    float[] customTintRGB = new float[4];
    public void AddEntity(BackgroundEntity entity, Color color, float customScale)
    {
        entityList.add(entity);

        color.getRGBComponents(customTintRGB);
        entity.SetTint(new Color(customTintRGB[0] * layerTintRGB[0],
                customTintRGB[1] * layerTintRGB[1],
                customTintRGB[2] * layerTintRGB[2]));

        entity.SetParallaxScale(parallaxScale);
        entity.SetScale(scale * customScale);
    }

    public void Render(ViewportAPI view, Vector2f viewPos, float time)
    {
        for (Iterator<BackgroundEntity> iter = entityList.iterator(); iter.hasNext(); )
        {
            BackgroundEntity entry = iter.next();

            entry.Render(view, viewPos, time);
        }
    }
}
