package dev.michey.expo.logic.entity.arch;

import dev.michey.expo.render.RenderContext;

public interface SelectableEntity {

    void renderSelected(RenderContext rc, float delta);

    float[] interactionPoints();

}
