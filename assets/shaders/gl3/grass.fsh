#version 330

#ifdef GL_ES
    #define LOWP lowp
    precision mediump float;
    #else
    #define LOWP
#endif

in vec4 v_color;
in vec4 v_grassColor;
in vec2 v_texCoords;

uniform sampler2D u_texture;
uniform int use;

out vec4 fragColor;

void main() {
    vec4 tileColor = texture(u_texture, v_texCoords);
    float ao = v_grassColor.a;

    float r = tileColor.r;
    float g = tileColor.g;
    float b = tileColor.b;

    bool dirtCheck = r < 0.22 && g < 0.16 && b < 0.15;

    if(tileColor.a > 0.0 && !dirtCheck) {
        r *= v_grassColor.r;
        g *= v_grassColor.g;
        b *= v_grassColor.b;
    }

    if(ao > 0.0) {
        // Has ambient occlusion
        float factor = 0.275;
        float sr = r - (r * factor * ao);
        float sg = g - (g * factor * ao);
        float sb = b - (b * factor * ao);
        fragColor = vec4(sr, sg, sb, tileColor.a) * v_color;
    } else {
        // No ambient occlusion
        fragColor = vec4(r, g, b, tileColor.a) * v_color;
    }
}