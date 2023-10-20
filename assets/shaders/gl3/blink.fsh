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
uniform float u_intensity;

out vec4 fragColor;

void main() {
    vec4 bc = texture(u_texturearray, vec3(v_texCoords, v_texture_index));
    fragColor = v_color * vec4(1.0 - (1.0 - bc.r) * u_intensity, 1.0 - (1.0 - bc.g) * u_intensity, 1.0 - (1.0 - bc.b) * u_intensity, bc.a);
}