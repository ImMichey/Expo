#version 330

#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

in vec4 a_position;
in vec4 a_color;
in vec2 a_texCoord0;
in float texture_index;

uniform mat4 u_projTrans;

out vec4 v_color;
out vec2 v_texCoords;
out float v_texture_index;

void main() {
    v_color = a_color;
    v_color.a = v_color.a * (255.0/254.0);
    v_texCoords = a_texCoord0;
    v_texture_index = texture_index;
    gl_Position = u_projTrans * a_position;
}