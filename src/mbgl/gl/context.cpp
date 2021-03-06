#include <mbgl/gl/context.hpp>

namespace mbgl {
namespace gl {

Context::~Context() {
    reset();
}

void Context::reset() {
    std::copy(pooledTextures.begin(), pooledTextures.end(), std::back_inserter(abandonedTextures));
    pooledTextures.resize(0);
    performCleanup();
}

namespace {

template <typename Fn>
void applyStateFunction(Context& context, Fn&& fn) {
    fn(context.stencilFunc);
    fn(context.stencilMask);
    fn(context.stencilTest);
    fn(context.stencilOp);
    fn(context.depthRange);
    fn(context.depthMask);
    fn(context.depthTest);
    fn(context.depthFunc);
    fn(context.blend);
    fn(context.blendFunc);
    fn(context.blendColor);
    fn(context.colorMask);
    fn(context.clearDepth);
    fn(context.clearColor);
    fn(context.clearStencil);
    fn(context.program);
    fn(context.lineWidth);
    fn(context.activeTexture);
    fn(context.bindFramebuffer);
    fn(context.viewport);
#ifndef GL_ES_VERSION_2_0
    fn(context.pixelZoom);
    fn(context.rasterPos);
#endif // GL_ES_VERSION_2_0
    for (auto& tex : context.texture) {
        fn(tex);
    }
    fn(context.vertexBuffer);
    fn(context.elementBuffer);
    fn(context.vertexArrayObject);
}

} // namespace

void Context::resetState() {
    applyStateFunction(*this, [](auto& state) { state.reset(); });
}

void Context::setDirtyState() {
    applyStateFunction(*this, [](auto& state) { state.setDirty(); });
}

void Context::performCleanup() {
    for (GLuint id : abandonedPrograms) {
        if (program == id) {
            program.setDirty();
        }
        MBGL_CHECK_ERROR(glDeleteProgram(id));
    }
    abandonedPrograms.clear();

    for (GLuint id : abandonedShaders) {
        MBGL_CHECK_ERROR(glDeleteShader(id));
    }
    abandonedShaders.clear();

    if (!abandonedBuffers.empty()) {
        for (const auto id : abandonedBuffers) {
            if (vertexBuffer == id) {
                vertexBuffer.setDirty();
            } else if (elementBuffer == id) {
                elementBuffer.setDirty();
            }
        }
        MBGL_CHECK_ERROR(glDeleteBuffers(int(abandonedBuffers.size()), abandonedBuffers.data()));
        abandonedBuffers.clear();
    }

    if (!abandonedTextures.empty()) {
        for (const auto id : abandonedTextures) {
            if (activeTexture == id) {
                activeTexture.setDirty();
            }
        }
        MBGL_CHECK_ERROR(glDeleteTextures(int(abandonedTextures.size()), abandonedTextures.data()));
        abandonedTextures.clear();
    }

    if (!abandonedVAOs.empty()) {
        for (const auto id : abandonedVAOs) {
            if (vertexArrayObject == id) {
                vertexArrayObject.setDirty();
            }
        }
        MBGL_CHECK_ERROR(gl::DeleteVertexArrays(int(abandonedVAOs.size()), abandonedVAOs.data()));
        abandonedVAOs.clear();
    }

    if (!abandonedFBOs.empty()) {
        for (const auto id : abandonedFBOs) {
            if (bindFramebuffer == id) {
                bindFramebuffer.setDirty();
            }
        }
        MBGL_CHECK_ERROR(glDeleteFramebuffers(int(abandonedFBOs.size()), abandonedFBOs.data()));
        abandonedFBOs.clear();
    }
}

} // namespace gl
} // namespace mbgl
