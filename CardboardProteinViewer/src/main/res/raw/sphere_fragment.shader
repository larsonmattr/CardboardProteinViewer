precision mediump float;

// Determines the final color of the spheres.
uniform vec3 lightPosition;
uniform vec3 sphereColor;
uniform mediump float sphereRadius;

// Provides a depth buffer. Not implemented yet in the shader.
// uniform sampler2D depthTexture;

// Created by sphere_vertex.shader
varying vec2 impostorCoord;
varying vec3 normalizedViewCoord;


void main()
{
    float distanceFromCenter = length(impostorCoord);

    // Establish the visual bounds of the sphere
    if (distanceFromCenter > 1.0)
    {
        // Discard ends shading operation if it is a pixel outside the sphere radius.
        discard;
        // Discard isn't working correctly, ie to throw away pixels outside a sphere.
    }

    float normalizedDepth = sqrt(1.0 - distanceFromCenter * distanceFromCenter);


    // Current depth
    float depthOfFragment = sphereRadius * 0.5 * normalizedDepth;
    float currentDepthValue = (normalizedViewCoord.z - depthOfFragment - 0.0025);

    // Calculate the lighting normal for the sphere
    vec3 normal = vec3(impostorCoord, normalizedDepth);
    vec3 finalSphereColor = sphereColor;

    // ambient
    float lightingIntensity = 0.3 + 0.7 * clamp(dot(lightPosition, normal), 0.0, 1.0);
    finalSphereColor *= lightingIntensity;

    // Per fragment specular lighting
    lightingIntensity  = clamp(dot(lightPosition, normal), 0.0, 1.0);
    lightingIntensity  = pow(lightingIntensity, 60.0);
    finalSphereColor += vec3(0.4, 0.4, 0.4) * lightingIntensity;
    gl_FragColor = vec4(finalSphereColor, 1.0);
    //gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);  // DEBUG
}