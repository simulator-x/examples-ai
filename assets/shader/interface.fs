#version 150
precision highp float;

uniform sampler2D jvr_Texture0;
uniform sampler2D jvr_Texture1;

uniform float alpha;

in vec2 texCoord;

out vec4 final_color;

void main (void){
    vec4 overlay = texture(jvr_Texture1, vec2(texCoord.x, 1.0-texCoord.y));
    vec4 original = texture(jvr_Texture0, texCoord);
    final_color = vec4(mix(original, vec4(overlay.xyz, 1), overlay.a).xyz, 1);
}
