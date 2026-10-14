#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

// width of steps, number of steps and bloom brightness (bloom2.fsh)
const float blurWidth = 0.002;
const float blurSteps = 0.1;
const float amount = 5.0;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float brightness = (color.r + color.g + color.b) / 3.0;
    vec4 sum = vec4(0.0);
    const float foo = 10.0 / (((blurSteps * 2.0) + 1.0) * 4.0);
    for (float i = -blurSteps; i < blurSteps; i++) {
        sum += texture(DiffuseSampler, texCoord + vec2(i, i) * blurWidth * 0.7);
        sum += texture(DiffuseSampler, texCoord + vec2(i, -i) * blurWidth * 0.7);
        sum += texture(DiffuseSampler, texCoord + vec2(0.0, i) * blurWidth);
        sum += texture(DiffuseSampler, texCoord + vec2(i, 0.0) * blurWidth);
    }
    sum *= foo;
    vec4 modifier = sum * sum * (0.015 - (brightness * 0.01));
    vec4 outputColor = modifier * amount + color;
    fragColor = vec4((outputColor * 0.9).rgb, 1.0);
}
