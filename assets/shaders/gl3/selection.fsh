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

uniform float u_progress;
uniform float u_pulseStrength;
uniform float u_pulseMin;

out vec4 fragColor;

void main() {
    vec4 c = texture(u_texture, v_texCoords);
    c.rgb = mix(c.rgb * u_pulseMin, c.rgb * u_pulseStrength, u_progress);
    fragColor = c;
}