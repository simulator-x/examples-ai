#version 150
precision highp float;
uniform sampler2D jvr_Texture0;
uniform sampler2D jvr_Texture1;
in vec2 texCoord;

uniform float nearClip;
uniform float farClip;
uniform float skyFog;

out vec4 final_color;

const float EULER = 2.718281828459;

float linearizeDepth(sampler2D tex)
{
  float z = texture(tex, texCoord).x;
  return (2.0 * nearClip) / (farClip + nearClip - z * (farClip - nearClip));
}

void main (void)
{
	vec4 base_color = texture(jvr_Texture1, texCoord);
	float zFog = 0.;
	
	zFog = linearizeDepth(jvr_Texture0);
	
	//vec4 final_color = vec4(zFog,zFog,zFog,1);
	vec4 fog_color = vec4(0.4,0.4,0.4,1.0);	

	final_color = mix(fog_color, base_color , pow(EULER,-1.0*pow(zFog*2.0,2.0)));
			
	if( zFog > 0.99 )
		final_color = ((1.0-skyFog) * final_color) + (skyFog * base_color);

   	//gl_FragColor = texture2D(jvr_Texture1, texCoord);

}
