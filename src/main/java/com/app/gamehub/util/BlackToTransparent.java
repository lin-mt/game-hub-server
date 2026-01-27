package com.app.gamehub.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class BlackToTransparent {

    public static void main(String[] args) throws Exception {
    File input = new File("C:\\Users\\linmt\\Downloads\\下载 (2).png");
        BufferedImage image = ImageIO.read(input);

        BufferedImage result = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        int threshold = 175; // 阈值 (0=纯黑, 值越大, 越多暗色会被透明化)

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);

                int alpha = (pixel >> 24) & 0xff;
                int red   = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue  = pixel & 0xff;

                // 判断是否为偏黑色：RGB 都比较小
                if (red < threshold && green < threshold && blue < threshold) {
                    // 改为透明
                    pixel = 0x00000000;
                } else {
                    // 保持原色
                    pixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                }

                result.setRGB(x, y, pixel);
            }
        }

        File output = new File("output.png");
        ImageIO.write(result, "PNG", output);

        System.out.println("处理完成，生成 output.png");
    }
}

