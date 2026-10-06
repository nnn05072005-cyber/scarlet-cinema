package vn.scarlet.cinema.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/** Tạo mã QR in trên vé bằng thư viện ZXing. */
@Service
public class QrService {

    /**
     * Trả về mã QR dạng ảnh SVG (chuỗi văn bản) chứa nội dung đã cho.
     * Mỗi dãy ô đen liền nhau trên một hàng được vẽ thành một hình chữ nhật.
     */
    public String svg(String content) {
        BitMatrix matrix;
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.MARGIN, 1,
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            // Kích thước 0 x 0: ZXing trả về ma trận nhỏ nhất, mỗi ô của mã QR là một điểm
            matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints);
        } catch (WriterException e) {
            throw new IllegalStateException("Không tạo được mã QR", e);
        }
        int size = matrix.getWidth();
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < matrix.getHeight(); y++) {
            int x = 0;
            while (x < size) {
                if (!matrix.get(x, y)) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < size && matrix.get(x, y)) {
                    x++;
                }
                int length = x - start;
                path.append('M').append(start).append(' ').append(y)
                        .append('h').append(length).append("v1h-").append(length).append('z');
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + size + " " + size + "\" "
                + "width=\"100%\" height=\"100%\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"Mã QR soát vé\">"
                + "<rect width=\"" + size + "\" height=\"" + size + "\" fill=\"#fff\"/>"
                + "<path fill=\"#000\" d=\"" + path + "\"/></svg>";
    }
}
