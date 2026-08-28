#version 330

// Vertex stage for the abyss effect.
//
// The fragment shader marches a ray through a procedural volume, so it needs two things per fragment: where
// the ray starts in view space (its own position), and where the viewer is in the world — both the facing
// (which rotates every ray) and the camera's position (which gives translation parallax). The facing comes
// from UV2 as it does for the sky; the position is what this kind adds.
//
// 🔴 This is the ONLY effect that reads the camera position, and it repurposes channels the sky needs for
// other things: UV0 carries the camera's folded X/Z instead of the item atlas coordinate (this shader never
// samples the item), and the colour channel's green/blue bytes carry the folded Y instead of light levels.
// ShaderLayerRenderer decides what to write per effect; see ViewerState for the packing.

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// Must match DefaultVertexFormat.ENTITY, which is what the pipeline binds. Unused attributes are still
// declared, because the format supplies them and the bindings are positional.
in vec3 Position;
in vec4 Color;
in vec2 UV0;   // camera folded X (u), Z (v), 0..1 over POSITION_PERIOD metres — not the atlas, for this effect
in ivec2 UV1;  // mask coordinate
in ivec2 UV2;  // yaw turns, pitch turns
in vec3 Normal;

out vec2 texCoord0;     // mask coordinate, 0..1 over the item's artwork
out vec3 viewPosition;  // this fragment in view space, where its ray starts
out vec3 frameState;    // yaw turns, pitch turns, camera folded Y (0..1)
out vec2 camXZ;         // camera folded X and Z, 0..1 over the period

// Must match FIXED_POINT_SCALE in ShaderLayerRenderer.
const float FIXED_POINT_SCALE = 32767.0;

void main() {
	vec4 view = ModelViewMat * vec4(Position, 1.0);

	gl_Position = ProjMat * view;

	viewPosition = view.xyz;

	// The mask coordinate travels as a signed 16-bit pair in UV1, exactly as for the other effects.
	texCoord0 = vec2(UV1) / FIXED_POINT_SCALE;

	// Yaw and pitch from UV2; the camera's folded vertical position is packed across the colour channel's
	// green and blue bytes as one sixteen-bit value (high byte in green, low in blue).
	float camY = (Color.g * 255.0 * 256.0 + Color.b * 255.0) / 65535.0;
	frameState = vec3(vec2(UV2) / FIXED_POINT_SCALE, camY);

	// The two horizontal camera coordinates arrive as the one floating-point pair, already folded to 0..1 by
	// the renderer; the fragment shader unfolds them back into metres.
	camXZ = UV0;
}