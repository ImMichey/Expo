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

out vec4 fragColor;

void main() {
    vec2 center = vec2(0.5, 0.5);
    float dist = distance(center, v_texCoords);

    float texAlpha = texture(u_texture, v_texCoords).a;
    float multiplier = v_color.a;

    fragColor = vec4(0.0, 0.0, 0.0, texAlpha * multiplier * (1.0 - dist * 2));
}