#version 150
precision highp float;
uniform sampler2D jvr_Texture0;
uniform float saturation;

in vec2 texCoord;

out vec4 final_color;

void main (void) {

  vec4 tex = texture( jvr_Texture0, texCoord );
  float c = (tex.r + tex.g + tex.b) / 3.0;
  vec4 bw = vec4( c, c, c, tex.a );
  final_color = mix( bw, tex, saturation );
}
