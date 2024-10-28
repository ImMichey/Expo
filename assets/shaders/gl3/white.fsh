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
    vec4 cl = texture(u_texture, v_texCoords);

    if(cl.a > 0.0) {
        fragColor = vec4(1.0, 1.0, 1.0, cl.a * v_color.a);
    } else {
        fragColor = vec4(0.0);
    }
}