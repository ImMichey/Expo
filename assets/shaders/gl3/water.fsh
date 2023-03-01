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
uniform sampler2D u_texture2;
uniform float u_time;

out vec4 fragColor;

void main() {
	vec4 dist = texture(u_texture2, v_texCoords + (u_time * 0.5));
	vec2 distorter = dist.rr * vec2(0.5);
	fragColor = v_color * texture(u_texture, v_texCoords + (u_time * 0.125) + distorter);
}