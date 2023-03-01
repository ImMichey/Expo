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
uniform float u_time;

const float textureAdjustment = 128.0; // (2048.0 / 16.0) -> equal to MAX_TEXTURE_WIDTH / TEXTURE_WIDTH;
const float speed = 2.0; // 2
const float bendFactor = 0.2 / textureAdjustment;
const float skewStartHeightMultiplier = 2.5;

void main() {
    float height = (1.0 - v_texCoords.y * textureAdjustment);
    float offset = pow(height, skewStartHeightMultiplier);
    offset *= (sin(u_time * speed) * bendFactor);

    gl_FragColor = v_color * texture(u_texturearray, vec3(vec2(v_texCoords.x + offset, v_texCoords.y), v_texture_index));
}