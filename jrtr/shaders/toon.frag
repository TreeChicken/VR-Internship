#version 150

uniform sampler2D myTexture;

in vec3 frag_normal;
in vec3 lightDir;
in vec4 frag_color;

in float intensity;
in vec2 frag_texcoord;

// Output variable, will be written to framebuffer automatically
//out vec4 out_color;

void main()
{
	
	vec4 color = vec4(1, 0, 0, 0);
	float mult;
	
	if (intensity > 0.95)
		mult = 1.0;
	else if (intensity > 0.5)
		mult = 0.75;
	else if (intensity > 0.25)
		mult = 0.5;
	else
		mult = 0.25;
	
	
	gl_FragColor = texture(myTexture, frag_texcoord) * mult;

}

