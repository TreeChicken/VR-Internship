#version 150
// GLSL version 1.50
// Fragment shader for diffuse shading in combination with a texture map

// Uniform variables passed in from host program
uniform sampler2D myTexture;

// Variables passed in from the vertex shader
in vec4 forFragColor;
in vec2 frag_texcoord;

in float lambertian_tot;
in vec4 spec_tot;

// Output variable, will be written to framebuffer automatically
out vec4 out_color;
out vec4 frag_shaded;

void main()
{		
	//Apply color
	frag_shaded = texture(myTexture, frag_texcoord);
}

