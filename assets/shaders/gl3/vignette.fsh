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
uniform vec2 u_resolution;

const float outerRadius = 0.8;
const float innerRadius = 0.2;
const float intensity = 0.65;
//const float outerRadius = 0.7;
//const float innerRadius = 0.4;
//const float intensity = 0.3;

out vec4 fragColor;

void main() {
    vec4 color = v_color * texture(u_texture, v_texCoords);

    vec2 relativePosition = gl_FragCoord.xy / u_resolution - 0.5;
    float len = length(relativePosition);
    float vignette = smoothstep(outerRadius, innerRadius, len);

    color.rgb = mix(color.rgb, color.rgb * vignette, intensity);
    fragColor = color;
}