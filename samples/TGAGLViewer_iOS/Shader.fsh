//
//  Shader.fsh
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

varying lowp vec2 vTexcoord;

uniform sampler2D texture;

void main()
{
    gl_FragColor = texture2D(texture, vTexcoord);
}
