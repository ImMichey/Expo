#version 330

#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

in LOWP vec4 v_color;
in vec2 v_texCoords;
in float v_texture_index;

uniform sampler2DArray u_texturearray;

uniform float u_delta;

out vec4 fragColor;

void main() {
    vec4 c = texture(u_texturearray, vec3(v_texCoords, v_texture_index));

    float coord = (v_texCoords.x + v_texCoords.y * 2.0) * 314.159;
    float lerp_pos = clamp((cos((u_delta - coord) * 0.125) - 0.984375) * 64, 0.0, 1.0);

    c.rgb = mix(c.rgb, c.rgb * 1.5, lerp_pos * 1.25);
    fragColor = v_color * c;
}