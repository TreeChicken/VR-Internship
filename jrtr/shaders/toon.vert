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

out vec3 frag_normal;
out vec3 lightDir;
out vec4 frag_color;

out float intensity;
out vec2 frag_texcoord;

void main()
{
	frag_normal = normal;
	vec4 direction = normalize(lightPosition[0] - position);
	
	lightDir = vec3(direction.x, direction.y, direction.z);
	
	intensity = max(dot(modelview * vec4(normal,0), direction),0);
	
	
	gl_Position = projection * modelview * position;

	frag_texcoord = texcoord;
	
}
