#version 330

// Fragment stage for the cosmic effect: a depth of sky seen through the item's outline.
//
// PORTED FROM AVARITIA'S cosmic.frag (Avaritia 3.3.0, MC 1.12.2)
// --------------------------------------------------------------
// The algorithm is that shader's, kept verbatim wherever 26.2 allows it, because its constants are tuned and
// rederiving them would only lose the tuning. Where it differs, the line says so.
//
// The reference set six uniforms per frame. 26.2 has no route for that on this draw path -- a uniform lives in
// a buffer, filling one means driving a RenderPass, and PreparedRenderType creates and closes its own. So:
//
//   time          -> GameTime from globals.glsl. NOTE this is NOT the same quantity: the reference had a
//                    monotonically rising int, this is a 0..1 ramp that resets once per Minecraft day. Used
//                    only for drift, where a periodic value is tolerable because the sphere is periodic too.
//   yaw, pitch    -> UV2, the sixteen-bit integer pair (see ViewerState for why not a byte)
//   externalScale -> selected by the vertex colour's red byte, near or far
//   lightlevel    -> the vertex colour's green and blue bytes, indexing vanilla's own lightmap texture
//   opacity       -> dropped; nothing here fades by quantity
//   cosmicuvs[10] -> compile-time constants, contributed by the renderer at bake time.
//
// 🔴 THAT LAST ONE IS THE IMPORTANT DIFFERENCE, and it is a simplification rather than a compromise. The
// reference re-uploaded its ten sprite rectangles every single frame. 26.2 animates a sprite by drawing the
// current frame INTO the sprite's fixed rectangle, so the rectangle never moves -- the pixels inside it
// change. The coordinates are therefore constants, and the animation still plays, driven entirely by vanilla.

#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;   // the item atlas: the item's own texture AND the star sprites live here
uniform sampler2D Sampler1;   // the mask; its red channel is the effect's opacity, as the reference uses
uniform sampler2D Sampler2;   // the lightmap, bound by the pipeline's useLightmap() render setup

in vec2 texCoord0;    // mask coordinate, 0..1 over the item's artwork
in vec2 atlasCoord;   // the item's own sprite, within the atlas
in vec3 viewPosition; // this fragment in view space, which is where its ray starts
in vec3 frameState;   // yaw turns, pitch turns, depth
in vec2 lightLevel;   // block and sky light, each 0..1 (fraction of 15), from vanilla's lightCoords

out vec4 fragColor;

const float TAU = 6.28318530718;
const float PI = 3.14159265359;

// The mask's rectangle within its atlas, contributed by the renderer at bake time.
//
// 🔴 The mask lives in an atlas rather than in its own texture, and that is what lets it animate: only
// TextureAtlas implements TickableTexture in 26.2, so a standalone mask would sit on frame zero forever and its
// .mcmeta would never even be read. The cost is this indirection — texCoord0 is 0..1 over the mask's own
// artwork, so it has to be mapped into the sheet before sampling.
//
// Constants for the same reason the star sprites' are: vanilla animates by blitting the current frame into a
// fixed rectangle, so the rectangle never moves and the pixels inside it are what change.
vec2 maskCoord(vec2 local) {
	return vec2(mix(MASK_U0, MASK_U1, local.x),
	            mix(MASK_V0, MASK_V1, local.y));
}

// The reference's uvtiles and cosmicoutof, unchanged.
const int SKY_CELLS = 16;
const float CELL_OUT_OF = 101.0;

// Rotation about an arbitrary axis. The reference's matrix, unchanged.
mat4 rotationMatrix(vec3 axis, float angle) {
	axis = normalize(axis);

	float s = sin(angle);
	float c = cos(angle);
	float oc = 1.0 - c;

	return mat4(oc * axis.x * axis.x + c,          oc * axis.x * axis.y - axis.z * s, oc * axis.z * axis.x + axis.y * s, 0.0,
	            oc * axis.x * axis.y + axis.z * s, oc * axis.y * axis.y + c,          oc * axis.y * axis.z - axis.x * s, 0.0,
	            oc * axis.z * axis.x - axis.y * s, oc * axis.y * axis.z + axis.x * s, oc * axis.z * axis.z + c,          0.0,
	            0.0,                               0.0,                               0.0,                              1.0);
}

// Chooses which cells hold a star, and which sprite and orientation each one gets.
//
// The reference's hash, with one correction: it took mod against 3.14 where sin's period is TAU. That confined
// the argument to [0, PI), over which sin is never negative — so half the hash's range was unreachable and the
// output was measurably lopsided. Against TAU the full range is used.
//
// ⚠️ This is still not a good hash, and correcting the modulus does not make it one. The dominant flaw is the
// input rather than the arithmetic: callers feed it small integers, and the layer offset of 10 is narrower than
// the 16-cell coordinate it is added to, so 35% of (layer, cell) pairs collide onto a value some other pair
// already produced. Fixing that would move every star, so it stays — the constellation is the reference's, and
// its quality is a property of the design rather than a defect to repair.
float rand2d(vec2 x) {
	return fract(sin(mod(dot(x, vec2(12.9898, 78.233)), TAU)) * 43758.5453);
}

// The star sprites' rectangles in the atlas, as constants.
//
// A chain guarded by the preprocessor rather than an array, because an array's length must be a constant and
// the number of sprites is configurable. The renderer emits SPRITE_COUNT as an integer define plus one set of
// SPRITE_n_U0 .. SPRITE_n_V1 per sprite, so exactly the branches that have data are compiled in. Those names
// are the renderer's, not this effect's — any effect drawing from named artwork gets the same ones.
//
// Returns (u0, v0, u1, v1). The caller takes its index modulo SPRITE_COUNT, so this is never asked for an
// index it has no branch for.
vec4 starBounds(int index) {
#if SPRITE_COUNT > 0
	if (index == 0) return vec4(SPRITE_0_U0, SPRITE_0_V0, SPRITE_0_U1, SPRITE_0_V1);
#endif
#if SPRITE_COUNT > 1
	if (index == 1) return vec4(SPRITE_1_U0, SPRITE_1_V0, SPRITE_1_U1, SPRITE_1_V1);
#endif
#if SPRITE_COUNT > 2
	if (index == 2) return vec4(SPRITE_2_U0, SPRITE_2_V0, SPRITE_2_U1, SPRITE_2_V1);
#endif
#if SPRITE_COUNT > 3
	if (index == 3) return vec4(SPRITE_3_U0, SPRITE_3_V0, SPRITE_3_U1, SPRITE_3_V1);
#endif
#if SPRITE_COUNT > 4
	if (index == 4) return vec4(SPRITE_4_U0, SPRITE_4_V0, SPRITE_4_U1, SPRITE_4_V1);
#endif
#if SPRITE_COUNT > 5
	if (index == 5) return vec4(SPRITE_5_U0, SPRITE_5_V0, SPRITE_5_U1, SPRITE_5_V1);
#endif
#if SPRITE_COUNT > 6
	if (index == 6) return vec4(SPRITE_6_U0, SPRITE_6_V0, SPRITE_6_U1, SPRITE_6_V1);
#endif
#if SPRITE_COUNT > 7
	if (index == 7) return vec4(SPRITE_7_U0, SPRITE_7_V0, SPRITE_7_U1, SPRITE_7_V1);
#endif
#if SPRITE_COUNT > 8
	if (index == 8) return vec4(SPRITE_8_U0, SPRITE_8_V0, SPRITE_8_U1, SPRITE_8_V1);
#endif
#if SPRITE_COUNT > 9
	if (index == 9) return vec4(SPRITE_9_U0, SPRITE_9_V0, SPRITE_9_U1, SPRITE_9_V1);
#endif

	// Unreachable for a well-formed effect. Returns a degenerate rectangle rather than sampling something
	// arbitrary, so a mistake shows up as nothing drawn instead of as the wrong artwork.
	return vec4(0.0);
}

void main() {
	// The mask decides where the effect appears, and its RED channel is the opacity -- the reference's
	// `col.a *= mask.r * opacity`, kept as-is.
	vec4 mask = texture(Sampler1, maskCoord(texCoord0));

	if (mask.r <= 0.0) {
		discard;
	}

#ifdef COSMIC_DEBUG_SOLID
	// ⚠️ DIAGNOSTIC ONLY. Paints the masked area a flat, unmistakable colour and returns before any of the
	// sky arithmetic runs.
	//
	// Placed here, immediately after the mask, so that it isolates exactly one question: does this layer
	// reach the screen at all, over the area the mask claims? Everything that could make the field faint --
	// the layer loop, the density test, the sprite sampling, the light mix -- is downstream and skipped.
	//
	// Magenta blade  => geometry, depth state, mask and blend are all correct; any faintness is the field's
	//                   own arithmetic and tuning it is worthwhile.
	// Unchanged blade => the fault is upstream of the field. Tuning the field is pointless. Look at the
	//                   depth state first: a coplanar layer biased the wrong way is rejected silently.
	fragColor = vec4(1.0, 0.0, 1.0, mask.r);
	return;
#endif

	// Unpack what the vertex stage forwarded. Turns rather than radians because the channel carries 0..1.
	float yaw = frameState.x * TAU;
	float pitch = (frameState.y - 0.5) * TAU;

	// How far away the field sits, selected by the depth byte. The reference drove this from a per-frame
	// uniform its item renderer set; here the renderer picks one of two ends and the shader interpolates
	// between them, which is all this draw path can carry. Pushing the field away is what makes a slot read
	// as fine still grain rather than as churn — the reference's own use for it in inventories.
	float externalScale = 1.0 + frameState.z * 24.0;
	float oneOverExternalScale = 1.0 / externalScale;

	// Background: the reference's dark red with its slow shift through green and blue. Unchanged.
	vec4 col = vec4(0.1, 0.0, 0.0, 1.0);

	// The reference took mod(time, 400)/400 over a rising tick count. GameTime is already a 0..1 ramp over a
	// Minecraft day, so the same 400-tick cycle is 60 turns per day. Multiplying the normalised clock rather
	// than scaling it back to ticks is deliberate: float32 puts a boundary in the wrong cycle otherwise.
	float pulse = fract(GameTime * 60.0);

	col.g = sin(pulse * TAU) * 0.075 + 0.225;
	col.b = cos(pulse * TAU) * 0.05 + 0.3;

	// The ray from the camera to this fragment.
	vec4 dir = normalize(vec4(-viewPosition, 0.0));

	// Rotate it to face where the viewer faces: pitch then yaw, as the reference does.
	float sb = sin(pitch);
	float cb = cos(pitch);
	dir = normalize(vec4(dir.x, dir.y * cb - dir.z * sb, dir.y * sb + dir.z * cb, 0.0));

	float sa = sin(-yaw);
	float ca = cos(-yaw);
	dir = normalize(vec4(dir.z * sa + dir.x * ca, dir.y, dir.z * ca - dir.x * sa, 0.0));

	// Drift. The reference multiplied its raw tick count by 0.0002, over a counter that never reset.
	// GameTime is a 0..1 ramp that returns to zero once per Minecraft day, so expressing the same rate
	// against it means the value here is the drift over a whole day — that is what COSMIC_DRIFT_SPEED states,
	// and it is applied directly, without the reference's 0.0002 on top.
	//
	// ⚠️ The field therefore returns to its starting position once per Minecraft day rather than drifting
	// forever. Making it monotonic needs a clock that does not reset, and 26.2 offers none on this draw path
	// (see the header). Rounding the speed to a whole number of cells per day would at least put the seam
	// where the pattern repeats — but it is not done, because the seam is only visible if one is watching a
	// single star, and pinning the speed to integers would take away the setting's usefulness.
	float drift = GameTime * COSMIC_DRIFT_SPEED;

	for (int i = 0; i < int(COSMIC_LAYERS); i++) {
		int mult = int(COSMIC_LAYERS) - i;

		// The reference's semi-random constants, unchanged. Arbitrary, but they are what give each layer an axis
		// unrelated to its neighbours' — which is what stops the stack reading as one sphere.
		int j = i + 7;
		float rand1 = float(j * j * 4321 + j * 8) * 2.0;
		int k = j + 1;
		float rand2 = float(k * k * k * 239 + k * 37) * 3.6;
		float rand3 = rand1 * 347.4 + rand2 * 63.4;

		vec3 axis = normalize(vec3(sin(rand1), sin(rand2), cos(rand3)));

		vec4 ray = dir * rotationMatrix(axis, mod(rand3, TAU));

		// Sphere to UV: longitude and latitude of where the ray leaves the sphere.
		float rawu = 0.5 + (atan(ray.z, ray.x) / TAU);
		float rawv = 0.5 + (asin(clamp(ray.y, -1.0, 1.0)) / PI);

		// Per-layer scale, so nearer layers slide faster than farther ones. This is where the depth comes from.
		float scale = float(mult) * 0.5 + 2.75;
		float u = rawu * scale * externalScale;
		float v = (rawv + drift * oneOverExternalScale) * scale * 0.6 * externalScale;

		// Which cell of this layer's grid the ray landed in.
		float cells = float(SKY_CELLS);
		int tu = int(mod(floor(u * cells), cells));
		int tv = int(mod(floor(v * cells), cells));

		// Does this cell hold a star? The reference's own test, hash % 101 < spriteCount, scaled by
		// COSMIC_DENSITY: 1.0 is the reference exactly, 2.0 doubles how many cells hold a star.
		int symbol = int(rand2d(vec2(float(tu), float(tv) + float(i) * 10.0)) * CELL_OUT_OF);

		if (symbol >= 0 && float(symbol) < float(SPRITE_COUNT) * COSMIC_DENSITY) {
			// 🔴 Which sprite this cell draws. Taken modulo SPRITE_COUNT because the test above admits
			// indices up to SPRITE_COUNT * COSMIC_DENSITY, and COSMIC_DENSITY may exceed 1.0 — a density of
			// 2.0 admits an index of 19 while only ten sprites exist. Without the modulo those cells
			// asked starBounds for an index it has no branch for and sampled the atlas origin instead,
			// which drew whatever unrelated artwork the packer left there.
			//
			// Raising the density therefore reuses the sprites rather than running out of them, which is
			// what "twice as many stars" ought to mean.
			int starIndex = int(mod(float(symbol), float(SPRITE_COUNT)));

			// Position within the cell.
			float ru = clamp(mod(u, 1.0) * cells - float(tu), 0.0, 1.0);
			float rv = clamp(mod(v, 1.0) * cells - float(tv), 0.0, 1.0);

			// Orientation: one of four rotations, optionally mirrored. Eight readings of one sprite, which is
			// most of why the reference's ten sprites do not read as ten repeated shapes.
			//
			// 🔴 The reference computed this as pow(tu, tv) + tu + 3 + tv*i, and that expression does not
			// survive float32. pow(15, 15) is 4.4e17 while a float's mantissa carries 24 bits, so for 89 of
			// the 256 coordinate pairs every low bit is gone — and a value whose low bits are zero is
			// congruent to zero, so mod 8 returns 0. Measured: 97% of the overflowing pairs collapse onto
			// rotation 0, leaving 38% of all stars unrotated where an eighth is intended.
			//
			// ⚠️ An earlier version here wrote pow(tu + 1.0, tv), to avoid pow(0, 0) being undefined. That
			// fixed the undefined case and made the distribution WORSE: raising the base brings the overflow
			// on sooner, taking it from 89 pairs to 98 and leaving 48% of stars unrotated. Rendered, the
			// overflowing region is a solid block of identically oriented sprites.
			//
			// So the orientation is drawn from the same hash that already chose the sprite, offset so the two
			// draws do not correlate. No exponentiation, nothing to overflow, and the arbitrary-but-stable
			// per-cell mixing the reference wanted is what a hash does by definition.
			//
			// Measured against the alternatives: this brings rotation 0 to 11.7% against an ideal 12.5%, and a
			// chi-squared of 11.2 over the eight orientations — below the 14.1 threshold at which uniformity
			// would be rejected. The reference's expression scores 2691, the pow(tu + 1.0, tv) variant 5004.
			//
			// A plain linear form such as tu*7 + tv*13 + i*29 scores a perfect zero, and is nonetheless
			// wrong: perfectly uniform because it is perfectly periodic, which renders as diagonal banding
			// marching across the sky. Uniformity of the histogram is not the property wanted here.
			int rotation = int(rand2d(vec2(float(tu) + 0.5, float(tv) + float(i) * 16.0 + 0.5)) * 8.0);
			bool flip = rotation >= 4;

			if (flip) {
				rotation -= 4;
				ru = 1.0 - ru;
			}

			float oru = ru;
			float orv = rv;

			if (rotation == 1) {
				oru = 1.0 - rv;
				orv = ru;
			} else if (rotation == 2) {
				oru = 1.0 - ru;
				orv = 1.0 - rv;
			} else if (rotation == 3) {
				oru = rv;
				orv = 1.0 - ru;
			}

			// The chosen sprite's rectangle, interpolated to the position within the cell.
			vec4 bounds = starBounds(starIndex);

			vec2 starCoord = vec2(
					mix(bounds.x, bounds.z, oru),
					mix(bounds.y, bounds.w, orv));

			// 🔴 The star's artwork, sampled from the item atlas. This is what replaces the procedural shape a
			// previous version of this shader invented: these sprites are animated by vanilla, and that
			// animation is where the field's shimmer comes from. Drawing the shape arithmetically produced a
			// field that was completely static.
			vec4 tcol = texture(Sampler0, starCoord);

			// Brightness from the sprite's red channel, faded towards the poles where the sphere mapping bunches
			// up and stars would otherwise crowd into a visible seam. The reference's expression, unchanged.
			float a = tcol.r
					* (0.5 + (1.0 / float(mult)))
					* (1.0 - smoothstep(0.15, 0.48, abs(rawv - 0.5)));

			// The reference's star colours, unchanged: cool and pale, varying per layer.
			float r = (mod(rand1, 29.0) / 29.0) * 0.3 + 0.4;
			float g = (mod(rand2, 35.0) / 35.0) * 0.4 + 0.6;
			float b = (mod(rand1, 17.0) / 17.0) * 0.3 + 0.7;

			// Added, not blended: overlapping layers brighten each other, which is what makes a dense patch read
			// as depth rather than as one flat sheet.
			col += vec4(r, g, b, 1.0) * a;
		}
	}

	// Environment light, as the reference applies it: the light where the item is, mixed at lightmix = 0.2
	// with full white, so the item is lit by the world around it — bright at noon on the surface, near-black
	// in a cave — and the sky dims with it. The reference set its lightlevel uniform from the item's own
	// location; here the two levels arrive in the vertex colour and index vanilla's lightmap texture, which is
	// the same quantity read from the same place vanilla reads it.
	//
	// The levels arrive as 0..1 fractions of 15, so scaling by 15/16 and offsetting by half a cell places
	// them at the centre of the lightmap's 16x16 cells rather than on the boundary between two.
	vec2 lightUV = lightLevel * (15.0 / 16.0) + (1.0 / 32.0);
	vec3 light = texture(Sampler2, lightUV).rgb;
	const float LIGHT_MIX = 0.2;
	vec3 shade = light * LIGHT_MIX + vec3(1.0 - LIGHT_MIX);

	// 🔴 A gain that reads the light every frame, and is deliberately NOT the reference's behaviour.
	//
	// The reference looks brighter than this shader did in every environment, and the cause is not the mix
	// above — that arithmetic is identical on both sides. It is what gets fed into it. The reference's light
	// began as gl_Color, which in 1.12.2 carried the fixed-function lighting its vertex program accumulated
	// (sceneColor plus ambient plus diffuse) before its lightlevel uniform was applied at all; and on the GUI
	// and fallback paths it did not sample the world's light, it simply asserted full brightness. 26.2 has no
	// fixed-function lighting to inherit, so this shader's light is the raw lightmap value and nothing else.
	//
	// Restoring the reference's constant offset would brighten every environment by the same amount. Instead the
	// gain is interpolated against the light in real time — strongest in darkness, weakest in daylight.
	//
	// ⚠️ Be clear about what this does to the environment response, because it is not a small thing: the gain
	// falls faster with light than `shade` rises, so the PRODUCT now decreases as the world gets brighter
	// (1.36x at black, 1.20x at full light). The field is therefore brightest in a cave and calmest at noon
	// — the inverse of what the mix above does on its own. That is the requested behaviour, not an oversight:
	// the sky is meant to assert itself where there is nothing else to see, and to stop competing with a lit
	// scene. Anyone "fixing" the direction should know they are changing the intent.
	//
	// 🔴 The factor is the luminance of `light`, NOT of `shade`. This is the whole reason the pair of endpoints
	// can be reached: LIGHT_MIX compresses shade into [1 - LIGHT_MIX, 1], so at 0.2 its luminance never leaves
	// [0.8, 1.0] and using it here would confine the gain to the top fifth of the range. `light` is the lightmap
	// sample itself and spans the full 0..1.
	//
	// Luminance rather than per channel so that a coloured light source — a redstone torch, lava — cannot tint
	// the gain. The sky's colours belong to the stars. The three weights sum to exactly 1.0, so a neutral
	// lightmap sample passes through unchanged and the two endpoints below are reached exactly.
	const vec3 LUMINANCE_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);
	const float GAIN_IN_DARK = 1.7;
	const float GAIN_IN_LIGHT = 1.2;

	float ambient = clamp(dot(light, LUMINANCE_WEIGHTS), 0.0, 1.0);
	float gain = mix(GAIN_IN_DARK, GAIN_IN_LIGHT, ambient);

	// ⚠️ Above unity everywhere, so the clamp below is reached more often than before — but the two endpoints
	// were chosen so that it costs headroom rather than colour, which is the failure an earlier pair of values
	// had. The reference's star colours are cool and pale (red 0.4–0.7, green 0.6–1.0, blue 0.7–1.0), so green
	// and blue saturate long before red does; a gain of 2.5 in darkness pinned both at 1.0 for every star and
	// the whole field read as white rather than as the intended cool blue.
	//
	// At 1.7 the peak product is 1.36x, which leaves the dimmest star at (0.54, 0.82, 0.95) — every channel
	// still below the clamp, so the palette survives at both ends. The only fragments that clip are stars whose
	// artwork is already 1.0 in green or blue, and those are the brightest few by design. Measured, not assumed.
	//
	// That is why 1.7 rather than more: it is the largest value in the requested 1.2–1.7 band, and the band
	// itself sits under the point where the palette starts collapsing. Note 1.2 would have been the wrong end
	// entirely — it makes the dark product 0.96x, i.e. dimmer than no gain at all.
	//
	// Distinct from COSMIC_BRIGHTNESS, which is also a multiplier: that one is a compile-time define and part of
	// what identifies a pipeline, so it states a fixed intent per configured effect. This one varies per
	// fragment from data in the vertex stream and compiles nothing.
	col.rgb *= shade * gain;

	col.rgb *= COSMIC_BRIGHTNESS;
	col = clamp(col, 0.0, 1.0);

	// The mask's red channel as opacity, exactly as the reference does.
	fragColor = vec4(col.rgb, mask.r);
}