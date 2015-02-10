// Source: Brad Larson: http://stackoverflow.com/questions/10488086/drawing-a-sphere-in-opengl-es/10506172#10506172
attribute vec4 position;
attribute vec4 inputImpostorSpaceCoordinate;

varying mediump vec2 impostorSpaceCoordinate;
varying mediump vec3 normalizedViewCoordinate;

uniform mat4 modelViewProjMatrix;
uniform mediump mat4 orthographicMatrix;
uniform mediump float sphereRadius;

void main()
{
    vec4 transformedPosition;
    transformedPosition = modelViewProjMatrix * position;
    impostorSpaceCoordinate = inputImpostorSpaceCoordinate.xy;

    transformedPosition.xy = transformedPosition.xy + inputImpostorSpaceCoordinate.xy * vec2(sphereRadius);
    transformedPosition = transformedPosition * orthographicMatrix;

    normalizedViewCoordinate = (transformedPosition.xyz + 1.0) / 2.0;
    gl_Position = transformedPosition;
}