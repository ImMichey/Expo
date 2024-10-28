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

out vec4 fragColor;

void main() {
    vec4 cl = texture(u_texturearray, vec3(v_texCoords, v_texture_index));

    if(cl.a > 0.0) {
        fragColor = v_color;
    } else {
        fragColor = vec4(0.0);
    }
}