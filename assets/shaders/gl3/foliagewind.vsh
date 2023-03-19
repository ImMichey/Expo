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

// Wind settings.
uniform float u_time;
uniform float u_offset;
uniform float u_speed;
uniform float u_strength;
uniform float u_skew;

out vec4 v_color;
out vec2 v_texCoords;
out float v_texture_index;

float getWind(vec2 vertex, vec2 uv, float time) {
    float diff = pow(-u_strength, 2.0);
    float strength = clamp(u_strength + diff + sin(time) * diff, u_strength, 0.0) * 100.0;
    float wind = (sin(time) + cos(time)) * strength * max(0.0, (1.0 - uv.y * 128.0));

    return wind;
}

void main() {
    v_color = a_color;
    v_color.a = v_color.a * (255.0/254.0);
    v_texCoords = a_texCoord0;
    v_texture_index = texture_index;
    float time = u_time * u_speed + u_offset;
    float posx = a_position.x + getWind(a_position.xy, v_texCoords, time);

    if(u_skew != 0.0) {
        posx += max(0.0, (1.0 - v_texCoords.y * 128.0)) * (u_skew / 12.0);
    }

    gl_Position = u_projTrans * vec4(posx, a_position.y, a_position.z, a_position.w);
}