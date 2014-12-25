//
//  Shader.vsh
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

attribute vec4 position;
attribute vec2 texcoord;

varying lowp vec2 vTexcoord;

uniform mat4 modelViewProjectionMatrix;

void main()
{
    vTexcoord = texcoord;
    gl_Position = modelViewProjectionMatrix * position;
}
