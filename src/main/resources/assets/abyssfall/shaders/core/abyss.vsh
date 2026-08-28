#version 330

// Vertex stage for the abyss effect.
//
// 🔴 A COPY of cosmic.vsh, deliberately kept separate. Edits here must not be folded back into that program,
// whose appearance has already been accepted; the whole point of the copy is that the two can diverge.
//
// Differs from masked_pulse.vsh in one substantive way: it forwards the fragment's position in view space,
// because the fragment stage treats each fragment as a ray leaving the camera and needs somewhere for that ray
// to point. It also unpacks the per-frame viewing angles out of the vertex stream; see ViewerState for why they
// travel there rather than as uniforms.

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// Must match DefaultVertexFormat.ENTITY, which is what the pipeline binds. Unused attributes are still
// declared, because the format supplies them and the bindings are positional.
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec2 atlasCoord;    // the item's own sprite, within the atlas
out vec2 texCoord0;     // mask coordinate, 0..1 over the item's artwork
out vec3 viewPosition;  // this fragment in view space, which is where its ray starts
out vec3 frameState;    // yaw turns, pitch turns, depth
out vec2 lightLevel;    // block and sky light, each 0..1 (fraction of 15), from vanilla's lightCoords

// Must match FIXED_POINT_SCALE in ShaderLayerRenderer.
const float FIXED_POINT_SCALE = 32767.0;

void main() {
	vec4 view = ModelViewMat * vec4(Position, 1.0);

	gl_Position = ProjMat * view;

	viewPosition = view.xyz;
	atlasCoord = UV0;

	// The mask coordinate travels as a signed 16-bit pair in UV1, because the format has only one float pair and
	// the atlas coordinate needs it. See ShaderLayerRenderer for why round this way.
	texCoord0 = vec2(UV1) / FIXED_POINT_SCALE;

	// yaw and pitch travel in UV2, the sixteen-bit pair that used to carry the lightmap. A byte gave 256 steps
	// per full turn — every 1.4° of head movement made the whole field jump at once. Sixteen bits give
	// 32767 steps, far finer than a mouse. Depth stays in the colour channel's red byte, a coarse near/far
	// selector. The block and sky light levels sit in that channel's green and blue bytes, unpacked by the
	// renderer from the lightCoords vanilla hands it — so they describe where the item is, not where the
	// viewer is. The packing is in ShaderLayerRenderer and the meaning is in ViewerState.
	frameState = vec3(vec2(UV2) / FIXED_POINT_SCALE, Color.r);
	lightLevel = vec2(Color.g, Color.b);
}