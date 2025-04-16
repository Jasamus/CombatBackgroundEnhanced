package data.scripts.Combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.Util.GenMath;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_REPLACE;


public class BackgroundEntity
{
    private static boolean bSkipStencil = false;

    public static void ReloadSettings(JSONObject options)
    {
        bSkipStencil = options.optBoolean("SkipStencil", false);
    }

    SpriteAPI sprite;
    Vector2f location;
    float size;
    float facing;
    float angularVelocity;
    Color color;
    float parallaxScale;
    float sizeScale;

    List<BackgroundEntityChild> underEntities = new ArrayList<>();
    List<BackgroundEntityChild> childEntities = new ArrayList<>();

    List<BackgroundEntityChild> overlayEntities = new ArrayList<>();

    class BackgroundEntityChild
    {
        SpriteAPI sprite;
        Vector2f offset;
        float facing;

        public BackgroundEntityChild(String spriteName, Vector2f offset, float scale, float facing)
        {
            sprite = Global.getSettings().getSprite(spriteName);

            sprite.setWidth(sprite.getWidth() * scale);
            sprite.setHeight(sprite.getHeight() * scale);

            this.offset = new Vector2f(offset);
            this.offset.scale(scale);
            this.facing = facing;
        }

        public BackgroundEntityChild(String spriteCategory, String spriteName, Vector2f offset, float scale, float facing)
        {
            sprite = Global.getSettings().getSprite(spriteCategory, spriteName);

            sprite.setWidth(sprite.getWidth() * scale);
            sprite.setHeight(sprite.getHeight() * scale);

            this.offset = new Vector2f(offset);
            this.offset.scale(scale);
            this.facing = facing;
        }

        public BackgroundEntityChild(SpriteAPI sprite, Vector2f offset, float scale, float facing)
        {
            this.sprite = sprite;

            sprite.setWidth(sprite.getWidth() * scale);
            sprite.setHeight(sprite.getHeight() * scale);

            this.offset = new Vector2f(offset);
            this.offset.scale(scale);
            this.facing = facing;
        }

        public BackgroundEntityChild(String spriteName, Vector2f offset, float scale, float facing, Vector2f center)
        {
            sprite = Global.getSettings().getSprite(spriteName);

            // Use the original sprite size
            float centerX = center.x * scale;
            float centerY = center.y * scale;

            sprite.setWidth(sprite.getWidth() * scale);
            sprite.setHeight(sprite.getHeight() * scale);

            sprite.setCenter(centerX, centerY);

            this.offset = new Vector2f(offset);
            this.offset.scale(scale);
            this.facing = facing;
        }

        public BackgroundEntityChild(String spriteName, Vector2f offset, float scale, float facing, float centerXPercent, float centerYPercent)
        {
            sprite = Global.getSettings().getSprite(spriteName);

            sprite.setWidth(sprite.getWidth() * scale);
            sprite.setHeight(sprite.getHeight() * scale);

            sprite.setCenter(centerXPercent * sprite.getWidth(), centerYPercent * sprite.getHeight());

            this.offset = new Vector2f(offset);
            this.offset.scale(scale);
            this.facing = facing;
        }

        public Vector2f GetSpriteSize()
        {
            return new Vector2f(sprite.getWidth(), sprite.getHeight());
        }

        public void SetTint(Color color)
        {
            sprite.setColor(color);
        }

        public void Render(Vector2f parentPos, float parentFacing)
        {
            Vector2f rotOffset = new Vector2f(offset);

            GenMath.VecRotate(rotOffset, parentFacing);

            sprite.setAngle(parentFacing + facing);
            sprite.renderAtCenter(parentPos.x + rotOffset.x, parentPos.y + rotOffset.y);
        }
    }

    public BackgroundEntity(String spriteCategory, String spriteName, Vector2f location, float facing, float angularVelocity)
    {
        sprite = Global.getSettings().getSprite(spriteCategory, spriteName);
        sprite.setCenter(sprite.getWidth()/2f, sprite.getHeight()/2f);

        this.location = new Vector2f(location);
        this.facing = facing;
        this.angularVelocity = angularVelocity;

        this.size = Math.max(sprite.getWidth(), sprite.getHeight()) / 2f;
    }

    public BackgroundEntity(String spriteName, Vector2f location, float centerX, float centerY, float facing, float angularVelocity)
    {
        sprite = Global.getSettings().getSprite(spriteName);

        sprite.setCenter(centerX, centerY);

        this.location = new Vector2f(location.x, location.y);
        this.facing = facing;
        this.angularVelocity = angularVelocity;

        this.size = Math.max(sprite.getWidth(), sprite.getHeight()) / 2f;
    }

    public void SetTint(Color color)
    {
        this.color = color;
        sprite.setColor(color);

        for (BackgroundEntityChild childEntity : childEntities)
        {
            childEntity.SetTint(color);
        }

        for(BackgroundEntityChild overlayEntity : overlayEntities)
        {
            overlayEntity.SetTint(color);
        }
    }

    public void SetParallaxScale(float parallaxScale)
    {
        this.parallaxScale = parallaxScale;
    }

    public void SetScale(float scale)
    {
        this.sizeScale = scale;
        sprite.setWidth(sprite.getWidth() * scale);
        sprite.setHeight(sprite.getHeight() * scale);
        sprite.setCenter(sprite.getCenterX() * scale,sprite.getCenterY() * scale);
    }

    public BackgroundEntityChild AddUnderSprite(String spriteName, Vector2f offset, float facing,Vector2f center)
    {
        BackgroundEntityChild under = new BackgroundEntityChild(spriteName, offset, sizeScale, facing, center);
        under.SetTint(color);
        underEntities.add(under);

        return under;
    }

    public BackgroundEntityChild AddChildSprite(String spriteName, Vector2f offset, float facing)
    {
        BackgroundEntityChild child = new BackgroundEntityChild(spriteName, offset, sizeScale, facing);
        child.SetTint(color);
        childEntities.add(child);

        return child;
    }

    public BackgroundEntityChild AddChildSprite(String spriteName, Vector2f offset, float facing, Vector2f center)
    {
        BackgroundEntityChild child = new BackgroundEntityChild(spriteName, offset, sizeScale, facing, center);
        child.SetTint(color);
        childEntities.add(child);

        return child;
    }

    public BackgroundEntityChild AddChildSprite(String spriteName, Vector2f offset, float facing, float centerXPercent, float centerYPercent)
    {
        BackgroundEntityChild child = new BackgroundEntityChild(spriteName, offset, sizeScale, facing, centerXPercent, centerYPercent);
        child.SetTint(color);
        childEntities.add(child);

        return child;
    }

    public void AddOverlaySprite(String spriteName, Vector2f offset, float facing)
    {
        BackgroundEntityChild overlay = new BackgroundEntityChild(spriteName, offset, sizeScale, facing);
        overlay.SetTint(color);
        overlayEntities.add(overlay);
    }

    public void AddOverlaySprite(String spriteName, Vector2f offset, float facing, float centerXPercent, float centerYPercent)
    {
        BackgroundEntityChild overlay = new BackgroundEntityChild(spriteName, offset, sizeScale, facing, centerXPercent, centerYPercent);
        overlay.SetTint(color);
        overlayEntities.add(overlay);
    }

    public void AddOverlaySprite(String spriteCategory, String spriteName, Vector2f offset, float facing)
    {
        BackgroundEntityChild overlay = new BackgroundEntityChild(spriteCategory, spriteName, offset, sizeScale, facing);
        overlay.SetTint(color);
        overlayEntities.add(overlay);
    }

    public void AddOverlaySprite(SpriteAPI sprite, Vector2f offset, float facing)
    {
        BackgroundEntityChild overlay = new BackgroundEntityChild(sprite, offset, sizeScale, facing);
        overlay.SetTint(color);
        overlayEntities.add(overlay);
    }

    public Vector2f GetLocation() { return location; }
    public float GetFacing() { return facing; }
    public Vector2f GetSpriteSize()
    {
        return new Vector2f(sprite.getWidth(), sprite.getHeight());
    }
    public Color GetTint() { return this.color; }

    public void Render(ViewportAPI view, Vector2f viewPos, float time)
    {
        Vector2f scaledLocation = new Vector2f(location);
        scaledLocation.translate(-viewPos.x, -viewPos.y);
        scaledLocation.scale(parallaxScale);
        scaledLocation.translate(viewPos.x, viewPos.y);

        boolean useStencil = !bSkipStencil && !overlayEntities.isEmpty();

        if (!view.isNearViewport(scaledLocation, size + 100f))
            return;

        float frameFacing = facing + (angularVelocity * time % 360f);

        for(BackgroundEntityChild underEntity : underEntities)
        {
            underEntity.Render(scaledLocation, frameFacing);
        }

        sprite.setAngle(frameFacing);
        if(useStencil)
            SetupStencil();

        sprite.renderAtCenter(scaledLocation.x, scaledLocation.y);

        for (BackgroundEntityChild childEntity : childEntities)
        {
            childEntity.Render(scaledLocation, frameFacing);
        }

        if(useStencil)
            EnableStencil();

        for(BackgroundEntityChild overlayEntity : overlayEntities)
        {
            overlayEntity.Render(scaledLocation, frameFacing);
        }

        if(useStencil)
            DisableStencil();
    }

    void SetupStencil()
    {
        glClear(GL_STENCIL_BUFFER_BIT);

        glEnable(GL_STENCIL_TEST);

        float threshold = 0.01f;
        glAlphaFunc(GL_GEQUAL, threshold);
        glEnable(GL_ALPHA_TEST);

        glStencilFunc(GL_ALWAYS, 1, 0xff);
        glStencilMask(0xff);
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
    }

    void EnableStencil()
    {
        glDisable(GL_ALPHA_TEST);

        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        glColorMask(true,true,true,true);
        glStencilFunc(GL_EQUAL, 1, 0xff);
    }

    void DisableStencil()
    {
        glDisable(GL_STENCIL_TEST);
    }
}
