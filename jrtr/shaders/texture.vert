#version 150
// GLSL version 1.50 
// Vertex shader for diffuse shading in combination with a texture map

// Uniform variables, passed in from host program via suitable 
// variants of glUniform*
uniform mat4 projection;
uniform mat4 modelview;

uniform vec4 lightPosition[8];
uniform vec3 specColor[8];

uniform int nLights;

// Input vertex attributes; passed in from host program to shader
// via vertex buffer objects
in vec3 normal;
in vec4 position;
in vec2 texcoord;

in vec4 color;

// Output variables for fragment shader
out vec4 forFragColor;
out vec2 frag_texcoord;

out float lambertian_tot;
out vec4 spec_tot;

//const vec3 specColor = vec3(1.0, 1.0, 1.0);

void main()
{		
	// Compute dot product of normal and light direction
	// and pass color to fragment shader
	// Note: here we assume "lightDirection" is specified in camera coordinates,
	// so we transform the normal to camera coordinates, and we don't transform
	// the light direction, i.e., it stays in camera coordinates

	// Pass texture coordiantes to fragment shader, OpenGL automatically
	// interpolates them to each pixel  (in a perspectively correct manner) 
	frag_texcoord = texcoord;

	// Transform position, including projection matrix
	// Note: gl_Position is a default output variable containing
	// the transformed vertex position
	gl_Position = projection * modelview * position;
}
