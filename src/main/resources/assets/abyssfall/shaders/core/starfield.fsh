#version 330

// Fragment stage for the starfield effect: a depth of sky seen through the item's outline.
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

// The reference's rand2d, unchanged. Its job is choosing which cells hold a star, and swapping it for a
// different hash changes the constellation -- so it stays as written even though it is not a good hash.
float rand2d(vec2 x) {
	return fract(sin(mod(dot(x, vec2(12.9898, 78.233)), 3.14)) * 43758.5453);
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
	vec4 mask = texture(Sampler1, texCoord0);

	if (mask.r <= 0.0) {
		discard;
	}

#ifdef STARFIELD_DEBUG_SOLID
	// ⚠️ DIAGNOSTIC ONLY. Paints the masked area a flat, unmistakable colour and returns before any of the
	// sky arithmetic runs.
	//
	// Placed here, immediately after the mask, so that it isolates exactly one question: does this layer
	// reach the screen at all, over the area the mask claims? Everything that could make the field faint --
	// the layer loop, the density test, the sprite sampling, the light mix -- is downstream and skipped.
	//
	// Magenta blade  => geometry, depth state, mask and blend are all correct; any faintness is the field's
	//                   own arithmetic and tuning it is worthwhile.
	// Unchanged blade => the fault is upstream of the starfield. Tuning the field is pointless. Look at the
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
	// against it means the value here is the drift over a whole day — that is what STAR_DRIFT_SPEED states,
	// and it is applied directly, without the reference's 0.0002 on top.
	//
	// ⚠️ The field therefore returns to its starting position once per Minecraft day rather than drifting
	// forever. Making it monotonic needs a clock that does not reset, and 26.2 offers none on this draw path
	// (see the header). Rounding the speed to a whole number of cells per day would at least put the seam
	// where the pattern repeats — but it is not done, because the seam is only visible if one is watching a
	// single star, and pinning the speed to integers would take away the setting's usefulness.
	float drift = GameTime * STAR_DRIFT_SPEED;

	for (int i = 0; i < int(STAR_LAYERS); i++) {
		int mult = int(STAR_LAYERS) - i;

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
		// STAR_DENSITY: 1.0 is the reference exactly, 2.0 doubles how many cells hold a star.
		int symbol = int(rand2d(vec2(float(tu), float(tv) + float(i) * 10.0)) * CELL_OUT_OF);

		if (symbol >= 0 && float(symbol) < float(SPRITE_COUNT) * STAR_DENSITY) {
			// 🔴 Which sprite this cell draws. Taken modulo SPRITE_COUNT because the test above admits
			// indices up to SPRITE_COUNT * STAR_DENSITY, and STAR_DENSITY may exceed 1.0 — a density of
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
			// The reference wrote pow(tu, tv) here, which is undefined in GLSL for tu = tv = 0 and may come
			// back as 0, 1 or NaN depending on the driver — one cell in 256 per layer, differing between
			// machines. Adding 1.0 to the base keeps the intent (an arbitrary but stable per-cell mixing of
			// the two coordinates) while leaving the base positive everywhere, which pow is defined for.
			int rotation = int(mod(pow(float(tu) + 1.0, float(tv)) + float(tu) + 3.0 + float(tv * i), 8.0));
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
			// starfield that was completely static.
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
	col.rgb *= light * LIGHT_MIX + vec3(1.0 - LIGHT_MIX);

	col.rgb *= STAR_BRIGHTNESS;
	col = clamp(col, 0.0, 1.0);

	// The mask's red channel as opacity, exactly as the reference does.
	fragColor = vec4(col.rgb, mask.r);
}