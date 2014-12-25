//
//  TGACollectionViewController.m
//  TGAImageViewer_iOS
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

#import "TGACollectionViewController.h"
#include "tga_reader.h"

@interface TGACollectionViewController () {
    NSArray *images;
}
@end

@implementation TGACollectionViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    images = [NSArray arrayWithObjects:
              @"grayscale_LL",
              @"grayscale_rle_LL",
              @"grayscale_a_LL",
              @"grayscale_a_rle_LL",
              @"indexed_LL",
              @"indexed_rle_LL",
              @"indexed_a_LL",
              @"indexed_a_rle_LL",
              @"rgb_LL",
              @"rgb_rle_LL",
              @"rgb_a_LL",
              @"rgb_a_rle_LL",
              nil];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (NSInteger)collectionView:(UICollectionView *)collectionView numberOfItemsInSection:(NSInteger)section {
    return images.count;
}

- (UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath {
    
    UICollectionViewCell *cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"Cell" forIndexPath:indexPath];
    
    UIImageView *imageView = (UIImageView *)[cell viewWithTag:100];
    NSString *path = [[NSBundle mainBundle] pathForResource:[images objectAtIndex:indexPath.row] ofType:@"tga"];
    imageView.image = [self createTGAImage:path];
    
    return cell;
    
}

- (UIImage *)createTGAImage:(NSString *)path {
    
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
        
        CGColorSpaceRef colorSpaceRef = CGColorSpaceCreateDeviceRGB();
        CGBitmapInfo bitmapInfo = (CGBitmapInfo)kCGImageAlphaLast;
        CGDataProviderRef providerRef = CGDataProviderCreateWithData(NULL, pixels, 4*width*height, releaseDataCallback);
        
        CGImageRef imageRef = CGImageCreate(width, height, 8, 32, 4*width, colorSpaceRef, bitmapInfo, providerRef, NULL, 0, kCGRenderingIntentDefault);
        
        UIImage *image = [[UIImage alloc] initWithCGImage:imageRef];
        
        CGColorSpaceRelease(colorSpaceRef);
        
        return image;
    }
    
    return nil;
    
}

static void releaseDataCallback(void *info, const void *data, size_t size) {
    tgaFree((void *)data);
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
