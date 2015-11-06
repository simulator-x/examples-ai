#version 150
precision highp float;
in vec4 jvr_Vertex;
in vec2 jvr_TexCoord;

uniform mat4  jvr_ProjectionMatrix;

out   vec2 texCoord;

void main(void)
{
	texCoord = jvr_TexCoord;
	gl_Position = jvr_ProjectionMatrix * jvr_Vertex;
}