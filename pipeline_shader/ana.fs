#version 150
precision highp float;
uniform sampler2D leftEye;
uniform sampler2D rightEye;

in vec2 texCoord;

out vec4 final_color;

void main (void)
{
  vec4 left = texture(leftEye, texCoord);
  vec4 right = texture(rightEye, texCoord);

  final_color = vec4(left.r, right.gb, 1.0);
  //gl_FragColor = right;
}
