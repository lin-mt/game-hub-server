package com.app.gamehub.util;

import static com.app.gamehub.util.ImageUtils.matToBufferedImage;

import com.app.gamehub.exception.BusinessException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class GameStatOcrUtil {

  private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d{1,4})(?:\\.\\d+)?%");

  static {
    nu.pattern.OpenCV.loadLocally();
  }

  public static void main(String[] args) {
    try {
      File imageFile = new File("img.png");
//      Map<String, Integer> stats = parseStats(imageFile);
//      System.out.println("Extracted Stats: " + stats);
      cutLeft(imageFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void cutLeft(File  file) throws IOException {
    BufferedImage original = ImageIO.read(file);

    int width = original.getWidth();
    int height = original.getHeight();

    // 裁剪左半部分
    BufferedImage leftHalf = original.getSubimage(
        0,              // x
        0,              // y
        width / 2,      // 裁剪宽度
        height          // 裁剪高度
    );

    ImageIO.write(leftHalf, "jpg", new File("output_left.jpg"));
  }

  public static Map<String, Integer> parseStats(File imageFile) throws Exception {

    // 1. 读取原图（OpenCV）
    Mat src = Imgcodecs.imread(imageFile.getAbsolutePath());
    int width = src.width();
    int height = src.height();

    // 2. 裁剪左半部分（ROI）
    Rect leftRoi = new Rect(0, 0, width / 2, height);
    Mat leftMat = new Mat(src, leftRoi);

    // 3. 灰度
    Mat gray = new Mat();
    Imgproc.cvtColor(leftMat, gray, Imgproc.COLOR_BGR2GRAY);

    // 4. 自适应二值化
    Mat binary = new Mat();
    Imgproc.adaptiveThreshold(
        gray, binary, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 5);

    // 5. Mat → BufferedImage（给 Tesseract）
    BufferedImage processedImage = matToBufferedImage(binary);

    // 6. OCR
    ITesseract tesseract = new Tesseract();
    tesseract.setLanguage("chi_sim");
    tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata");
    tesseract.setVariable("user_defined_dpi", "300");
    tesseract.setVariable("tessedit_char_whitelist", "0123456789.%");

    String resultText = tesseract.doOCR(processedImage).replace(" ", "").replace("_", "");

    return extractStats(resultText);
  }

  private static Map<String, Integer> extractStats(String text) {

    // 顺序输出
    Map<String, Integer> result = new LinkedHashMap<>();

    // 1. 提取所有百分比整数
    List<Integer> values = new ArrayList<>();
    Matcher matcher = PERCENT_PATTERN.matcher(text);

    while (matcher.find()) {
      values.add(Integer.parseInt(matcher.group(1)));
    }

    int count = values.size();
    if (count < 11) {
      throw new BusinessException("属性数据错误，应该至少包含11个属性，建议手动填写数据");
    }

    // 2. 只取最后最多 12 个
    List<Integer> tail = count > 12 ? values.subList(count - 12, count) : values;

    // 3. 属性定义（从前到后）
    String[] keys = {
      "步兵攻击力", "步兵防御力", "步兵破坏力", "步兵生命值", "骑兵攻击力", "骑兵防御力", "骑兵破坏力", "骑兵生命值", "弓兵攻击力", "弓兵防御力",
      "弓兵破坏力", "弓兵生命值"
    };

    // 4. 如果少一个，默认缺的是“步兵攻击力”
    int keyOffset = tail.size() == 11 ? 1 : 0;

    for (int i = 0; i < tail.size(); i++) {
      result.put(keys[i + keyOffset], tail.get(i));
    }

    return result;
  }
}
