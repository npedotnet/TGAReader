//
//  ViewController.m
//  TGAGLViewer_iOS
//
// Copyright (c) 2014 Kenji Sasaki
// Released under the MIT license.
// https://github.com/npedotnet/TGAReader/blob/master/LICENSE
//
// English document
// https://github.com/npedotnet/TGAReader/blob/master/README.md
//
// Japanese document
// http://3dtech.jp/wiki/index.php?TGAReader
//

#import "ViewController.h"
#include "tga_reader.h"

#define BUFFER_OFFSET(i) ((char *)NULL + (i))

static float POSITIONS[] = {
    // front
    -0.5f, +0.5f, +0.5f,
    -0.5f, -0.5f, +0.5f,
    +0.5f, +0.5f, +0.5f,
    +0.5f, -0.5f, +0.5f,
    // back
    +0.5f, +0.5f, -0.5f,
    +0.5f, -0.5f, -0.5f,
    -0.5f, +0.5f, -0.5f,
    -0.5f, -0.5f, -0.5f,
};

static float TEXCOORDS[] = {
    // front
    0.f, 0.f,
    0.f, 1.f,
    1.f, 0.f,
    1.f, 1.f,
    // back
    0.f, 0.f,
    0.f, 1.f,
    1.f, 0.f,
    1.f, 1.f,
};

@interface ViewController () {
    GLuint _program;
    
    GLKMatrix4 _mvpMatrices[3];
    GLint _textures[3];
    
    GLuint _vertexBuffer;
}
@property (strong, nonatomic) EAGLContext *context;

- (void)setupGL;
- (void)tearDownGL;

- (GLuint)createTGATexture:(NSString *)path;
- (BOOL)loadShaders;
- (BOOL)compileShader:(GLuint *)shader type:(GLenum)type file:(NSString *)file;
- (BOOL)linkProgram:(GLuint)prog;
- (BOOL)validateProgram:(GLuint)prog;
@end

@implementation ViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    self.context = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];

    if (!self.context) {
        NSLog(@"Failed to create ES context");
    }
    
    GLKView *view = (GLKView *)self.view;
    view.context = self.context;
    view.drawableDepthFormat = GLKViewDrawableDepthFormat24;
    
    [self setupGL];
}

- (void)dealloc
{    
    [self tearDownGL];
    
    if ([EAGLContext currentContext] == self.context) {
        [EAGLContext setCurrentContext:nil];
    }
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];

    if ([self isViewLoaded] && ([[self view] window] == nil)) {
        self.view = nil;
        
        [self tearDownGL];
        
        if ([EAGLContext currentContext] == self.context) {
            [EAGLContext setCurrentContext:nil];
        }
        self.context = nil;
    }

    // Dispose of any resources that can be recreated.
}

- (void)setupGL
{
    [EAGLContext setCurrentContext:self.context];
    
    [self loadShaders];
    
    glEnable(GL_DEPTH_TEST);
    
    int positionSize = sizeof(POSITIONS);
    int texcoordSize = sizeof(TEXCOORDS);
    int vertexBufferSize = positionSize + texcoordSize;
    int positionOffset = 0;
    int texcoordOffset = positionOffset + positionSize;
    
    glGenBuffers(1, &_vertexBuffer);
    glBindBuffer(GL_ARRAY_BUFFER, _vertexBuffer);
    glBufferData(GL_ARRAY_BUFFER, vertexBufferSize, NULL, GL_STATIC_DRAW);
    glBufferSubData(GL_ARRAY_BUFFER, positionOffset, positionSize, POSITIONS);
    glBufferSubData(GL_ARRAY_BUFFER, texcoordOffset, texcoordSize, TEXCOORDS);
    
    _textures[0] = [self createTGATexture:[[NSBundle mainBundle] pathForResource:@"rgb_rle_LL" ofType:@"tga"]];
    _textures[1] = [self createTGATexture:[[NSBundle mainBundle] pathForResource:@"grayscale_rle_LL" ofType:@"tga"]];
    _textures[2] = [self createTGATexture:[[NSBundle mainBundle] pathForResource:@"indexed_rle_LL" ofType:@"tga"]];
    
}

- (void)tearDownGL
{
    [EAGLContext setCurrentContext:self.context];
    
    glDeleteBuffers(1, &_vertexBuffer);
    
    glDeleteTextures(3, (const GLuint *)_textures);
    
    if (_program) {
        glDeleteProgram(_program);
        _program = 0;
    }
}

- (GLuint)createTGATexture:(NSString *)path {
    
    GLuint texture = 0;
    
    FILE *file = fopen([path UTF8String], "rb");
    if(file) {
        fseek(file, 0, SEEK_END);
        int size = ftell(file);
        fseek(file, 0, SEEK_SET);
        
        unsigned char *buffer = (unsigned char *)tgaMalloc(size);
        fread(buffer, 1, size, file);
        fclose(file);
        
        int width = tgaGetWidth(buffer);
        int height = tgaGetHeight(buffer);
        int *pixels = tgaRead(buffer, TGA_READER_ABGR);
        
        tgaFree(buffer);
        
        glGenTextures(1, &texture);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, texture);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        
        tgaFree(pixels);
    }
    
    return texture;

}

#pragma mark - GLKView and GLKViewController delegate methods

- (void)update
{
    float aspect = fabsf(self.view.bounds.size.width / self.view.bounds.size.height);
    
    GLKMatrix4 projection = GLKMatrix4MakePerspective(GLKMathDegreesToRadians(45.0f), aspect, 1.f, 10.0f);
    GLKMatrix4 view = GLKMatrix4MakeLookAt(3, 3, 3, 0, 0, 0, 0, 1, 0);
    GLKMatrix4 vp = GLKMatrix4Multiply(projection, view);
    
    // front/back
    _mvpMatrices[0] = vp;
    
    // left/right
    GLKMatrix4 m = GLKMatrix4MakeRotation(M_PI*0.5f, 0, 1, 0);
    _mvpMatrices[1] = GLKMatrix4Multiply(vp, m);
    
    // top/bottom
    m = GLKMatrix4MakeRotation(M_PI*0.5f, -1, 0, 0);
    _mvpMatrices[2] = GLKMatrix4Multiply(vp, m);
    
}

- (void)glkView:(GLKView *)view drawInRect:(CGRect)rect
{
    glClearColor(0.65f, 0.65f, 0.65f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);
    
    // Render the object again with ES2
    glUseProgram(_program);
    
    glBindBuffer(GL_ARRAY_BUFFER, _vertexBuffer);
    
    // vertexattrib
    int posAttr = glGetAttribLocation(_program, "position");
    glEnableVertexAttribArray(posAttr);
    int positionOffset = 0;
    glVertexAttribPointer(posAttr, 3, GL_FLOAT, GL_FALSE, 0, BUFFER_OFFSET(positionOffset));
    
    int texAttr = glGetAttribLocation(_program, "texcoord");
    glEnableVertexAttribArray(texAttr);
    int texcoordOffset = sizeof(POSITIONS);
    glVertexAttribPointer(texAttr, 2, GL_FLOAT, GL_FALSE, 0, BUFFER_OFFSET(texcoordOffset));
    
    // uniform
    GLint mvpUniform = glGetUniformLocation(_program, "modelViewProjectionMatrix");
    
    GLint texUniform = glGetUniformLocation(_program, "texturea");
    glUniform1i(texUniform, 0);
    
    glEnable(GL_TEXTURE_2D);
    glActiveTexture(GL_TEXTURE0);
    
    // draw front/back
    glUniformMatrix4fv(mvpUniform, 1, 0, _mvpMatrices[0].m);
    glBindTexture(GL_TEXTURE_2D, _textures[0]);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDrawArrays(GL_TRIANGLE_STRIP, 4, 4);
    
    // draw left/right
    glUniformMatrix4fv(mvpUniform, 1, 0, _mvpMatrices[1].m);
    glBindTexture(GL_TEXTURE_2D, _textures[1]);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDrawArrays(GL_TRIANGLE_STRIP, 4, 4);
    
    // draw top/bottom
    glUniformMatrix4fv(mvpUniform, 1, 0, _mvpMatrices[2].m);
    glBindTexture(GL_TEXTURE_2D, _textures[2]);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDrawArrays(GL_TRIANGLE_STRIP, 4, 4);
    
}

#pragma mark -  OpenGL ES 2 shader compilation

- (BOOL)loadShaders
{
    GLuint vertShader, fragShader;
    NSString *vertShaderPathname, *fragShaderPathname;
    
    // Create shader program.
    _program = glCreateProgram();
    
    // Create and compile vertex shader.
    vertShaderPathname = [[NSBundle mainBundle] pathForResource:@"Shader" ofType:@"vsh"];
    if (![self compileShader:&vertShader type:GL_VERTEX_SHADER file:vertShaderPathname]) {
        NSLog(@"Failed to compile vertex shader");
        return NO;
    }
    
    // Create and compile fragment shader.
    fragShaderPathname = [[NSBundle mainBundle] pathForResource:@"Shader" ofType:@"fsh"];
    if (![self compileShader:&fragShader type:GL_FRAGMENT_SHADER file:fragShaderPathname]) {
        NSLog(@"Failed to compile fragment shader");
        return NO;
    }
    
    // Attach vertex shader to program.
    glAttachShader(_program, vertShader);
    
    // Attach fragment shader to program.
    glAttachShader(_program, fragShader);
    
    // Link program.
    if (![self linkProgram:_program]) {
        NSLog(@"Failed to link program: %d", _program);
        
        if (vertShader) {
            glDeleteShader(vertShader);
            vertShader = 0;
        }
        if (fragShader) {
            glDeleteShader(fragShader);
            fragShader = 0;
        }
        if (_program) {
            glDeleteProgram(_program);
            _program = 0;
        }
        
        return NO;
    }
    
    // Release vertex and fragment shaders.
    if (vertShader) {
        glDetachShader(_program, vertShader);
        glDeleteShader(vertShader);
    }
    if (fragShader) {
        glDetachShader(_program, fragShader);
        glDeleteShader(fragShader);
    }
    
    return YES;
}

- (BOOL)compileShader:(GLuint *)shader type:(GLenum)type file:(NSString *)file
{
    GLint status;
    const GLchar *source;
    
    source = (GLchar *)[[NSString stringWithContentsOfFile:file encoding:NSUTF8StringEncoding error:nil] UTF8String];
    if (!source) {
        NSLog(@"Failed to load vertex shader");
        return NO;
    }
    
    *shader = glCreateShader(type);
    glShaderSource(*shader, 1, &source, NULL);
    glCompileShader(*shader);
    
#if defined(DEBUG)
    GLint logLength;
    glGetShaderiv(*shader, GL_INFO_LOG_LENGTH, &logLength);
    if (logLength > 0) {
        GLchar *log = (GLchar *)malloc(logLength);
        glGetShaderInfoLog(*shader, logLength, &logLength, log);
        NSLog(@"Shader compile log:\n%s", log);
        free(log);
    }
#endif
    
    glGetShaderiv(*shader, GL_COMPILE_STATUS, &status);
    if (status == 0) {
        glDeleteShader(*shader);
        return NO;
    }
    
    return YES;
}

- (BOOL)linkProgram:(GLuint)prog
{
    GLint status;
    glLinkProgram(prog);
    
#if defined(DEBUG)
    GLint logLength;
    glGetProgramiv(prog, GL_INFO_LOG_LENGTH, &logLength);
    if (logLength > 0) {
        GLchar *log = (GLchar *)malloc(logLength);
        glGetProgramInfoLog(prog, logLength, &logLength, log);
        NSLog(@"Program link log:\n%s", log);
        free(log);
    }
#endif
    
    glGetProgramiv(prog, GL_LINK_STATUS, &status);
    if (status == 0) {
        return NO;
    }
    
    return YES;
}

- (BOOL)validateProgram:(GLuint)prog
{
    GLint logLength, status;
    
    glValidateProgram(prog);
    glGetProgramiv(prog, GL_INFO_LOG_LENGTH, &logLength);
    if (logLength > 0) {
        GLchar *log = (GLchar *)malloc(logLength);
        glGetProgramInfoLog(prog, logLength, &logLength, log);
        NSLog(@"Program validate log:\n%s", log);
        free(log);
    }
    
    glGetProgramiv(prog, GL_VALIDATE_STATUS, &status);
    if (status == 0) {
        return NO;
    }
    
    return YES;
}

@end
