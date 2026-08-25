#version 330

// Vertex stage for an item's shader layer.
//
// Does no lighting and no fog: the fragment stage decides the colour outright, so anything this
// stage computed for it would only be discarded.
//
// The two imports are what make ModelViewMat and ProjMat available. They correspond to the bind
// group layouts declared on the pipeline (BindGroupLayouts.MATRICES_PROJECTION); the set here and
// the set there have to agree, or the program will not link.

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// Must match DefaultVertexFormat.ENTITY, which is what the pipeline binds. Unused attributes are
// still declared, because the format supplies them and the bindings are positional.
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec2 texCoord0;

void main() {
	gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

	texCoord0 = UV0;
}
