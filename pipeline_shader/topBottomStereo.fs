#version 100
precision highp float;
uniform sampler2D rightEye;
uniform sampler2D leftEye;
uniform float intensity;
int vec2 texCoord;

out vec4 final_color;

void main (void)
{
    if (texCoord.y < 0.5)
        final_color = texture(rightEye, vec2(texCoord.x, texCoord.y*2.0));
    else
        final_color = texture(leftEye, vec2(texCoord.x, texCoord.y*2.0-1.0));
}
