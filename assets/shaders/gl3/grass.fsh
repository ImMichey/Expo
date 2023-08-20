#version 330

#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

in LOWP vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture;
uniform int use;

out vec4 fragColor;

void main() {
    vec4 tileColor = texture(u_texture, v_texCoords);
    vec4 data = v_color;
    bool defaultRenderMode = data.b == 1.0;

    if(defaultRenderMode) {
        fragColor = tileColor;
    } else {
        float grassColor = data.r;
        float ambientOcclusion = data.g;

        if(grassColor > 0.0) {
            tileColor.r = tileColor.r - (tileColor.r * grassColor * 1.5);
            tileColor.g = tileColor.g - (tileColor.g * grassColor * 1.0);
            tileColor.b = tileColor.b - (tileColor.b * grassColor * 2.0);
        }

        if(ambientOcclusion > 0.0 && tileColor.a > 0.0) {
            float factor = 0.25;
            float _r = tileColor.r - (tileColor.r * data.a * factor);
            float _g = tileColor.g - (tileColor.g * data.a * factor);
            float _b = tileColor.b - (tileColor.b * data.a * factor);
            fragColor = vec4(_r, _g, _b, 1.0);
        } else {
            fragColor = tileColor;
        }
    }
}
    // bc * v_color
    // v_color = 0.0, 0.0, 0.0, [0-1]

    /*
    if(v_color.a > 0.1 || bc.g < 0.2) {
        fragColor = bc;
    } else {
        if(use == 1) {
            fragColor = bc;
        } else {
            float grassColor = v_color.r;
            float ambientOcclusion = v_color.g;

            float baseR = bc.r;
            float baseG = bc.g;
            float baseB = bc.b;

            fragColor = vec4(baseR - (baseR * grassColor * 1.5), baseG - (baseG * grassColor * 1), baseB - (baseB * grassColor * 2), bc.a);
        }
    }
    */