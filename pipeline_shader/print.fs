#version 150
precision highp float;
uniform sampler2D jvr_Texture0;
in vec2 texCoord;

out vec4 final_color;

void main (void)
{	
	final_color = texture(jvr_Texture0, texCoord);
}
