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
uniform sampler2D u_lookup;

out vec4 fragColor;

void main() {
    vec4 lookup = texture(u_lookup, v_texCoords);

    if(lookup.a > 0.0) {
        fragColor = texture(u_texture, v_texCoords) * v_color;
    } else {
        fragColor = vec4(0.0);
    }
}