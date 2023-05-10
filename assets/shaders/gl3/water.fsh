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
uniform sampler2D u_displacement;

uniform float u_time;

out vec4 fragColor;

void main() {
    float m = 1024.0 / 16.0;
    float x = v_texCoords.x;
    float y = v_texCoords.y;

    float off1 = 50.0 / m;
    float off2 = 233.0 / m;
    float off3 = 284.0 / m;

    float t1 = u_time / m;
    float t2 = u_time / m + off1;
    float t3 = u_time / m + off2;
    float t4 = u_time / m + off3;

    vec2 distortion1 = texture(u_displacement, vec2(x - t1, y + t1)).rg;
    vec2 distortion2 = texture(u_displacement, vec2(x + t2, y + t2)).rg;
    vec2 distortion3 = texture(u_displacement, vec2(x + t3, y - t3)).rg;
    vec2 distortion4 = texture(u_displacement, vec2(x - t4, y - t4)).rg;

    vec4 c1 = texture(u_texture, vec2(x + t1, y - t1) + distortion1);
    vec4 c2 = texture(u_texture, vec2(x - t2, y - t2) + distortion2);
    vec4 c3 = texture(u_texture, vec2(x - t3, y + t3) + distortion3);
    vec4 c4 = texture(u_texture, vec2(x + t4, y + t4) + distortion4);

    vec4 pixelColor = c1 * 0.25 + c2 * 0.25 + c3 * 0.25 + c4 * 0.25;

    pixelColor.r += (1.0 - pixelColor.r) * 0.25;
    pixelColor.g += (1.0 - pixelColor.g) * 0.25;
    pixelColor.b += (1.0 - pixelColor.b) * 0.25;

    if(pixelColor.r > 0.81) {
        pixelColor.rgb = vec3(1.0);
    }

    fragColor = pixelColor * v_color;
}