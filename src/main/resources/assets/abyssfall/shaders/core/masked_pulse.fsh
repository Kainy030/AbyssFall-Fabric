#version 330

// Fragment stage for an item's shader layer.
//
// Two effects share one mask texture, told apart by channel:
//
//   green — lit continuously, every masked pixel, always on
//   blue  — a random subset, held for one round, then replaced by a fresh subset
//
// In both cases the channel's VALUE is the opacity, so artwork can fade an effect out towards an
// edge without any code changing. The two are then modulated by a common pulse, which is what keeps
// an item reading as one object instead of two effects sharing a silhouette.
//
// The tunables arrive as compile-time defines rather than as a uniform block. A uniform would mean a
// buffer of our own, and filling one means driving a RenderPass by hand — which is not possible from
// inside submitCustomGeometry, where this shader is used. Defines cost nothing at runtime; the price
// is that changing a value requires a new pipeline, which is exactly what happens anyway since the
// mod does not reload configuration in place.
//
// One program is compiled per distinct set of values, so two items configured differently do not
// interfere, and two configured identically share.

#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;   // the item's own texture, bound but unread: the ordinary layer drew it
uniform sampler2D Sampler1;   // the mask: green = continuous, blue = sampled

in vec2 texCoord0;

out vec4 fragColor;

const float TAU = 6.28318530718;
const float PI = 3.14159265359;

// Every value below arrives as a #define from MaskedPulseEffect. Nothing about the item, its size or
// its colours is written here — MASK_RESOLUTION in particular must come from configuration, since a
// mask is not necessarily 16 pixels and this project already ships a 16x48 item texture.
//
//   MASK_RESOLUTION        pixels across the mask, which sets the sampling grid
//   SAMPLE_DENSITY         fraction of eligible pixels lit per round
//   SAMPLE_ROUNDS_PER_DAY  rounds per Minecraft day
//   PULSE_CYCLES_PER_DAY   pulse cycles per Minecraft day
//   SAMPLE_FADE            flag: ease sampled pixels in and out
//
// COLOR_A_* and COLOR_B_* come from a ShaderColorSource rather than from this effect. Where a colour is
// decided is deliberately not settled yet, so this shader states only what it needs — two colours to
// sweep between — and does not care whether they were configured, computed, or sampled from something
// else. A source that supplied colour differently would come with its own shader.

// Hash of three integers, deciding whether a pixel is lit this round. Keyed on the round number as
// well as the position, so each round is an independent draw while staying constant within itself —
// which is what makes a chosen pixel hold for its whole round and then vanish.
float hash13(ivec3 key) {
	int h = key.x * 374761393 + key.y * 668265263 + key.z * 1274126177;
	h = (h ^ (h >> 13)) * 1274126177;
	h = h ^ (h >> 16);
	return float(h & 0x00FFFFFF) / 16777216.0;
}

void main() {
	vec4 mask = texture(Sampler1, texCoord0);

	// One shared pulse. GameTime is a 0..1 ramp over a Minecraft day, so a rate is expressed as
	// cycles per day rather than in ticks.
	float pulse = sin(GameTime * PULSE_CYCLES_PER_DAY * TAU) * 0.5 + 0.5;

	// The continuous half: opacity straight from the green channel.
	float continuous = mask.g;

	// The sampled half.
	float sampled = 0.0;

	if (mask.b > 0.0) {
		// Same domain discipline as the pulse: multiply the normalised clock, never scale it back up
		// to ticks first. Doing that reintroduces float error large enough to put a boundary in the
		// wrong round.
		float roundPosition = GameTime * SAMPLE_ROUNDS_PER_DAY;
		float roundNumber = floor(roundPosition);

		ivec2 pixel = ivec2(floor(texCoord0 * MASK_RESOLUTION));
		float draw = hash13(ivec3(pixel, int(roundNumber)));

		if (draw < SAMPLE_DENSITY) {
			sampled = mask.b;

#ifdef SAMPLE_FADE
			// Ease in and out across the round rather than switching. A sine over the round's own
			// progress is zero at both ends, so a pixel is never cut off mid-brightness — and the
			// next round's choice is independent, so nothing appears to move.
			sampled *= sin(fract(roundPosition) * PI);
#endif
		}
	}

	// The two halves are added. Artwork is expected to keep the channels apart, so in practice only
	// one is non-zero at any pixel; adding is simply the operation that does not care either way.
	float opacity = clamp(continuous + sampled, 0.0, 1.0);

	if (opacity <= 0.0) {
		discard;
	}

	vec3 colorA = vec3(COLOR_A_R, COLOR_A_G, COLOR_A_B);
	vec3 colorB = vec3(COLOR_B_R, COLOR_B_G, COLOR_B_B);

	fragColor = vec4(mix(colorB, colorA, pulse), opacity);
}
