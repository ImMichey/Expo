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

uniform vec2 u_textureSize;
uniform vec4 u_outlineColor;

uniform float u_progress;
uniform float u_pulseStrength;
uniform float u_pulseMin;
uniform float u_thickness;
uniform bool u_outline;

out vec4 fragColor;

void main() {
    vec4 c = texture(u_texturearray, vec3(v_texCoords, v_texture_index));

    if(c.a == 0.0) {
        vec2 pixelSize = u_thickness / u_textureSize;

        vec4 c1 = texture(u_texturearray, vec3(v_texCoords + vec2(pixelSize.x, 0.0), v_texture_index));
        vec4 c2 = texture(u_texturearray, vec3(v_texCoords + vec2(-pixelSize.x, 0.0), v_texture_index));
        vec4 c3 = texture(u_texturearray, vec3(v_texCoords + vec2(0.0, pixelSize.y), v_texture_index));
        vec4 c4 = texture(u_texturearray, vec3(v_texCoords + vec2(0.0, -pixelSize.y), v_texture_index));

        if(u_outline) {
            if(c1.a > 0.0 || c2.a > 0.0 || c3.a > 0.0 || c4.a > 0.0) {
                fragColor = u_outlineColor;
                return;
            }
        }
    } else {
        c.rgb = mix(c.rgb * u_pulseMin, c.rgb * u_pulseStrength, u_progress);
        fragColor = v_color * c;
    }
}