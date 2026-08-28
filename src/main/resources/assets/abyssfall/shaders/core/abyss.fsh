#version 330

// Fragment stage for the abyss effect: a dark, infinitely deep space seen through the item's outline.
//
// 🔴 This is a LAYERED DIRECTION FIELD, not a volume march. The earlier version treated every fragment as a
// ray and walked it through a 3-D voxel lattice, computing world points and near-misses per fragment — which
// is world-generation done in the shader every frame, and its cost scaled with the marched distance and, far
// more, with the held item's on-screen area. That is the wrong model for an abyss: the viewer cannot measure
// real parallax depth in a dark, foggy field, so the depth only has to be SUGGESTED.
//
// Like the cosmic sky it now draws a set of infinitely far shells keyed off the viewing DIRECTION, which costs
// the same regardless of any distance. The depth illusion is added the cheap way: each layer's direction is
// nudged by the CAMERA POSITION by an amount that falls off with layer depth (see parallax()). A near layer
// is shifted a lot by a sideways move, a far layer almost nothing — the eye reads that as objects at different
// distances, even though nothing has a real 3-D position. There is no per-fragment loop over space; the only
// loop is over a fixed, small number of layers.
//
// Each layer is a seamless sky: a point is stable where a hashed grid cell falls under the occupancy, and the
// field wraps over the whole view, so there is nothing that can tile, pop in at a moving sphere, or require a
// chunk to "load". The GLSL holds only the field construction; every look is an AbyssEffect #define.
//
// Per-frame values still arrive through the vertex stream (no uniform route on this draw path):
//   yaw, pitch    -> UV2 sixteen-bit pair (facing; rotates every ray)
//   camera X/Z    -> UV0 float pair, folded 0..1 over POSITION_PERIOD metres
//   camera Y      -> colour green/blue bytes, one sixteen-bit folded value
//   time          -> GameTime from globals.glsl, a 0..1 ramp resetting each Minecraft day (the faint stir).

#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;   // declared to match the pipeline's bind layout; this shader does not sample it
uniform sampler2D Sampler1;   // the mask; its red channel is the window's opacity
uniform sampler2D Sampler2;   // declared to match the bind layout; the abyss lights itself and does not read it

in vec2 texCoord0;    // mask coordinate, 0..1 over the item's artwork
in vec3 viewPosition; // this fragment in view space, where its ray starts
in vec3 frameState;   // yaw turns, pitch turns, camera folded Y (0..1)
in vec2 camXZ;        // camera folded X and Z, 0..1 over the period

out vec4 fragColor;

const float TAU = 6.28318530718;

// Must match ViewerState.POSITION_PERIOD: the world distance over which the camera position is folded. Used
// only to scale the packed 0..1 position into a small parallax offset; no volume reaches this far.
const float POSITION_PERIOD = 1024.0;

// 🔴 This is a LAYERED DIRECTION FIELD, not a volume march (see header). No voxel lattice, no DDA, no 3-D
// point math. Each shell is a 2-D sky keyed off the (parallax-shifted) viewing DIRECTION, so it is seamless
// and its cost is the same at any "distance".

// A stable 0..1 hash of a 2-D direction cell plus a layer seed. Folded with & 255 so neighbouring cells differ
// while the value stays defined across the whole view.
float hash2(vec2 p, float seed) {
	uvec3 u = uvec3(ivec3(floor(p), int(mod(seed, 255.0)))) & 255u;
	uint h = u.x * 73856093u ^ u.y * 19349663u ^ u.z * 83492791u;
	h = (h ^ (h >> 13)) * 1274126177u;
	h ^= (h >> 16);
	return float(h) / 4294967295.0;
}

// Smooth 2-D value noise: a random value per direction cell, bilinearly interpolated with smoothstep so it is
// continuous across every cell edge (this is what makes the haze soft rather than square).
float valueNoise2(vec2 p, float seed) {
	ivec2 c = ivec2(floor(p));
	vec2 f = fract(p);
	vec2 u = f * f * (3.0 - 2.0 * f);

	float a = hash2(vec2(c) + vec2(0.0, 0.0), seed);
	float b = hash2(vec2(c) + vec2(1.0, 0.0), seed);
	float d = hash2(vec2(c) + vec2(0.0, 1.0), seed);
	float e = hash2(vec2(c) + vec2(1.0, 1.0), seed);

	return mix(mix(a, b, u.x), mix(d, e, u.x), u.y);
}

// Two-octave haze on a direction plane: a broad sweep plus finer detail.
float hazeField2(vec2 p, float seed) {
	float base = valueNoise2(p, seed);
	float detail = valueNoise2(p * 2.13 + 17.7, seed + 91.0);
	return base * 0.7 + detail * 0.3;
}

// The mask's rectangle within its atlas, contributed by the renderer at bake time. The mask lives in an atlas
// (only TextureAtlas ticks in 26.2, so a standalone mask could never animate), and texCoord0 is 0..1 over the
// mask's own artwork, so it is mapped into the sheet before sampling.
vec2 maskCoord(vec2 local) {
	return vec2(mix(MASK_U0, MASK_U1, local.x),
	            mix(MASK_V0, MASK_V1, local.y));
}

// The cheap parallax: shift a shell's direction by the camera position, scaled by a strength that falls off
// sharply for deeper shells. depth01 is 0 (nearest) .. 1 (farthest); the power keeps most shells almost
// parallax-free while the closest few slide strongly, which the eye reads as objects at different distances.
vec2 parallax(vec2 dir, vec3 cam, float depth01) {
	float strength = ABYSS_PARALLAX * pow(1.0 - depth01, ABYSS_PARALLAX_FALLOFF);
	return dir + cam.xz * strength + cam.y * strength * 0.6;
}

// One shell's worth of light over a direction plane: soft haze plus scattered sharp motes. Everything here is
// a function of the (already parallax-shifted) viewing direction and the layer seed only, so it is seamless
// and costs nothing in distance. Returns the additive colour; reports its haze strength through hazeOut.
vec3 shellField(vec2 dir, float seed, float freq, float timePhase, out float hazeOut) {
	// Haze: soft two-octave murk. Sampled in DIRECTION space (not grid space) so its screen size is stable
	// across layers; the layer frequency only decides how dense the scattering is. Debug: pushed bright.
	float n = hazeField2(dir * ABYSS_HAZE_SCALE, seed);
	float density = smoothstep(1.0 - ABYSS_HAZE_COVERAGE, 1.0, n);
	hazeOut = density;

	float greyMix = valueNoise2(dir * ABYSS_HAZE_SCALE * 0.5 + 31.0, seed + 57.0);
	vec3 hazeCol = mix(vec3(ABYSS_STRUCT_R, ABYSS_STRUCT_G, ABYSS_STRUCT_B), vec3(0.6), greyMix * 0.6);
	vec3 col = hazeCol * hazeOut * ABYSS_HAZE_BRIGHTNESS;

	// Motes: a direction-space grid (one cell per 1/freq of view). The glow radius is also in DIRECTION
	// units, so a mote reads the same angular size on every layer — the earlier version measured it in grid
	// units, which (after dir*freq) shrank it to a sub-pixel speck. Check the 3x3 cells around this fragment.
	float best = 0.0;
	vec2 cellId = floor(dir * freq);
	// Glow angular half-size in direction units. Derived from the configured structure radius but given a
	// clear debug floor so points are unmistakably visible before we tune for look.
	float glowR = max(ABYSS_STRUCTURE_RADIUS / max(freq, 0.001) * 2.0, 0.02);
	for (int ox = -1; ox <= 1; ox++) {
		for (int oy = -1; oy <= 1; oy++) {
			vec2 cc = cellId + vec2(ox, oy);

			for (int s = 0; s < 4; s++) {
				if (s >= int(ABYSS_STRUCTURES_PER_CELL)) {
					break;
				}
				float lseed = seed + float(s) * 13.7;
				float occ = hash2(cc, lseed);

				if (occ >= ABYSS_OCCUPANCY) {
					continue;
				}

				// Sub-point position inside the cell (in direction space), plus the faint periodic wander.
				vec2 r = vec2(hash2(cc + 0.37, lseed), hash2(cc + 7.13, lseed + 3.3));
				vec2 anchor = (cc + r) / freq;
				vec2 stir = vec2(
					sin(timePhase + r.x * TAU),
					cos(timePhase * 0.9 + r.y * TAU)) * ABYSS_STIR_AMPLITUDE;

				float dd = distance(dir, anchor + stir);
				best = max(best, 1.0 - smoothstep(glowR * 0.5, glowR, dd));
			}
		}
	}

	col += vec3(ABYSS_STRUCT_R, ABYSS_STRUCT_G, ABYSS_STRUCT_B) * best * ABYSS_BRIGHTNESS;
	return col;
}

void main() {
	// The mask decides where the window opens; its RED channel is the opacity.
	vec4 mask = texture(Sampler1, maskCoord(texCoord0));

	if (mask.r <= 0.0) {
		discard;
	}

#ifdef ABYSS_DEBUG_SOLID
	fragColor = vec4(1.0, 0.0, 1.0, mask.r);
	return;
#endif

	vec3 voidColour = vec3(ABYSS_VOID_R, ABYSS_VOID_G, ABYSS_VOID_B);

	// Facing: turns -> radians, then rotate this fragment's view-space direction into world space (pitch then
	// yaw, the order the sky already verified in-game).
	float yaw = frameState.x * TAU;
	float pitch = (frameState.y - 0.5) * TAU;

	vec3 rd = normalize(-viewPosition);
	float sb = sin(pitch);
	float cb = cos(pitch);
	rd = normalize(vec3(rd.x, rd.y * cb - rd.z * sb, rd.y * sb + rd.z * cb));
	float sa = sin(-yaw);
	float ca = cos(-yaw);
	rd = normalize(vec3(rd.z * sa + rd.x * ca, rd.y, rd.z * ca - rd.x * sa));

	// Camera position (folded metres, Y-flipped for the verified vertical sign), measured in cells.
	vec3 cam = vec3(
		(camXZ.x - 0.5) * POSITION_PERIOD,
		-(frameState.z - 0.5) * POSITION_PERIOD,
		(camXZ.y - 0.5) * POSITION_PERIOD) / ABYSS_CELL_SIZE;

	float timePhase = GameTime * TAU * ABYSS_STIR_SPEED;

	vec3 col = voidColour;

	// Stack the shells nearest-first. Debug: additive over the void with NO foreground dimming, so every
	// layer contributes and nothing can be hidden. Near shells are low-frequency/large/strong-parallax, far
	// shells finer/denser/nearly static — that frequency+parallax gradient is the whole perspective trick.
	for (int L = 0; L < ABYSS_LAYERS; L++) {
		float depth01 = float(L) / float(ABYSS_LAYERS - 1);
		float freq = mix(ABYSS_LAYER_FREQ_NEAR, ABYSS_LAYER_FREQ_FAR, depth01);

		vec2 dir = parallax(rd.xz, cam, depth01);
		dir.y += rd.y * freq * 0.5; // spread on the vertical axis so looking up/down is not a flat sheet

		float haze = 0.0;
		col += shellField(dir, float(L) * 7.13, freq, timePhase, haze);
	}

	col = clamp(col, 0.0, 1.0);
	fragColor = vec4(col, mask.r);
}
