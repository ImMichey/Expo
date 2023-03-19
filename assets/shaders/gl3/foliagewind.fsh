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
uniform int u_selected;
uniform float u_progress;

out vec4 fragColor;

void main() {
    vec4 c = texture(u_texturearray, vec3(v_texCoords, v_texture_index));

    if(u_selected == 1) {
        c.rgb = mix(c.rgb * 1.05, c.rgb * 1.25, u_progress);
    }

    fragColor = v_color * c;
}