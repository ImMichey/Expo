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
    vec4 base = texture(u_texture, v_texCoords);
    float r = base.r;
    float g = base.g;
    float b = base.b;
    float a = base.a;

    fragColor = base;

    if(a > 0.0) {
        //fragColor = vec4(0.0, 0.5, 1.0, 0.75);
        //fragColor = vec4(0.0);
    } else {
        
    }
}