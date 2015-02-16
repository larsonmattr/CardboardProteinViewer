// Pass it a vec3 or vec4 FloatBuffer of atom positions.
attribute vec4 pos;
// Pass it a vec2 of FloatBuffer of impostor corner positions ([
attribute vec4 inputImpostorCoord;

// Pass these on to the fragment shader.
// impostor.xy used to create billboard coordinate.
varying vec2 impostorCoord;
// normalized
varying vec3 normalizedViewCoord;

// Provide a single matrix for rotations of model/camera/projection.
uniform mat4 modelViewProjMatrix;
uniform mat4 orthographicMatrix;
uniform mediump float sphereRadius;

void main() {
	vec4 tp;
//	tp = modelViewProjMatrix * pos;
//	impostorCoord = inputImpostorCoord.xy;
//	tp.xy = tp.xy + inputImpostorCoord.xy * vec2(sphereRadius);
//	tp = tp * orthographicMatrix;
//	normalizedViewCoord = (tp.xyz + 1.0) / 2.0;
	gl_Position = pos;
}