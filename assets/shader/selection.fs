#version 150
precision highp float;

uniform sampler2D sceneMap;
uniform float time;
uniform bool enabled;

in vec2 texCoord;

out vec4 glFragColor;

void main()
{
    float boarder = 0.2;
    glFragColor = texture(sceneMap, texCoord);
    if (enabled) {
        if(texCoord.y < boarder || texCoord.y > (1.0-boarder)) {
            float boarderDistance = 1.0;
            if(texCoord.y < boarderDistance) boarderDistance = texCoord.y;
            if(1.0 - texCoord.y < boarderDistance) boarderDistance = 1.0 - texCoord.y;
            float boarderFactor = 1.0 - (boarderDistance / boarder);
            
            float locationFactor = texCoord.x * texCoord.y * 20.0;
            float factor = (sin(time*2.0+locationFactor) / 4.0) + 0.75;
            factor = factor * boarderFactor;
            float greyScale = (glFragColor.x+glFragColor.y+glFragColor.z)/3.0;
            greyScale = greyScale + 0.3;
            if(greyScale > 1.0) greyScale = 1.0;
            vec4 target = vec4(greyScale,greyScale,greyScale,1.0);
            glFragColor = glFragColor + ((target - glFragColor) * factor);
        }
    }
}


