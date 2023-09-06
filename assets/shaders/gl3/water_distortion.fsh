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

uniform vec2 u_cameraPos;
uniform vec2 u_screenSize;
uniform float u_time;
uniform float u_waterSkewX;
uniform float u_waterSkewY;

out vec4 fragColor;

void main() {
    vec2 c = v_texCoords.xy;
    vec4 color = texture(u_texture, v_texCoords);

    vec2 v = vec2(1.0 / u_screenSize.x, 1.0 / u_screenSize.y);
    vec2 coords = vec2(c.x / v.x + u_cameraPos.x, c.y / v.y + u_cameraPos.y);

    if(color.a > 0.01) {
        vec4 old = color;
        color = texture(u_texture, c + vec2(sin(u_time / u_waterSkewX + coords.y / u_waterSkewY) * v.x, 0.0));

        if(color.a < 0.01) {
            color = old;
            color.a = 0.0;
        } else {
            color.a = 1.0;
        }
    }

    fragColor = color;
}