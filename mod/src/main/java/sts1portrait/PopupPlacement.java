package sts1portrait;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;

/** Fits native popup graphics and pointer coordinates into the same visible rectangle. */
final class PopupPlacement {
    private final Matrix4 original = new Matrix4();
    private final Matrix4 moved = new Matrix4();
    private float x, y, scale = 1f;

    void reset() {
        x = y = 0f;
        scale = 1f;
    }

    void placeWithin(float centerX, float centerY, float width, float height,
            float left, float bottom, float right, float top) {
        scale = Math.min(1f, Math.min((right - left) / width, (top - bottom) / height));
        float halfWidth = width * scale * .5f;
        float halfHeight = height * scale * .5f;
        x = Math.max(left + halfWidth, Math.min(right - halfWidth, centerX)) - centerX * scale;
        y = Math.max(bottom + halfHeight, Math.min(top - halfHeight, centerY)) - centerY * scale;
    }

    float mapX(float value) { return value * scale + x; }
    float mapY(float value) { return value * scale + y; }
    float unmapX(float value) { return (value - x) / scale; }
    float unmapY(float value) { return (value - y) / scale; }
    float scale() { return scale; }

    void begin(SpriteBatch batch) {
        original.set(batch.getTransformMatrix());
        if (x != 0f || y != 0f || scale != 1f) {
            batch.setTransformMatrix(moved.set(original).translate(x, y, 0f).scale(scale, scale, 1f));
        }
    }

    void end(SpriteBatch batch) {
        batch.setTransformMatrix(original);
    }
}
