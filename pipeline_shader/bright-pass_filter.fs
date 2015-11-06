#version 150
precision highp float;
/**
 * Copyright 2013 Marc Roßbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

uniform sampler2D jvr_Texture0;
in vec2 texCoord;

out vec4 final_color;

void main (void)
{
	vec4 color = texture(jvr_Texture0, texCoord);
	
	color -= 0.5;
	color *= 3.0;
	
	final_color = color;
}
