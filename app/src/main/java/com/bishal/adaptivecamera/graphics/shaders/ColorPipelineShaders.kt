package com.bishal.adaptivecamera.graphics.shaders

/**
 * Single authority containing OpenGL ES 3.0 GLSL shader programs for Xamera's
 * GPU color science pipeline, 3D LUT sampling, film grain, and halation.
 */
object ColorPipelineShaders {

    /**
     * Shared vertex shader for quad rendering with texture coordinate transformation matrix.
     */
    const val VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec4 aPosition;
layout(location = 1) in vec2 aTexCoord;

uniform mat4 uTexMatrix;
out vec2 vTexCoord;

void main() {
    gl_Position = aPosition;
    vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
}
"""

    /**
     * Common color science functions embedded in all fragment shaders.
     */
    private const val COMMON_COLOR_SCIENCE_FUNCTIONS = """
// --- Transfer Function Utilities ---
vec3 linearizeSRGB(vec3 c) {
    vec3 low = c / 12.92;
    vec3 high = pow((c + 0.055) / 1.055, vec3(2.4));
    return mix(low, high, step(vec3(0.04045), c));
}

vec3 encodeSRGB(vec3 c) {
    vec3 low = c * 12.92;
    vec3 high = 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055;
    return mix(low, high, step(vec3(0.0031308), c));
}

vec3 slog3ToLinear(vec3 c) {
    // Sony S-Log3 EOTF approximation
    vec3 high = pow(vec3(10.0), ((c * 1023.0 - 420.0) / 261.5)) * (0.18 + 0.01) - 0.01;
    vec3 low = (c * 1023.0 - 95.0) * 0.18 / 75.0;
    return mix(low, high, step(vec3(171.21029464 / 1023.0), c));
}

vec3 logCToLinear(vec3 c) {
    // ARRI Log-C EI 800 approximation
    float cut = 0.010591;
    float a = 5.555556;
    float b = 0.052272;
    float c_const = 0.247190;
    float d = 0.385537;
    vec3 high = (pow(vec3(10.0), (c - d) / c_const) - b) / a;
    vec3 low = (c - 0.092809) / 4.0;
    return mix(low, high, step(vec3(0.149658), c));
}

vec3 hlgToLinear(vec3 c) {
    // ITU-R BT.2100 HLG Inverse OETF
    float a = 0.17883277;
    float b = 0.28466892; // 1 - 4a
    float c_const = 0.55991073; // 0.5 - a * ln(4a)
    vec3 low = (c * c) / 3.0;
    vec3 high = (exp((c - c_const) / a) + b) / 12.0;
    return mix(low, high, step(vec3(0.5), c));
}

vec3 applyInputTransfer(vec3 color, int mode) {
    if (mode == 1) return linearizeSRGB(color);
    if (mode == 2) return color; // Linear
    if (mode == 3) return slog3ToLinear(color);
    if (mode == 4) return logCToLinear(color);
    if (mode == 5) return hlgToLinear(color);
    return linearizeSRGB(color); // Default Rec.709 / sRGB
}

// Pseudo-random white noise generator
float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y) - 0.5;
}
"""

    /**
     * Fragment shader for live camera preview (OES External Texture).
     */
    const val PREVIEW_FRAGMENT_SHADER = """#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
precision mediump sampler3D;

in vec2 vTexCoord;
out vec4 fragColor;

uniform samplerExternalOES uCameraTexture;
uniform sampler3D uLut3dTexture;

// Pipeline Controls
uniform int uLutEnabled;
uniform float uLutIntensity;
uniform float uLutScale;
uniform float uLutOffset;
uniform int uTransferMode;

// Film Grain
uniform int uGrainEnabled;
uniform float uGrainIntensity;
uniform float uGrainSize;
uniform float uTime;
uniform vec2 uResolution;

// Photochemical Halation
uniform int uHalationEnabled;
uniform float uHalationIntensity;
uniform float uHalationThreshold;
uniform float uHalationRadius;

$COMMON_COLOR_SCIENCE_FUNCTIONS

void main() {
    vec4 rawColor = texture(uCameraTexture, vTexCoord);
    vec3 color = rawColor.rgb;

    // 1. Input Transfer Function (Log / Gamma to Linear)
    color = applyInputTransfer(color, uTransferMode);

    // 2. 3D LUT Color Grading Stage
    if (uLutEnabled == 1) {
        // Half-texel offset compensation: coord = color * (N - 1) / N + 0.5 / N
        vec3 lutCoord = clamp(color, 0.0, 1.0) * uLutScale + uLutOffset;
        vec3 graded = texture(uLut3dTexture, lutCoord).rgb;
        color = mix(color, graded, uLutIntensity);
    }

    // 3. Photochemical Halation (Specular highlight edge bleed)
    if (uHalationEnabled == 1 && uHalationIntensity > 0.0) {
        float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        float excess = clamp((luma - uHalationThreshold) / max(1.0 - uHalationThreshold, 0.001), 0.0, 1.0);
        if (excess > 0.0) {
            vec3 halationTint = vec3(1.0, 0.35, 0.05); // Classic photographic warm red-amber
            color += halationTint * (excess * excess) * uHalationIntensity;
        }
    }

    // 4. Procedural Film Grain (Organic silver halide emulation)
    if (uGrainEnabled == 1 && uGrainIntensity > 0.0) {
        float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        // Luma weighting: strongest in midtones, soft in highlights/shadows
        float lumaWeight = clamp(1.0 - abs(2.0 * luma - 1.0), 0.25, 1.0);
        vec2 grainCoord = (vTexCoord * uResolution) / max(uGrainSize, 0.5);
        vec2 seed = grainCoord + vec2(uTime * 17.3, uTime * 43.7);
        float noise = hash(seed);
        color += vec3(noise * uGrainIntensity * lumaWeight);
    }

    // 5. Output Transfer (Encode to display sRGB)
    color = encodeSRGB(clamp(color, 0.0, 1.0));
    fragColor = vec4(color, rawColor.a);
}
"""

    /**
     * Fragment shader for still photos and standard 2D textures (FBO offscreen rendering).
     */
    const val PHOTO_FRAGMENT_SHADER = """#version 300 es
precision mediump float;
precision mediump sampler3D;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uImageTexture;
uniform sampler3D uLut3dTexture;

// Pipeline Controls
uniform int uLutEnabled;
uniform float uLutIntensity;
uniform float uLutScale;
uniform float uLutOffset;
uniform int uTransferMode;

// Film Grain
uniform int uGrainEnabled;
uniform float uGrainIntensity;
uniform float uGrainSize;
uniform float uTime;
uniform vec2 uResolution;

// Photochemical Halation
uniform int uHalationEnabled;
uniform float uHalationIntensity;
uniform float uHalationThreshold;
uniform float uHalationRadius;

$COMMON_COLOR_SCIENCE_FUNCTIONS

void main() {
    vec4 rawColor = texture(uImageTexture, vTexCoord);
    vec3 color = rawColor.rgb;

    // 1. Input Transfer Function
    color = applyInputTransfer(color, uTransferMode);

    // 2. 3D LUT Color Grading Stage
    if (uLutEnabled == 1) {
        vec3 lutCoord = clamp(color, 0.0, 1.0) * uLutScale + uLutOffset;
        vec3 graded = texture(uLut3dTexture, lutCoord).rgb;
        color = mix(color, graded, uLutIntensity);
    }

    // 3. Photochemical Halation
    if (uHalationEnabled == 1 && uHalationIntensity > 0.0) {
        float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        float excess = clamp((luma - uHalationThreshold) / max(1.0 - uHalationThreshold, 0.001), 0.0, 1.0);
        if (excess > 0.0) {
            vec3 halationTint = vec3(1.0, 0.35, 0.05);
            color += halationTint * (excess * excess) * uHalationIntensity;
        }
    }

    // 4. Procedural Film Grain (Deterministic seed for still photos)
    if (uGrainEnabled == 1 && uGrainIntensity > 0.0) {
        float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
        float lumaWeight = clamp(1.0 - abs(2.0 * luma - 1.0), 0.25, 1.0);
        vec2 grainCoord = (vTexCoord * uResolution) / max(uGrainSize, 0.5);
        float noise = hash(grainCoord + vec2(133.7, 89.1));
        color += vec3(noise * uGrainIntensity * lumaWeight);
    }

    // 5. Output Transfer
    color = encodeSRGB(clamp(color, 0.0, 1.0));
    fragColor = vec4(color, rawColor.a);
}
"""
}
