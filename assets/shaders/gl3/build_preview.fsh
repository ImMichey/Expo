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

const vec3 cgv = vec3(0.2125, 0.7154, 0.0721);

out vec4 fragColor;

void main() {
    vec4 base = texture(u_texturearray, vec3(v_texCoords, v_texture_index));
    float gray = dot(base.rgb, cgv);
    fragColor = v_color * vec4(gray, gray, gray, base.a);
}