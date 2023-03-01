#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_texCoords;
varying vec4 v_color;

uniform sampler2D u_texture;
uniform sampler2D u_texture2;
uniform float u_time;

void main() {
	vec4 dist = texture2D(u_texture2, v_texCoords + (u_time * 0.5));
	vec2 distorter = dist.rr * vec2(0.5);
	gl_FragColor = texture2D(u_texture, v_texCoords + (u_time * 0.125) + distorter) * v_color;
}