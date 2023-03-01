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
uniform sampler2D u_displace;
uniform float u_time;
uniform vec2 u_resolution;
uniform float u_zoom;

out vec4 fragColor;

const float magnitude = 0.5;
const float speedX = 0.5;
const float speedY = 0.5;

void main() {
    //float distortion = texture(u_texture2, v_texCoords * u_distortion + u_time).x;
    //fragColor = texture(u_texture, vec2(v_texCoords.x - distortion, v_texCoords.y));

    //vec2 test = gl_FragCoord.xy / u_resolution.xy;

    //vec2 disp = texture(u_displace, 0.1).xy;
    //disp = ((disp * 2) - 1) * magnitude;

    vec2 dist = texture(u_displace, vec2(u_time)).xy;

    fragColor = v_color * texture(u_texture, v_texCoords + dist);
}