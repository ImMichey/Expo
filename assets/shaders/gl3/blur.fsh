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

uniform float u_resolution;
uniform float u_radius;
uniform vec2 u_dir;

out vec4 fragColor;

void main() {
    vec4 sum = vec4(0.0);
    vec2 tc = v_texCoords;
    float blur = u_radius / u_resolution;
    float hstep = u_dir.x;
    float vstep = u_dir.y;

    sum += texture(u_texture, vec2(tc.x - 4.0 * blur * hstep, tc.y - 4.0 * blur * vstep)) * 0.0162162162;
    sum += texture(u_texture, vec2(tc.x - 3.0 * blur * hstep, tc.y - 3.0 * blur * vstep)) * 0.0540540541;
    sum += texture(u_texture, vec2(tc.x - 2.0 * blur * hstep, tc.y - 2.0 * blur * vstep)) * 0.1216216216;
    sum += texture(u_texture, vec2(tc.x - 1.0 * blur * hstep, tc.y - 1.0 * blur * vstep)) * 0.1945945946;
    sum += texture(u_texture, vec2(tc.x, tc.y)) * 0.2270270270;
    sum += texture(u_texture, vec2(tc.x + 1.0 * blur * hstep, tc.y + 1.0 * blur * vstep)) * 0.1945945946;
    sum += texture(u_texture, vec2(tc.x + 2.0 * blur * hstep, tc.y + 2.0 * blur * vstep)) * 0.1216216216;
    sum += texture(u_texture, vec2(tc.x + 3.0 * blur * hstep, tc.y + 3.0 * blur * vstep)) * 0.0540540541;
    sum += texture(u_texture, vec2(tc.x + 4.0 * blur * hstep, tc.y + 4.0 * blur * vstep)) * 0.0162162162;

    fragColor = v_color * sum;
}