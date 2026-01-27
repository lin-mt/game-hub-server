package com.app.gamehub.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.imageio.ImageIO;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class ImageUtils {

  static {
    nu.pattern.OpenCV.loadLocally();
  }

  /** 灰度 + 自适应二值化 */
  public static BufferedImage grayAndBinary(File imageFile) throws Exception {

    // 读取原图
    Mat src = Imgcodecs.imread(imageFile.getAbsolutePath());

    // 灰度
    Mat gray = new Mat();
    Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

    // 自适应二值化（非常适合截图）
    Mat binary = new Mat();
    Imgproc.adaptiveThreshold(
        gray, binary, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 31, 5);

    return matToBufferedImage(binary);
  }

  public static BufferedImage matToBufferedImage(Mat mat) throws Exception {
    MatOfByte mob = new MatOfByte();
    Imgcodecs.imencode(".png", mat, mob);
    return ImageIO.read(new ByteArrayInputStream(mob.toArray()));
  }

  /** 预处理流程：缩放 -> 灰度化 -> 二值化 */
  public static BufferedImage preprocess(BufferedImage srcImage) {
    // 1. 放大图片 (建议放大 2 倍，让文字像素更清晰)
    int width = srcImage.getWidth() * 2;
    int height = srcImage.getHeight() * 2;
    Image scaledImage = srcImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);

    BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
    Graphics2D g2d = bufferedImage.createGraphics();
    g2d.drawImage(scaledImage, 0, 0, null);
    g2d.dispose();

    // 2. 二值化处理（将灰色像素转为纯黑或纯白）
    // 阈值通常设为 120-150，可根据游戏背景深浅调整
    int threshold = 230;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int gray = (bufferedImage.getRGB(x, y) >> 8) & 0xFF;
        if (gray > threshold) {
          bufferedImage.setRGB(x, y, Color.WHITE.getRGB());
        } else {
          bufferedImage.setRGB(x, y, Color.BLACK.getRGB());
        }
      }
    }
    return bufferedImage;
  }

  /** 裁剪左侧属性列，排除右侧干扰 假设图片宽度为 W，高度为 H */
  public static BufferedImage cropAttributeArea(BufferedImage src) {
    int w = src.getWidth();
    int h = src.getHeight();

    // 根据你提供的图片比例：
    // 左侧属性名+数值大约占宽度的 60%
    // 顶部“信函”标题以下到按钮以上大约占高度的 15% - 75%
    int x = (int) (w * 0.05); // 稍微留点左边距
    int y = (int) (h * 0.12); // 避开顶部的“信函”字样
    int width = (int) (w * 0.5); // 只取到中间偏右一点，刚好切断右侧1.5%那列
    int height = (int) (h * 0.65); // 取到弓兵生命值下方

    return src.getSubimage(x, y, width, height);
  }

  /**
   * 裁剪图片的左半部分 用于百度OCR识别前的预处理
   *
   * @param imageFile 原始图片文件
   * @return 裁剪后的图片文件
   * @throws Exception 处理异常
   */
  public static File cropLeftHalf(File imageFile) throws Exception {
    // 读取原始图片
    BufferedImage originalImage = ImageIO.read(imageFile);

    int width = originalImage.getWidth();
    int height = originalImage.getHeight();

    // 裁剪左半部分
    int cropWidth = width / 2;
    BufferedImage croppedImage = originalImage.getSubimage(0, 0, cropWidth, height);

    // 获取原始文件扩展名
    String originalName = imageFile.getName();
    String extension = "png";
    if (originalName.contains(".")) {
      extension = originalName.substring(originalName.lastIndexOf(".") + 1);
    }

    // 创建临时文件保存裁剪后的图片
    File croppedFile = File.createTempFile("cropped_left_", "." + extension);
    ImageIO.write(croppedImage, extension, croppedFile);

    return croppedFile;
  }
}
