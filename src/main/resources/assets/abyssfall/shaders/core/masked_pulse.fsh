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

uniform sampler2D Sampler0;   // the item's own texture, as an atlas. Read when colour is derived from it
uniform sampler2D Sampler1;   // the mask: green = continuous, blue = sampled

in vec2 texCoord0;
in vec2 atlasCoord;

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

#ifdef COLOR_FROM_TEXTURE
// Perceived brightness. Weighted rather than a plain average because the eye is far more sensitive to
// green than to blue, and an unweighted mean makes a saturated blue read as brighter than it looks.
float luminance(vec3 color) {
	return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

// The colour to draw, worked out from the pixel this effect is covering.
//
// Every branch here is guarded by a #ifdef rather than selected at runtime: only the one derivation an
// effect asked for is compiled in, so the others cost nothing and cannot be reached by accident.
//
// Each ends by mixing back towards the original by DERIVE_STRENGTH, so a derivation can be applied
// partially. That is what lets an effect deepen as San falls while remaining the same kind of effect.
vec3 deriveColor(vec3 original) {
	vec3 target = vec3(DERIVE_R, DERIVE_G, DERIVE_B);
	vec3 derived = original;

#ifdef DERIVE_TINTED
	// Keep the item's light and shade, take the hue from elsewhere. Multiplying the target by the
	// original's brightness preserves every highlight and shadow the artist painted, so the object stays
	// itself while its colour stops being its own.
	derived = target * (luminance(original) * 2.0);
#endif

#ifdef DERIVE_DRAINED
	// Take the colour out first, then stain what is left. Going through grey is what makes this read as
	// something removed rather than something added, and it makes two differently coloured items
	// converge on the same appearance.
	float grey = luminance(original);
	derived = mix(vec3(grey), target * grey * 2.0, 0.5);
#endif

#ifdef DERIVE_INVERTED
	// Invert brightness while keeping the target's hue, so the shape stays legible but its lighting is
	// impossible. Reads as a negative of the object rather than as a different object.
	float inverted = 1.0 - luminance(original);
	derived = mix(vec3(inverted), target, 0.5) * (0.5 + inverted);
#endif

#ifdef DERIVE_GLOWING
	// Leave the item alone and add light. The only derivation that cannot make an item harder to
	// identify, because nothing is taken away.
	derived = original + target * luminance(original);
#endif

	return clamp(mix(original, derived, DERIVE_STRENGTH), 0.0, 1.0);
}
#endif

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

#ifdef COLOR_FROM_TEXTURE
	// Colour derived from the item itself. Sampling its own texture also gives its alpha, which matters:
	// the geometry follows the item's faces, and a face may cover transparent parts of the sprite where
	// the item is not actually there. Drawing over those would thicken the item's silhouette.
	vec4 itemColor = texture(Sampler0, atlasCoord);

	if (itemColor.a <= 0.0) {
		discard;
	}

	vec3 shaded = deriveColor(itemColor.rgb);

	// Pulsing towards the item's own colour rather than between two invented ones: the effect breathes
	// between "as it was" and "as it has become", which is a different statement from two colours
	// alternating.
	fragColor = vec4(mix(itemColor.rgb, shaded, pulse * 0.5 + 0.5), opacity * itemColor.a);
#else
	vec3 colorA = vec3(COLOR_A_R, COLOR_A_G, COLOR_A_B);
	vec3 colorB = vec3(COLOR_B_R, COLOR_B_G, COLOR_B_B);

	fragColor = vec4(mix(colorB, colorA, pulse), opacity);
#endif
}
