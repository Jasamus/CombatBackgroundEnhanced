package data.scripts.Util;

import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

public class GenMath
{
    static Random ran;

    public static void SetRandom(Random newRandom) { ran = newRandom; }

    public static boolean IsTrue(float chance) { return ran.nextFloat() < chance; }
    public static float Lerp(float a, float b, float t) { return a * (1.0f - t) + (b * t); }
    public static float LerpRand(float a, float b) { return Lerp(a, b, ran.nextFloat()); }
    // Randomly selects between min and max while giving a random sign.
    public static float LerpRandFlip(float min, float max) { return LerpRand(min, max) * (ran.nextBoolean() ? 1.f : -1f); }
    public static int GetRandLayer() { return ran.nextInt(5); }
    public static int GetRandLayerNonEdges() {  return ran.nextInt(3) + 1; }
    public static float RandFacing() { return ran.nextFloat() * 360f; }
    public static Vector2f RandDir() { return Misc.getUnitVectorAtDegreeAngle(RandFacing()); }
    public static Vector2f RandOffset(float minDist, float maxDist) { return (Vector2f)RandDir().scale(LerpRand(minDist, maxDist)); }
    public static Vector2f RandOffset(float dist) { return (Vector2f)RandDir().scale(dist); }

    public static Vector2f VecRotate(Vector2f toRotate, float angle) { return VecRotate(toRotate, angle, toRotate); }

    public static Vector2f VecRotate(Vector2f toRotate, float angle, Vector2f dest)
    {
        if (angle == 0f)
        {
            return dest.set(toRotate);
        }

        angle = (float) Math.toRadians(angle);
        final float cos = (float) cos(angle), sin = (float) sin(angle);
        dest.set((toRotate.x * cos) - (toRotate.y * sin),
                (toRotate.x * sin) + (toRotate.y * cos));
        return dest;
    }

    public static double cos(double radians)
    {
        return sin(radians + Math.PI / 2.0);
    }

    public static double sin(double radians)
    {
        radians = reduceSinAngle(radians); // limits angle to between -PI/2 and +PI/2
        if (Math.abs(radians) <= Math.PI / 4.0)
            return Math.sin(radians);
        else
            return Math.cos(Math.PI / 2.0 - radians);
    }

    private static double reduceSinAngle(double radians)
    {
        radians %= Math.PI * 2.0; // put us in -2PI to +2PI space
        if (Math.abs(radians) > Math.PI)
        { // put us in -PI to +PI space
            radians -= (Math.PI * 2.0);
        }
        if (Math.abs(radians) > Math.PI / 2.0)
        {// put us in -PI/2 to +PI/2 space
            radians = Math.PI - radians;
        }

        return radians;
    }
}
